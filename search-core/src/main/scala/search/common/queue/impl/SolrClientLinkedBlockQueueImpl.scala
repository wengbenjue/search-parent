package search.common.queue.impl

import java.util.concurrent.LinkedBlockingQueue

import search.solr.client.SolrClientConf
import search.common.queue.BlockQueue
import search.common.util.Logging
import scala.collection.mutable.HashMap

import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/11/23.
  */
private[search] class SolrClientLinkedBlockQueueImpl[T: ClassTag] private(conf: SolrClientConf, blockLength: Int) extends BlockQueue[T] with Logging {
  def this(conf: SolrClientConf) = this(conf, -1)

  private var blockSize = conf.getInt("solrclient.linked.block.queue.block.size", 100000)

  if (blockLength != -1) blockSize = blockLength

  private val taskBlockingQueue = new LinkedBlockingQueue[T](blockSize)

  override def put(task: T): Unit = {
    this.taskBlockingQueue.put(task)
  }

  override def offer(obj: T): Boolean = {
    this.taskBlockingQueue.offer(obj)
  }

  override def take(): T = {
    this.taskBlockingQueue.take()
  }

  override def size(): Int = {
    this.taskBlockingQueue.size()
  }

  override def remove(obj: T): Unit = {
    this.taskBlockingQueue.remove(obj)
  }

  override def toArray(): Array[AnyRef] = {
    this.taskBlockingQueue.toArray
  }

  override def poll(): T = {
   this.taskBlockingQueue.poll()
  }
}

private[search] object SolrClientLinkedBlockQueueImpl {
  private val instanceMap = new HashMap[String, AnyRef]()

  def apply[T:ClassTag](bizCode: String, conf: SolrClientConf): SolrClientLinkedBlockQueueImpl[T] = {
    if (!instanceMap.contains(bizCode))
      instanceMap(bizCode) = new SolrClientLinkedBlockQueueImpl[T](conf)
    instanceMap(bizCode).asInstanceOf[SolrClientLinkedBlockQueueImpl[T]]
  }

  def apply[T:ClassTag](bizCode: String, blockLength: Int, conf: SolrClientConf): SolrClientLinkedBlockQueueImpl[T] = {
    if (!instanceMap.contains(bizCode))
      instanceMap(bizCode) = new SolrClientLinkedBlockQueueImpl[T](conf, blockLength)
    instanceMap(bizCode).asInstanceOf[SolrClientLinkedBlockQueueImpl[T]]
  }
}
