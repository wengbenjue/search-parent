package search.solr.client.consume

import java.util

import search.solr.client.SolrClientConf
import search.common.config.{SolrConfiguration, Configuration}
import search.solr.client.index.manager.{IndexManagerRunner, IndexManager}
import search.common.queue.MessageQueue
import search.common.queue.impl.KafkaMessageQueue
import search.common.util.{Util, Logging}
import search.common.view.control.ControlWebView
import scala.collection.JavaConversions._

/**
  * Created by soledede on 2016/2/14.
  */
object Consumer extends Logging with SolrConfiguration {


  var indexer: IndexManager = _


  if (useSolr) init

  def init() = {
    indexer = IndexManager()
    if (webStart) {
      val web = new ControlWebView(monitorPort, new SolrClientConf())
      web.bind()
    }
  }

  /* var coreThreadsNumber = consumerCoreThreadsNum

   var moreThreadsNumber = 0

   if (consumerThreadsNum % Util.inferCores() > 0) moreThreadsNumber = 1


   if (consumerThreadsNum > 0) coreThreadsNumber = consumerThreadsNum / Util.inferCores() + moreThreadsNumber

   val consumerManageThreadPool = Util.newDaemonFixedThreadPool((Util.inferCores() * coreThreadsNumber), "consumer_manage_thread_excutor")*/

  def main(args: Array[String]) {
    if (startKafka) {
      MessageQueue().start() //start recieve message,default we use kafka
      receive
    }

  }

  def receive(): Unit = {
    while (true) {
      try {
        val message = KafkaMessageQueue.kafkaBlockQueue.take()
        if (message != null && !message.trim.equalsIgnoreCase("") && message.length > 0) {
          //consumerManageThreadPool.execute(new IndexManagerRunner(message, indexer))

          indexer.requestData(message)

          // val collection = message.split(Producter.separator)(0)
          // val data = indexer.requestData(message)
          //generate xml for data
          /*  val xmlBool = indexer.geneXml(data,collection)
            if (xmlBool != null) {
              indexData(collection, xmlBool)
              /* if (xmlBool.isInstanceOf[java.util.ArrayList[java.lang.String]]) {
                 deleteIndexData(collection, xmlBool)
               }
               else {
                 indexData(collection, xmlBool)
               }*/
            }*/
        }
      } catch {
        case e: Exception => logError("manager index faield!", e)
      }
    }
  }

  /**
    * delete index data
    *
    * @param collection
    * @param xmlBool
    */
  def deleteIndexData(collection: String, xmlBool: AnyRef): Unit = {
    val delData = xmlBool.asInstanceOf[util.ArrayList[java.lang.String]]
    if (indexer.delete(delData, collection)) {
      logInfo("delete index success!")
    } else {
      logError("delete index faield!Ids:")
      delData.foreach(id => logInfo(s"delete faield id:\t${id}"))
    }
  }

  /**
    * index data
    *
    * @param collection
    * @param xmlBool
    */
  def indexData(collection: String, xmlBool: AnyRef): Unit = {
    try {
      val indexData = xmlBool.asInstanceOf[java.util.ArrayList[java.util.Map[java.lang.String, Object]]]
      indexData(0).asInstanceOf[java.util.Map[java.lang.String, Object]]
      if (indexer.indexData(indexData, collection)) logInfo(" index success!")
      else {
        logError("index faield!Ids:")
        indexData.foreach { doc =>
          logInfo(s"index faield id:\t${doc.get("id")}")
        }
      }
    } catch {
      case castEx: java.lang.ClassCastException =>
        deleteIndexData(collection, xmlBool)
      // case e: Exception => logError("index faield", e)
    }

  }
}
