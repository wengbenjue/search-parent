package search.solr.client.product

import search.common.config.{SolrConfiguration, Configuration}
import search.common.entity.searchinterface.NiNi
import search.common.queue.MessageQueue
import search.common.util.{Util, Logging}
import scala.collection.JavaConversions._

/**
  * Created by soledede on 2016/2/14.
  */
private[search] object Producter extends Logging with SolrConfiguration {

  val separator = "-"
  val DELETE = "delete"
  val DELETE_BY_QUERY = "delquery"


/*  def send(startUpdateTime: Long, endUpdataTime: Long, totalNum: Int): Boolean = {
    send(collection, startUpdateTime, endUpdataTime, totalNum)
  }*/

/*
  def send(minUpdateTime: Long, totalNum: Int): Boolean = {
    send(collection, minUpdateTime, totalNum)
  }
*/


  def delete(id: String): Boolean = {
    delete(collection, id)
  }

  def delete(ids: java.util.List[String]): NiNi = {
    delete(collection, ids)
  }

  /**
    * @param collection
    * @param startUpdateTime
    * @param endUpdataTime
    * @param totalNum
    * eg: mergescloud-2343433212-234343211-34
    * mergescloud-1456329600-1456477928-1
    * mergescloud-null-null-259417
    * mergescloud_test-null-null-772554
    * mergescloud-null-null-772554
    * screencloud-null-null-25468
    * screencloud_test-null-null-27174
    * mergescloud_prod-null-null-258276
    * screencloud_prod-null-null-8021
    * screencloud-null-null-8017
    * mergescloud-null-null-259252
    * mergescloud_prod-null-null-2
    * mergescloud_prod-null-null-261556
    *
    * bin/kafka-console-producer.sh --broker-list 121.40.54.54:9092 --topic indexManagesTest
    * @return
    * for NiNi
    * actural Boolean
    */
 def send(collection: String, startUpdateTime: Long, endUpdataTime: Long, totalNum: Int): NiNi = {
    Util.caculateCostTime {
      logInfo(s"sendMessage-collection:$collection-startTime:$startUpdateTime-endTime:$endUpdataTime-totalNum:$totalNum")
      if (MessageQueue().sendMsg(collection + separator + startUpdateTime + separator + endUpdataTime + separator + totalNum)) true
      else false
    }
  }

  /**
    * @param collection
    * @param minUpdateTime
    * @param totalNum
    * eg:mergescloud-234343211-34
    * screencloud-234343211-34
    * mergescloud-1456329600-10
    * mergescloud-1456219967513-297840
    * mergescloud-1455960816240-715520
    * mergescloud-1457286099000-1
    * screencloud-1456329600-10
    * @return
    * for NiNi
    * actural Boolean
    */
 def send(collection: String, minUpdateTime: Long, totalNum: Int): NiNi = {
    Util.caculateCostTime {
      logInfo(s"sendMessage-collection:$collection-minUpdateTime:$minUpdateTime-totalNum:$totalNum")
      if (MessageQueue().sendMsg(collection + separator + minUpdateTime + separator + totalNum)) true
      else false
    }
  }


  /**
    * @param collection
    * @param id
    * delete single id
    * eg: mergescloud-delete-100429
    * screencloud-delete-1003484_t87_s
    */
  private def delete(collection: String, id: String): Boolean = {
    logInfo(s"deleteMessage-collection:$collection-id:$id")
    if (MessageQueue().sendMsg(collection + separator + DELETE + separator + id)) true
    else false
  }

  /**
    * @param collection
    * @param ids
    * delete multiple ids
    * eg:mergescloud-delete-109432-1003435-2562234
    * screencloud-delete-109432-1003435-2562234
    * return
    * for NiNi
    * actural Boolean
    *
    */
   def delete(collection: String, ids: java.util.List[String]): NiNi = {
    Util.caculateCostTime {
      val idMsg = new StringBuilder()
      if (ids != null && ids.size() > 0) {
        ids.foreach(idMsg.append(_).append(separator))
        idMsg.deleteCharAt(idMsg.length - 1)
        logInfo(s"deleteMessage--collection:$collection-id:${idMsg.toString()}")
        if (MessageQueue().sendMsg(collection + separator + DELETE + separator + idMsg.toString())) true
        else false
      } else false
    }
  }

  //test dev env
  /* def deleteAll(collection: String): Boolean = {
     val query = "*:*"
     if (MessageQueue().sendMsg(collection + separator + DELETE_BY_QUERY + separator + query)) true
     else false
   }*/

  //product env
  private def deleteAll(collection: String): Boolean = {
    val query = "*:*"
    if (MessageQueue().sendMsg(collection + separator + DELETE_BY_QUERY + separator + query)) true
    else false
  }

  /**
    *
    * this is generate  inteface for product message,whenever you want to expand your function
    *
    * @param msg
    * @return
    * eg: test-234-3423-445
    */
  private def sendMsg(msg: String): Boolean = {
    logInfo(s"customSendMessage-message:$msg")
    if (MessageQueue().sendMsg(msg)) true
    else false
  }

  def main(args: Array[String]) {
    // testeleteAll
    testSend
  }

  def testeleteAll() = {
    //Producter.deletell("mergescloud_prod")
    //  Producter.deleteAll("screencloud_prod")
  }

  def testSend() = {
    Producter.send("mergescloud_prod ", 0L, 0L, 5)
    Producter.send("screencloud_prod", 0L, 0L, 9286)
  }

}

