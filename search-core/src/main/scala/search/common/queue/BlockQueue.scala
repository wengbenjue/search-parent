package search.common.queue


import search.solr.client.SolrClientConf
import search.common.queue.impl.SolrClientLinkedBlockQueueImpl

import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/11/23.
  */
private[search] trait BlockQueue[T] {

  def put(task: T): Unit = {}

  def take(): T = {
    null.asInstanceOf[T]
  }

  def size(): Int = {
    -1
  }

  def offer(obj: T): Boolean = {
    false
  }

  def toArray(): Array[AnyRef] = {
    null
  }

  def remove(obj: T): Unit = {}

  def poll():T = null.asInstanceOf[T]

}

private[search] object BlockQueue {

  def apply[T:ClassTag](conf: SolrClientConf, bizCode: String,insType: String="linkedBlock"): BlockQueue[T] = {
    insType match {
      case "linkedBlock" => SolrClientLinkedBlockQueueImpl[T](bizCode, conf)
      case _ => null
    }
  }

  def apply[T:ClassTag](blockLength: Int, conf: SolrClientConf, bizCode: String ,insType: String): BlockQueue[T] = {
    insType match {
      case "linkedBlock" => SolrClientLinkedBlockQueueImpl[T](bizCode, blockLength, conf)
      case _ => null
    }
  }
}
