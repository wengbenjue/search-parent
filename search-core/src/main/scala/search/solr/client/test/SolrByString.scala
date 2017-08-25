package search.solr.client.test

import java.net.URLEncoder

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.response.QueryResponse
import search.solr.client.{SolrClientConf, SolrClient}
import scala.collection.JavaConversions._

/**
  * Created by soledede on 2015/11/20.
  */
object SolrByString {

  def main(args: Array[String]) {
    val query: SolrQuery = new SolrQuery
    query.setRequestHandler("/select")
    query.setQuery("工具")
    //query.setRows(10)
    //query.setStart(0)
    val client =SolrClient(new SolrClientConf())
    val r =   client.searchByQuery[SolrQuery](query).asInstanceOf[QueryResponse]
    r.getResults.foreach(println)
    client.close()

  }
}
