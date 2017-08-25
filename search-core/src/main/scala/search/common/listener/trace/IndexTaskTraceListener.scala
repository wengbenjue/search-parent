package search.common.listener.trace

import search.common.config.SolrConfiguration
import search.common.redis.Redis
import search.common.view.control.ControlWebPage
import search.solr.client.impl.SolJSolrCloudClient
import search.solr.client.{SolrClient, SolrClientConf}

/**
  * Created by soledede on 2016/4/8.
  */
class IndexTaskTraceListener() extends TraceListener with SolrConfiguration {
  var redis: Redis = _
  var solrClient: SolrClient = _


  if (useSolr) init()

  def init() = {
    redis = Redis()
    solrClient = SolrClient(new SolrClientConf())
  }

  override def onAddIndex(content: AddIndex): Unit = {
    //add current indexing data to local queue
    ControlWebPage.currentIndexDatas.offer(content.content)
    redis.putToSet(IndexTaskTraceListener.SET_KEY, content.content)
  }

  override def onDelLastIndex(): Unit = {
    redis.delKey(Seq(IndexTaskTraceListener.SET_KEY))
  }

  override def onSolrCollectionTimeout(): Unit = {
    Thread.sleep(1000 * 60 * 2)
    solrClient.setSolrServer(SolJSolrCloudClient.singleCloudInstance(new SolrClientConf()))
  }

  override def onNodeNoHealth(): Unit = {
    solrClient.setSolrServer(SolJSolrCloudClient.singleCloudBackupInstance(new SolrClientConf()))
    Thread.sleep(1000 * 60 * 5)
    solrClient.setSolrServer(SolJSolrCloudClient.singleCloudInstance(new SolrClientConf()))
  }

  override def onSwitchSolrServer(server: String): Unit = {
    if (server != null && !server.trim.equalsIgnoreCase("")) {
      SolJSolrCloudClient.switch.compareAndSet(false, true)
      if (server.equalsIgnoreCase(SolJSolrCloudClient.BACUUP_SOLR_SERVER)) solrClient.setSolrServer(SolJSolrCloudClient.singleCloudBackupInstance(new SolrClientConf()))
      else solrClient.setSolrServer(SolJSolrCloudClient.singleCloudInstance(new SolrClientConf()))
    }
  }
}

object IndexTaskTraceListener {

  val SET_KEY: String = "last_index"

  def main(args: Array[String]) {


  }

}
