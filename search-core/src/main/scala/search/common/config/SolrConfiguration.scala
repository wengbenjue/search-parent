package search.common.config

import com.typesafe.config.ConfigFactory
import search.common.util.Util

import scala.util.Try

/**
  * Created by soledede.weng on 2016/7/27.
  */
trait SolrConfiguration extends Configuration {

  val solrConfig = ConfigFactory.load("solr.conf")
  val useSolr = Try(solrConfig.getBoolean("solr.use")).getOrElse(false)


  lazy val solrBaseUrls = Try(solrConfig.getString("solr.baseUrls")).getOrElse("localhost:9092")


  /**
    * generate xml
    */
  lazy val multiValuedString = Try(solrConfig.getString("field.multivalued")).getOrElse("")
  lazy val filedirMergeCloud = Try(solrConfig.getString("filedir.mergeclouds")).getOrElse("")
  lazy val filedirScreenCloud = Try(solrConfig.getString("filedir.screenclouds")).getOrElse("")

  /**
    * filter
    *
    */

  lazy val filterChanges = Try(solrConfig.getString("filter.change")).getOrElse("")

  /**
    * solr
    */

  lazy val collection = Try(solrConfig.getString("collection")).getOrElse("")

  lazy val defaultCollection = Try(solrConfig.getString("defaultcollection")).getOrElse("")

  lazy val defaultAttrCollection = Try(solrConfig.getString("defaultattrcollection")).getOrElse("")


  lazy val defaultSuggestCollection = Try(solrConfig.getString("defaultsuggestcollection")).getOrElse("")


  /**
    * remote http url
    */
  lazy val mergesCloudsUrl = Try(solrConfig.getString("url.mergeclouds")).getOrElse("http://localhost:8088/mergeclouds")
  lazy val screenCloudsUrl = Try(solrConfig.getString("url.screenclouds")).getOrElse("http://localhost:8088/screenclouds")

  //page
  lazy val pageSize = Try(solrConfig.getInt("pageSize")).getOrElse(10)

  lazy val productCollection = Try(config.getString("productCollection")).getOrElse("mergescloud_prod")
}
