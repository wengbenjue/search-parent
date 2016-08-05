package search.solr.client.user

import java.util.concurrent._

import com.google.common.util.concurrent.{Futures, FutureCallback, MoreExecutors}
import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.{SolrInputDocument, SolrDocumentList, SolrDocument}
import search.common.entity.IndexResult
import search.solr.client.index.manager.Indexer
import search.solr.client.{SolrClientConf, SolrClient}

import scala.reflect.ClassTag


/**
  * Created by soledede on 2015/11/16.
  */
private[search] class SorlClientUI {
 // val executor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(Runtime.getRuntime.availableProcessors(), Runtime.getRuntime.availableProcessors(), 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue[Runnable](10000)));
  val executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors()))


  val indexer = Indexer(new SolrClientConf())
  // indexer.initBlockQueue[java.util.List[java.util.Map[String, Object]]]()
  //indexer.start

  val c = SolrClient(new SolrClientConf())


  def searchByQuery(query: SolrQuery): QueryResponse = searchByQuery(query, "searchcloud")

  /**
    *
    * @param query
    * @param collection
    * @returns
    */
  def searchByQuery(query: SolrQuery, collection: String = "searchcloud"): QueryResponse = {
    val res = c.searchByQuery[SolrQuery](query, collection).asInstanceOf[QueryResponse]
    res
  }

  /**
    *
    * @param doc
    * @return
    */
  def updateIndexByDoc(doc: SolrInputDocument): Boolean = updateIndexByDoc(doc, "searchcloud")

  /**
    *
    * @param doc
    * @param collection
    * @return
    */
  def updateIndexByDoc(doc: SolrInputDocument, collection: String = "searchcloud"): Boolean = {
    try {
      c.updateIndices(doc, collection)
      true
    } catch {
      case e: Exception => false
    }
  }

  /**
    *
    * @param mapSet
    */
  def updateIndicesByMapSet(mapSet: java.util.List[java.util.Map[String, Object]]): Boolean = updateIndicesByMapSet(mapSet, "searchcloud")

  /**
    *
    * @param mapSet
    * eg:List(Map("docId1"->32343,"time"->Map("set"->"2015")))
    * @param collection
    */
  def updateIndicesByMapSet(mapSet: java.util.List[java.util.Map[String, Object]], collection: String = "searchcloud"): Boolean = {
    try {
      c.updateIndices(mapSet, collection)
      true
    } catch {
      case e: Exception => false
    }
  }

  def addIndices(mapSet: java.util.List[java.util.Map[String, Object]]): Boolean = addIndices(mapSet,"searchcloud")

  /**
    *
    * @param mapSet
    * @param collection
    * @return
    */
  def addIndices(mapSet: java.util.List[java.util.Map[String, Object]], collection: String = "searchcloud"): Boolean = {
    try {
      c.addIndices(mapSet, collection)
      true
    } catch {
      case e: Exception => false
    }
  }


  def asynUpdateIndicesByMapSet(mapSet: java.util.List[java.util.Map[String, Object]], future: FutureCallback[IndexResult]): Unit = asynUpdateIndicesByMapSet(mapSet, future, "searchcloud")


  def asynUpdateIndicesByMapSet(mapSet: java.util.List[java.util.Map[String, Object]], future: FutureCallback[IndexResult], collection: String = "searchcloud"): Unit = {
    val execTask = executor.submit(new IndexTask[IndexResult, java.util.List[java.util.Map[String, Object]]](mapSet))
    Futures.addCallback(execTask, future, Executors.newSingleThreadExecutor())
    executor.shutdown
     while (!executor.isTerminated()) {
       executor.awaitTermination(java.lang.Long.MAX_VALUE, TimeUnit.NANOSECONDS)
   }
  }


  private class IndexTask[T: ClassTag, D: ClassTag](doc: D) extends Callable[T] {
    override def call(): T = {
      val result = new IndexResult()
      try {
        c.updateIndices(doc)
        result.asInstanceOf[T]
      } catch {
        case e: Exception =>
          result.code = -1
          result.msg = "failed"
          result.error = e.getMessage
          result.asInstanceOf[T]
      }
    }
  }


  def close(): Unit = {
    c.close()
  }
}

private[search] object SorlClientUI {
  var clUI: SorlClientUI = null

  def singleInstanceUI(): SorlClientUI = {
    if (clUI == null) clUI = new SorlClientUI()
    clUI
  }
}
