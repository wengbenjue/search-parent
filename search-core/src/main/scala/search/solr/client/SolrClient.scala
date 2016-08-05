package search.solr.client

import java.util.concurrent.atomic.AtomicInteger

import org.apache.solr.client.solrj.impl.{HttpSolrClient, CloudSolrClient}
import search.solr.client.impl.{HttpSolrQuery, SolJSolrCloudClient}
import search.solr.client.solrj.SorlCLoudClientSolrJ

import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/11/16.
  */
private[search] trait SolrClient {
  def deleteByQuery(query: String, collection: String): Boolean = false

  def searchByQuery[T: ClassTag](query: T, collection: String = "searchcloud"): AnyRef = null

  def updateIndices[D: ClassTag](doc: D, collection: String = "searchcloud"): Unit = {}

  def addIndices[D: ClassTag](doc: D, collection: String = "searchcloud"): Unit = {}

  def delete(list: java.util.ArrayList[java.lang.String], collection: String = "searchcloud"): Boolean = false

  def close(): Unit = {}

  def closeKw(): Unit = {}

  def setSolrServer(server: CloudSolrClient): Unit = {}

}

private[search] object SolrClient {
  var faieldCOunt = new AtomicInteger(0)

  def apply(conf: SolrClientConf, cType: String = "solrJSolrCloud"): SolrClient = {
    cType match {
      case "solrJSolrCloud" => SolJSolrCloudClient(conf)
      case "httpUrl" => HttpSolrQuery(conf)
      case _ => null
    }
  }

  def switchClient(oldClient: org.apache.solr.client.solrj.SolrClient, url: String): org.apache.solr.client.solrj.SolrClient = {
    var changeSolrClient: org.apache.solr.client.solrj.SolrClient = oldClient
    if (faieldCOunt.get() > 5) {
      if (oldClient.isInstanceOf[HttpSolrClient]) {
        changeSolrClient = SolJSolrCloudClient.singleCloudInstance(new SolrClientConf())
      } else if (oldClient.isInstanceOf[CloudSolrClient]) {
        changeSolrClient = HttpSolrQuery.singletonHttpSolrClient(url)
      }
      faieldCOunt.getAndSet(0)
    }
    changeSolrClient
  }

  def countIncrement() = {
    faieldCOunt.incrementAndGet()
  }



}
