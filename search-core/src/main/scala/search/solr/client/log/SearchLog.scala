package search.solr.client.log

import search.solr.client.log.impl.MongoSearchLogImpl

/**
  * Created by soledede on 2016/2/24.
  */
trait SearchLog extends Log {

  def write(keyWords: java.lang.String, appKey: java.lang.String, clientIp: java.lang.String, userAgent: java.lang.String, sourceType: java.lang.String, cookies: java.lang.String, userId: java.lang.String,currentTime: java.util.Date)

  def write(logMap: java.util.Map[String,Object]): Unit = null

}

object SearchLog {
  def apply(searchType: String = "mongo"):SearchLog = {
    searchType match {
      case "mongo" => MongoSearchLogImpl()
      case "hbase" => null
      case _ => null
    }
  }
}
