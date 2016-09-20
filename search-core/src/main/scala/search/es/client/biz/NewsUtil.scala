package search.es.client.biz

import java.util

import scala.collection.JavaConversions._
import com.mongodb.{BasicDBList, DBObject}
import search.common.cache.impl.LocalCache
import search.common.util.{Logging, Util}
import shapeless.get

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] object NewsUtil extends Logging{

  def newsToMapCollection(news: java.util.List[DBObject]): util.Collection[java.util.Map[String, Object]] = {
    val mapList = new util.ArrayList[java.util.Map[String, Object]]()
    try {
      if (news == null || news.size() == 0) return mapList
      news.foreach { dbObj =>
        val map = new java.util.HashMap[String, Object]
        val id = if (dbObj.get("uid") != null) dbObj.get("uid").toString.trim else null
        if (id != null && !"".equalsIgnoreCase(id)) {
          map.put("_id", id)
          val title = if (dbObj.get("t") != null) dbObj.get("t").toString.trim else null
          if (title != null && !"".equalsIgnoreCase(title)) map.put("title", title)
          val auth = if (dbObj.get("auth") != null) dbObj.get("auth").toString.trim else null
          if (auth != null && !"".equalsIgnoreCase(auth)) map.put("auth", auth)
          val summary = if (dbObj.get("sum") != null) dbObj.get("sum").toString.trim else null
          if (summary != null && !"".equalsIgnoreCase(summary)) map.put("summary", summary)
          val createOn = if (dbObj.get("dt") != null) dbObj.get("dt").toString else null
          if (createOn != null && !"".equalsIgnoreCase(createOn)) map.put("create_on", Util.stringToDate(createOn))
          val companys = if (dbObj.get("asw") != null) dbObj.get("asw").asInstanceOf[BasicDBList] else null
          if (companys != null && companys.size() > 0) {
            val multFieldList = new util.ArrayList[String]()
            companys.foreach { s =>
              val comCode = s.toString
              if (comCode != null && !comCode.trim.equalsIgnoreCase("")) {
                if (LocalCache.codeToCompanyNameCache.containsKey(comCode.trim)) {
                  multFieldList.add(LocalCache.codeToCompanyNameCache(comCode.trim))
                }
              }
            }
            if (multFieldList.size() > 0)
              map.put("companys", multFieldList)
          }
          val events = if (dbObj.get("rule") != null) dbObj.get("rule").asInstanceOf[BasicDBList] else null
          if (events != null && events.size() > 0) {
            val multFieldList = new util.ArrayList[String]()
            events.foreach(s => multFieldList.add(s.toString))
            if (multFieldList.size() > 0)
              map.put("events", multFieldList)
          }
          val topics = if (dbObj.get("topic") != null) dbObj.get("topic").asInstanceOf[BasicDBList] else null
          if (topics != null && topics.size() > 0) {
            val multFieldList = new util.ArrayList[String]()
            topics.foreach { s =>
              val dbObj = s.asInstanceOf[DBObject]
              val topic = if (dbObj.get("topic") != null) dbObj.get("topic").toString.trim else null
              if (topic != null && !"".equalsIgnoreCase(topic))
                multFieldList.add(topic)
            }
            if (multFieldList.size() > 0)
              map.put("topics", multFieldList)
          }
        }
        if (!map.isEmpty)
          mapList.add(map)
      }
    } catch {
      case e: Exception => logError("news convert to map from mongo failed!",e)
    }
    mapList
  }

}
