package search.solr.client.index.manager

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.protocol.HttpContext
import search.common.entity.enumeration.HttpRequestMethodType
import search.solr.client.index.manager.impl.DefaultIndexManager
import search.common.util.Logging

import scala.collection.mutable

/**
  * Created by soledede on 2016/2/15.
  */
trait IndexManager extends Logging {

  def requestData(message: String): AnyRef

  def geneXml(data: AnyRef,collection: String): AnyRef

  def indexData(data: AnyRef,collection: String): Boolean

  def delete(ids: java.util.ArrayList[java.lang.String],collection: String): Boolean

  def execute(request: HttpRequestBase, context: HttpClientContext, callback: (HttpContext, HttpResponse) => Unit): Unit = null
}

object IndexManager {
  def apply(indexer: String = "default"): IndexManager = {
    indexer match {
      case "default" => DefaultIndexManager()
      case _ => null
    }
  }
}
