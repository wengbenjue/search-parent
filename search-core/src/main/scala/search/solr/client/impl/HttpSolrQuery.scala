package search.solr.client.impl

import java.util
import java.util.concurrent.atomic.AtomicInteger

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.HttpSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import search.solr.client.{SolrClientConf, SolrClient}
import search.common.config.{SolrConfiguration, Configuration}
import search.common.util.Logging

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/12/16.
  */
class HttpSolrQuery extends SolrClient with SolrConfiguration with Logging {

  var server: HttpSolrClient = null

  override def searchByQuery[T: ClassTag](query: T, collection: String): AnyRef = {
    val urls = HttpSolrQuery.solrBaseUrlArray
    val aSize = urls.length
    if (urls != null && aSize > 0) {
      val point = HttpSolrQuery.urlPoint.getAndIncrement()
      HttpSolrQuery.urlPoint.compareAndSet(aSize, 0)
      searchByQuery(urls(point % aSize), query.asInstanceOf[SolrQuery], collection)
    } else {
      logError("solr urls is null,set solr urls please!")
      null
    }
  }

  private def searchByQuery[T: ClassTag](baseUrl: String, query: SolrQuery, collection: String): AnyRef = {
    var response: QueryResponse = null
    try {
      if (server == null) server = HttpSolrQuery.singletonHttpSolrClient(baseUrl)
      server = SolrClient.switchClient(server, baseUrl).asInstanceOf[HttpSolrClient]
      response = server.query(collection, query)
    } catch {
      case e: Exception =>
        SolrClient.countIncrement()
        logError("solr query faield!", e)
    }
    response
  }
}

object HttpSolrQuery extends SolrConfiguration {
  var server: HttpSolrClient = null
  var serverMap: mutable.Map[String, HttpSolrClient] = new mutable.HashMap[String, HttpSolrClient]()
  var query: HttpSolrQuery = null

  final val urlSepator = ","
  var urlPoint = new AtomicInteger(0)


  var solrBaseUrlArray: Array[String] = null

  if (solrBaseUrls != null)
    solrBaseUrlArray = solrBaseUrls.split(urlSepator)

  def apply(conf: SolrClientConf): HttpSolrQuery = {
    if (query == null) query = new HttpSolrQuery
    query
  }

  def singletonHttpSolrClient(url: String): HttpSolrClient = {
    if (!serverMap.contains(url.trim)) {
      val server = new HttpSolrClient(url)
      if (url != null && !url.equalsIgnoreCase(""))
        server.setBaseURL(url)
      //server.setMaxRetries(1); // defaults to 0.  > 1 not recommended.
      server.setConnectionTimeout(6*60 * 1000) // 1 minute to establish TCP
      // Setting the XML response parser is only required for cross
      // version compatibility and only when one side is 1.4.1 or
      // earlier and the other side is 3.1 or later.
      //server.setParser(new XMLResponseParser()); // binary parser is used by default
      // The following settings are provided here for completeness.
      // They will not normally be required, and should only be used
      // after consulting javadocs to know whether they are truly required.
      server.setSoTimeout(5*60*1000) // socket read timeout
      server.setDefaultMaxConnectionsPerHost(500)
      server.setMaxTotalConnections(1000)
      // server.setFollowRedirects(false); // defaults to false
      // allowCompression defaults to false.
      // Server side must support gzip or deflate for this to have any effect.
      //server.setAllowCompression(true);
      serverMap(url.trim) = server
    }
    serverMap(url.trim)
  }


  def main(args: Array[String]) {
    //  val url = "http://121.40.241.26:10032/solr"
    val query: SolrQuery = new SolrQuery()
    query.setRequestHandler("/select")
    query.setQuery("*:*")
    query.setStart(0)
    query.setRows(10)
    var r = HttpSolrQuery(new SolrClientConf()).searchByQuery(query, "mergescloud")
    r = HttpSolrQuery(new SolrClientConf()).searchByQuery(query, "mergescloud")
    r = HttpSolrQuery(new SolrClientConf()).searchByQuery(query, "mergescloud")
    r = HttpSolrQuery(new SolrClientConf()).searchByQuery(query, "mergescloud")
    r = HttpSolrQuery(new SolrClientConf()).searchByQuery(query, "mergescloud")
    println(r)
  }
}