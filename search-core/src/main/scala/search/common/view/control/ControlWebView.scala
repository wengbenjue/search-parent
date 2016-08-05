package search.common.view.control

import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.http.HttpServletRequest

import org.json4s.JValue
import search.common.listener.trace.{SwitchSolrServer, DelLastIndex, IndexTaskTraceListener}
import search.solr.client.SolrClientConf
import search.solr.client.index.manager.impl.DefaultIndexManager
import search.common.queue.BlockQueue
import search.common.redis.Redis
import search.solr.client.searchInterface.SearchInterface
import search.common.util.{Util, Json4sHelp, Logging}
import search.common.view.{PageUtil, WebView, WebViewPage}

import scala.collection.mutable
import scala.xml.Node
import search.common.view.JettyUtil._


/**
  * Created by soledede on 2016/4/8.
  */
private[search] class ControlWebView(requestedPort: Int, conf: SolrClientConf) extends WebView(requestedPort, conf, name = "ControlWebView") with Logging {


  /** Initialize all components of the server. */
  override def initialize(): Unit = {
    attachHandler(createStaticHandler(WEB_STATIC_RESOURCE_DIR, "static"))
    val controlPage = new ControlWebPage()
    attachPage(controlPage)
    val listPage = new ListWebPage()
    attachPage(listPage)
  }

  initialize()
}


private[search] class ControlWebPage extends WebViewPage("solr") with PageUtil with Logging {


  override def render(request: HttpServletRequest): Seq[Node] = {

    val currentActiveCount = DefaultIndexManager.indexProcessThreadPool.getActiveCount
    val completeTaskCount = DefaultIndexManager.indexProcessThreadPool.getCompletedTaskCount
    val runTaskCount = DefaultIndexManager.indexProcessThreadPool.getQueue.size()

    val currentConsumerActiveCount = DefaultIndexManager.consumerManageThreadPool.getActiveCount
    val completeConsumerTaskCount = DefaultIndexManager.consumerManageThreadPool.getCompletedTaskCount
    val runConsumerTaskCount = DefaultIndexManager.consumerManageThreadPool.getQueue.size()

    //println("completeTaskCount:"+completeTaskCount+"\nrunTaskCount:"+runTaskCount+"\ncurrentActiveCount:"+currentActiveCount)

    if (currentConsumerActiveCount > 0 && completeConsumerTaskCount <= 5 && runConsumerTaskCount > 0) DefaultIndexManager.bus.post(DelLastIndex())
    else if (currentActiveCount > 0 && completeTaskCount <= 5 && runTaskCount > 0) DefaultIndexManager.bus.post(DelLastIndex())


    val queryString = request.getQueryString


    val cacheIndexDataQueue = ControlWebPage.currentIndexDatas
    var currentserver = "well"

    try {
      if (queryString != null) {
        if (queryString.contains("switchCollection=")) {
          val isChoosenCollection = Util.regexExtract(queryString, "switchCollection=([a-zA-Z0-9]+)&*", 1)
          if (isChoosenCollection != null && !isChoosenCollection.toString.trim.equalsIgnoreCase(""))
            SearchInterface.switchCollection = isChoosenCollection.toString.toBoolean
        }
        if (SearchInterface.switchCollection) {
          if (queryString.contains("switchMg=")) {
            val mergeClouds = Util.regexExtract(queryString, "switchMg=([a-zA-Z0-9]+)&*", 1)
            if (mergeClouds != null && !mergeClouds.toString.trim.equalsIgnoreCase(""))
              SearchInterface.switchMg = mergeClouds.toString.trim
          }
          if (queryString.contains("switchSc=")) {
            val screenClouds = Util.regexExtract(queryString, "switchSc=([a-zA-Z0-9]+)&*", 1)
            if (screenClouds != null && !screenClouds.toString.trim.equalsIgnoreCase(""))
              SearchInterface.switchSc = screenClouds.toString.trim
          }
        }
        if (queryString.contains("switchSolrServer=")) {
          val switchSolrServer = Util.regexExtract(queryString, "switchSolrServer=([a-zA-Z0-9]+)&*", 1)
          if (switchSolrServer != null && !switchSolrServer.toString.trim.equalsIgnoreCase(""))
            currentserver = switchSolrServer.toString.trim
          DefaultIndexManager.bus.post(SwitchSolrServer(currentserver))
        }
      }
    } catch {
      case e: Exception => log.error("switch faield", e)
    }


    val showPage = {
      <div>
        <img src="http://www.ehsy.com/images/logo.png"/>{if (currentActiveCount > 0) {
        <h4 style="color:red">Index is Running...请刷新浏览器！</h4>
      } else {
        <h4 style="color:green">Index Finished</h4>
      }}{if (SearchInterface.switchMg != null && !"null".equalsIgnoreCase(SearchInterface.switchMg)) {
        <h4 style="color:yellow">current product collection:
          {SearchInterface.switchMg}
        </h4>
      }}<br/>{if (SearchInterface.switchSc != null && !"null".equalsIgnoreCase(SearchInterface.switchSc)) {
        <h4 style="color:yellow">current attribute collection:
          {SearchInterface.switchSc}
        </h4>
      }}<br/>{if (currentActiveCount > 0 && cacheIndexDataQueue.size() > 0) {
        <h3>正在索引或删除的sku/id.</h3>
          <h4>
            {cacheIndexDataQueue.poll()}
          </h4>
          <a href="list" target="_blank">查看本次索引历史列表.</a>
      }}<br/>{if (currentActiveCount <= 0) {
        <a href="list" target="_blank">点击查看上次索引更新列表.</a>
      }}<br/>
        <h3 style="color:red">current server status:
          {currentserver}
        </h3>
      </div>
    }
    assemblePage(showPage, "solr task trace")
  }

  override def renderJson(request: HttpServletRequest): JValue = Json4sHelp.writeTest
}

object ControlWebPage {
  val currentIndexDatas = BlockQueue[String](new SolrClientConf(), "indexing")
  val isAlive = new AtomicBoolean(false)



  private val listenerThread = new Thread("check activity for thread") {
    setDaemon(true)

    override def run(): Unit = {
      while (true) {
        val currentActiveCount = DefaultIndexManager.consumerManageThreadPool.getActiveCount
        val completeTaskCount = DefaultIndexManager.consumerManageThreadPool.getCompletedTaskCount
        val runTaskCount = DefaultIndexManager.consumerManageThreadPool.getQueue.size()

        //println("completeTaskCount:"+completeTaskCount+"\nrunTaskCount:"+runTaskCount+"\ncurrentActiveCount:"+currentActiveCount)

        if (currentActiveCount > 0 && completeTaskCount <= 1 && runTaskCount > 0) DefaultIndexManager.bus.post(DelLastIndex())
        /*if (currentActiveCount > 0)
          ControlWebPage.isAlive.compareAndSet(false, true)
        else ControlWebPage.isAlive.compareAndSet(true, false)*/
        Thread.sleep(30 * 1000)
      }
    }
  }
  // listenerThread.start()
}


private[search] class ListWebPage extends WebViewPage("solr/list") with PageUtil with Logging {
  val redis = Redis()

  override def render(request: HttpServletRequest): Seq[Node] = {

    val currentActiveCount = DefaultIndexManager.indexProcessThreadPool.getActiveCount
    val completeTaskCount = DefaultIndexManager.indexProcessThreadPool.getCompletedTaskCount
    val runTaskCount = DefaultIndexManager.indexProcessThreadPool.getQueue.size()

    val currentConsumerActiveCount = DefaultIndexManager.consumerManageThreadPool.getActiveCount
    val completeConsumerTaskCount = DefaultIndexManager.consumerManageThreadPool.getCompletedTaskCount
    val runConsumerTaskCount = DefaultIndexManager.consumerManageThreadPool.getQueue.size()

    //println("completeTaskCount:"+completeTaskCount+"\nrunTaskCount:"+runTaskCount+"\ncurrentActiveCount:"+currentActiveCount)

    if (currentConsumerActiveCount > 0 && completeConsumerTaskCount <= 5 && runConsumerTaskCount > 0) DefaultIndexManager.bus.post(DelLastIndex())
    else if (currentActiveCount > 0 && completeTaskCount <= 5 && runTaskCount > 0) DefaultIndexManager.bus.post(DelLastIndex())

    val listSkus = redis.getAllFromSetByKey[String](IndexTaskTraceListener.SET_KEY)
    val count = if (listSkus == null) 0 else listSkus.size

    val showPage = {
      <div>
        <img src="http://www.ehsy.com/images/logo.png"/>
        <h1 style="color:red">索引列表,总数(大约)：
          {count}
          条记录!请刷新浏览器！</h1>{if (count > 0) {
        listSkus.map { s =>
          <h6>
            {s}
          </h6>
        }
      }}
      </div>
    }
    assemblePage(showPage, "solr list show")
  }

  override def renderJson(request: HttpServletRequest): JValue = Json4sHelp.writeTest
}

