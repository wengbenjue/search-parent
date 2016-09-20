package search.es.client.biz

import java.util

import scala.collection.JavaConversions._
import com.mongodb.DBObject
import shapeless.get

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] object NewsUtil {

  def newsToMapCollection(news: java.util.List[DBObject]): util.Collection[java.util.Map[String, Object]] = {
    if (news == null || news.size() == 0) return null
    news.foreach { dbObj =>
      val id = if(dbObj.get("uid") != null)  dbObj.get("uid").toString else  null
      val title = if(dbObj.get("t") != null)  dbObj.get("t").toString else  null
      val auth = if(dbObj.get("auth") != null)  dbObj.get("auth").toString else  null
      val summary = if(dbObj.get("sum") != null)  dbObj.get("sum").toString else  null
      val createOn = if(dbObj.get("dt") != null)  dbObj.get("dt").toString else  null
      val companys = if(dbObj.get("uid") != null)  dbObj.get("uid").toString else  null
      val events = if(dbObj.get("uid") != null)  dbObj.get("uid").toString else  null
      val topics = if(dbObj.get("uid") != null)  dbObj.get("uid").toString else  null
    }
    null
  }

}
