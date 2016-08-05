package search.solr.client.log.impl

import java.util

import com.mongodb.BasicDBObject
import search.es.client.biz.BizeEsInterface
import search.es.client.util.EsClientConf
import search.solr.client.log.SearchLog
import scala.collection.JavaConversions._

//import search.common.mongo.MongoStorage

/**
  * Created by soledede on 2016/2/24.
  */
class MongoSearchLogImpl() extends SearchLog {
  override def write(keyWords: String, appKey: String, clientIp: String, userAgent: String, sourceType: String, cookies: String, userId: String, currentTime: java.util.Date): Unit = {
    val map = new util.HashMap[String, Object]()
    map.put("keyWords", keyWords)
    map.put("appKey", appKey)
    map.put("clientIp", clientIp)
    map.put("userAgent", userAgent)
    map.put("sourceType", sourceType)
    map.put("cookies", cookies)
    map.put("userId", userId)
    map.put("currentTime", currentTime)
    // MongoStorage.saveMap("searchlogcollection", map)
  }

  override def write(logMap: util.Map[String, Object]): Unit = {
    val dbObj = new BasicDBObject()
    logMap.foreach { case (k, v) =>
      dbObj.put(k, v)
    }
    BizeEsInterface.conf.mongoDataManager.getLogCollection.insert(dbObj)
    //MongoStorage.saveMap("searchlogcollection", logMap)
  }
}

object MongoSearchLogImpl {
  var mongoSearchLogImpl: MongoSearchLogImpl = null

  def apply(): MongoSearchLogImpl = {
    if (mongoSearchLogImpl == null) mongoSearchLogImpl = new MongoSearchLogImpl()
    mongoSearchLogImpl
  }
}