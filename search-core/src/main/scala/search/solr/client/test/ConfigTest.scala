package search.solr.client.test

import search.common.config.{SolrConfiguration, Configuration}

/**
  * Created by soledede on 2016/2/14.
  */
object ConfigTest extends SolrConfiguration{

  def main(args: Array[String]) {
    println("serviceHost="+brokers)
  }

}
