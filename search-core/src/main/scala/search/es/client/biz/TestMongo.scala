package search.es.client.biz

import java.util
import java.util.{Calendar, Date}

import search.es.client.util.EsClientConf

import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2017-09-04.
  */
object TestMongo {

  def main(args: Array[String]) {
    testStasticFetch
  }

  def testStasticFetch() = {
    val conf = new EsClientConf
    val set = new util.HashSet[String]()
    conf.initMongo()
    var calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH,4)
    calendar.set(Calendar.HOUR_OF_DAY,0)
    calendar.set(Calendar.SECOND,0)
    val news = conf.mongoDataManager.findFetchNews(calendar.getTime, null)
    val newsUrl =news.map{db=>
      db.get("url").toString
    }
    set.addAll(newsUrl)
    println(s"news size=${newsUrl.size},set news size =${set.size()}")
    //println(news)
  }

}
