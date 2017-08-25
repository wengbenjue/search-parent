package search.solr.client.impl

import java.util
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException
import org.apache.solr.client.solrj.impl._
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrInputDocument
import org.apache.solr.common.params.ModifiableSolrParams
import search.common.config.{SolrConfiguration, Configuration}
import search.common.listener.trace.{SolrNoHelthNode, SolrCollectionTimeout, ManagerListenerWaiter}
import search.common.util.Logging
import search.solr.client.{SolrClient, SolrClientConf}

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/11/16.
  */
private[search] class SolJSolrCloudClient private(conf: SolrClientConf) extends SolrClient with Logging with SolrConfiguration {
  var managerListenerWaiter: ManagerListenerWaiter = _

  private var server: CloudSolrClient = _

  if (useSolr) {
    server = SolJSolrCloudClient.singleCloudInstance(conf)
    managerListenerWaiter = ManagerListenerWaiter()
  }


  override def searchByQuery[T: ClassTag](query: T, collection: String = "searchcloud"): AnyRef = {
    var response: QueryResponse = null
    try {
      if (server == null) server = SolJSolrCloudClient.singleCloudInstance(conf)
      server = SolrClient.switchClient(server, null).asInstanceOf[CloudSolrClient]
      response = server.query(collection, query.asInstanceOf[SolrQuery])
    } catch {
      case et: java.net.ConnectException =>
        SolrClient.countIncrement()
        logError("Connection timed out!", et)
        server.close()
        server = null
        managerListenerWaiter.post(SolrCollectionTimeout())
      // server.connect()
      case se: org.apache.solr.common.SolrException =>
        SolrClient.countIncrement()
        logError("Could not find a healthy node to handle the request!", se)
        server.close()
        server = null
        managerListenerWaiter.post(SolrNoHelthNode())
      case e: Exception =>
        SolrClient.countIncrement()
        logError("search faield!", e)
        server.close()
        server = null
      //e.printStackTrace()
      //server.close()
      //TODO Log
    }
    response
  }

  override def addIndices[D: ClassTag](zeus: D, collection: String = "searchcloud"): Unit = {
    try {
      val startIndexTime = System.currentTimeMillis()
      logInfo(s"solr start index  time(ms)$startIndexTime,\t current threadName:${Thread.currentThread().getName}")
      if (zeus.isInstanceOf[java.util.List[java.util.Map[java.lang.String, Object]]]) {
        val zList = zeus.asInstanceOf[java.util.List[java.util.Map[java.lang.String, Object]]]
        if (zList.size() > 0) {
          val docList = new java.util.ArrayList[SolrInputDocument]()
          zList.foreach { doc =>
            val docSingle: SolrInputDocument = new SolrInputDocument
            doc.foreach { field =>
              val fieldName = field._1
              val fieldVal = field._2
              if (fieldVal.isInstanceOf[Array[Object]]) {
                //have boost
                val addBoost = fieldVal.asInstanceOf[Array[Object]]
                //set->300->9.6
                docSingle.addField(fieldName, addBoost(0), addBoost(1).asInstanceOf[java.lang.Float])
              } else
                docSingle.addField(fieldName, fieldVal)
            }
            docList.add(docSingle)
          }
          val response = server.add(collection, docList)
          server.commit(collection)
          logInfo("index status:" + response.getStatus)

        } else {
          logError("not input document")

          return new Exception("请传入文档")
        }
      }
      val endIndexTime = System.currentTimeMillis()
      logInfo(s"start index end time(ms)$endIndexTime,\t total cost ${endIndexTime - startIndexTime}(ms)\t current threadId:${Thread.currentThread().getId}")
    } catch {
      case e1: RemoteSolrException => logError("remoteSolrException", e1)
      case e: Exception =>
        logError(s"add index faield$zeus!", e)
        throw new Exception(s"添加索引失败,${e.getMessage}", e.getCause)
    }
  }

  override def updateIndices[D: ClassTag](zeus: D, collection: String = "searchcloud"): Unit = {
    try {
      if (zeus.isInstanceOf[SolrInputDocument]) {
        server.add(collection, zeus.asInstanceOf[SolrInputDocument])
        // server.optimize()
        server.commit(collection)
      } else if (zeus.isInstanceOf[java.util.List[java.util.Map[java.lang.String, Object]]]) {
        //  eg:List(Map("docId1"->32343,"time"->Map("set"->"2015")))
        //eg:List(Map("docId1"->32343,"time"->Map("set"->Array("2015",9.8))))

        val zList = zeus.asInstanceOf[java.util.List[java.util.Map[java.lang.String, Object]]]
        if (zList.size() > 0) {
          val docList = new java.util.ArrayList[SolrInputDocument]()
          zList.foreach { doc =>
            val docSingle: SolrInputDocument = new SolrInputDocument
            doc.foreach { field =>
              val fieldName = field._1
              val fieldVal = field._2

              if (fieldVal.isInstanceOf[java.util.Map[java.lang.String, Object]]) {
                val fieldValMap = fieldVal.asInstanceOf[java.util.Map[java.lang.String, Object]]
                docSingle.addField(fieldName, fieldValMap)
              } else if (fieldVal.isInstanceOf[Array[Object]]) {
                //have boost
                val updateBoost = fieldVal.asInstanceOf[Array[Object]]
                //set->300->9.6
                val uMap = new java.util.LinkedHashMap[String, Object]()
                uMap.put(updateBoost(0).toString, updateBoost(1))
                docSingle.addField(fieldName, uMap, updateBoost(2).asInstanceOf[java.lang.Float])
              } else {
                //uniqueKey
                docSingle.setField(fieldName, fieldVal)
              }
            }
            docList.add(docSingle)
          }
          server.add(collection, docList)
          server.commit(collection)
        } else {
          logError("not input document")
          return new Exception("请传入文档")
        }
      }
    } catch {
      case e: Exception =>
        e.printStackTrace()
        logError("automic update faield!", e)
        throw new Exception(s"原子更新索引失败${e.getMessage}", e.getCause)
    }

  }


  override def deleteByQuery(query: String, collection: String): Boolean = {
    try {
      server.deleteByQuery(collection, query)
      server.commit(collection)
      true
    } catch {
      case e: Exception =>
        logError("deleteById index faield", e)
        false
    }
  }

  override def delete(list: util.ArrayList[String], collection: String = "searchcloud"): Boolean = {
    try {
      server.deleteById(collection, list)
      server.commit(collection)
      true
    } catch {
      case e: Exception =>
        logError("deleteById index faield", e)
        false
    }
  }

  override def close(): Unit = {
    SolJSolrCloudClient.close()
  }

  override def setSolrServer(server: CloudSolrClient): Unit = {
    this.server = server
    SolJSolrCloudClient.switch.compareAndSet(true, false)
    logInfo(s"switch solr 【${server.getZkHost}】 server sucsess")
  }
}


object SolJSolrCloudClient extends SolrConfiguration with Logging {

  val lockSearch = new Object
  val lockKwSearch = new Object
  val lockBackupSearch = new Object

  val BACUUP_SOLR_SERVER = "backup"

  @volatile var switch = new AtomicBoolean(false)


  var solrJClient: SolJSolrCloudClient = null

  def apply(conf: SolrClientConf): SolrClient = {
    if (solrJClient == null) solrJClient = new SolJSolrCloudClient(conf)
    solrJClient
  }

  var server: CloudSolrClient = null
  var kwServer: CloudSolrClient = null
  var serverBackup: CloudSolrClient = null


  def singleCloudInstance(conf: SolrClientConf): CloudSolrClient = {
    instanseSolrCloud(conf, zk)
  }


  def instanseSolrCloud(conf: SolrClientConf, zks: String): CloudSolrClient = {

    if (server == null || SolJSolrCloudClient.switch.get()) {
      lockSearch.synchronized {
        if (server == null || SolJSolrCloudClient.switch.get()) {
          // val zkHostString: String = conf.get("solrj.zk", "solr1:3213,solr2:3213,solr3:3213/solr")
          //server = new CloudSolrClient(zkHostString)
          val params = new ModifiableSolrParams()
          params.set(HttpClientUtil.PROP_MAX_CONNECTIONS, 1000) //10
          params.set(HttpClientUtil.PROP_MAX_CONNECTIONS_PER_HOST, 500) //5
          val client = HttpClientUtil.createClient(params)
          val lbSolrClient = new LBHttpSolrClient(client)
          server = new CloudSolrClient(s"$zks/solr", lbSolrClient)
          server.setDefaultCollection(conf.get("solrj.collection", "searchcloud"))
          server.setZkConnectTimeout(conf.getInt("solrj.zkConnectTimeout", 60000))
          server.setZkClientTimeout(conf.getInt("solrj.zkClientTimeout", 60000))
          server.setRequestWriter(new BinaryRequestWriter())
          server.connect()
          logInfo(s"connect solr cloud instanse ${zks} success!")
        }
      }
    }
    server
  }

  def singleCloudBackupInstance(conf: SolrClientConf): CloudSolrClient = {
    instanseSolrCloud(conf, zkBackUp)
  }


  def connect() = {
    lockSearch.synchronized {
      server.connect
    }
  }

  def close() = {
    lockSearch.synchronized {
      if (server != null)
        server.close
    }
  }

}
