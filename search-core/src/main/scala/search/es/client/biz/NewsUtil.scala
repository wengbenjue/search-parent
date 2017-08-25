package search.es.client.biz

import java.io.PrintWriter
import java.util

import scala.collection.JavaConversions._
import com.mongodb.{BasicDBList, DBObject}
import search.common.cache.impl.LocalCache
import search.common.util.{Logging, Util}
import shapeless.get

import scala.collection.mutable

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] object NewsUtil extends Logging{
  val authSet = new mutable.HashSet[String]()

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

          val url = if (dbObj.get("url") != null) dbObj.get("url").toString.trim else null
          if (url != null && !"".equalsIgnoreCase(url)) map.put("url", url)

          val auth = if (dbObj.get("auth") != null) dbObj.get("auth").toString.trim else null
          if (auth != null && !"".equalsIgnoreCase(auth)) {
            map.put("auth", auth)
            //authSet += auth
          }
          val summary = if (dbObj.get("sum") != null) dbObj.get("sum").toString.trim else null
          if (summary != null && !"".equalsIgnoreCase(summary)) map.put("summary", summary)
          val createOn = if (dbObj.get("dt") != null) dbObj.get("dt").toString else null
          if (createOn != null && !"".equalsIgnoreCase(createOn)) map.put("create_on", Util.stringToDate(createOn))



          val companys = if (dbObj.get("asw") != null) dbObj.get("asw").asInstanceOf[BasicDBList] else null
          if (companys != null && companys.size() > 0) {
            val multFieldList = new util.HashSet[String]()
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
            val multFieldList = new util.HashSet[String]()
            events.foreach{s =>
              val eS = s.toString.trim.replaceAll("[\\|||]\\S+","")
              if(LocalCache.eventSet.contains(eS))
              multFieldList.add(s.toString)
            }
            if (multFieldList.size() > 0)
              map.put("events", multFieldList)
          }
          val topics = if (dbObj.get("topic") != null) dbObj.get("topic").asInstanceOf[BasicDBList] else null
          if (topics != null && topics.size() > 0) {
            val multFieldList = new util.HashSet[String]()
            topics.foreach { s =>
              val dbObj = s.asInstanceOf[DBObject]
              val topic = if (dbObj.get("topic") != null) dbObj.get("topic").toString.trim else null
              if (topic != null && !"".equalsIgnoreCase(topic))
                multFieldList.add(topic)
            }
            if (multFieldList.size() > 0)
              map.put("topics", multFieldList)
          }

          val kws = if (dbObj.get("kw") != null) dbObj.get("kw").asInstanceOf[BasicDBList] else null
          if (kws != null && kws.size() > 0) {
            val multFieldList = new util.HashSet[String]()
            kws.foreach(s => multFieldList.add(s.toString))
            if (multFieldList.size() > 0)
              map.put("kw", multFieldList)
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

  def writeAuthToFile(auths: mutable.Set[String]) = {
    val out = new PrintWriter("D:\\java\\auth.txt")
    auths.foreach(a=>out.println(a))
    out.close()
    println(s"成功写入新闻来源文件个数: ${auths.size}")
  }

}

object TestNewsUtil{
  def main(args: Array[String]) {
    val event = "震荡|2"
    val rEvent = event.replaceAll("[\\|||]\\S+","")
    println(rEvent)
  }
}
