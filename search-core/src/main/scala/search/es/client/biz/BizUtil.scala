package search.es.client.biz

import java.io.IOException
import java.util.{Calendar, Date}

import com.alibaba.fastjson.{JSON, JSONObject}
import com.mongodb.DBObject
import org.ansj.library.UserDefineLibrary
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import search.common.config.EsConfiguration
import search.common.http.HttpClientUtil
import search.common.util.Logging
import search.es.client.EsClient
import search.es.client.util.EsClientConf

import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2016-10-20.
  */
private[search] object BizUtil extends Logging with EsConfiguration {


  val speechPost = "userDefine"
  val freq = 1000

  def loadDataToDictionary(conf: EsClientConf): Long = {
    getAllFromGraphsToDictionary(graphNodeDataUrl)
    //getDicFromNewsKeywordDictToDictionary(conf)
    -1
  }


  def getDicFromNewsKeywordDictToDictionary(conf: EsClientConf): Unit = {
    var dics: java.util.List[DBObject] = conf.mongoDataManager.getDicFromNewsKeywordDict()
    logInfo("start add keywordDic data to dictionary")
    dics.foreach { dic =>
      val word = if (dic.get("word") != null) dic.get("word").toString.trim else null
      if (word != null) {
        UserDefineLibrary.insertWord(word, speechPost, freq)
        logInfo(s" keywordDic ${word} have loaded to dictionary")
      }
    }
    dics = null
    logInfo(s"finished add keywordDic data to dictionary,total number:${dics.size()}")
  }

  //get all data from graph
  def getAllFromGraphsToDictionary(graphNodeDataUrl: String): Unit = {
    val allNodeJsonObj = requestHttpByURL(graphNodeDataUrl)
    if (allNodeJsonObj != null) {
      var nodes = allNodeJsonObj.getJSONArray("message")
      if (nodes != null && nodes.size() > 0) {
        logInfo("start add graph data to dictionary")
        nodes.foreach { n =>
          UserDefineLibrary.insertWord(n.toString, speechPost, freq)
          logInfo(s" graph ${n.toString} have loaded to dictionary")
        }
        logInfo(s"finished add graph data to dictionary,total number:${nodes.size()}")
      }
      nodes = null
    }
  }


  def requestHttpByURL(url: String): JSONObject = {
    var httpResp: CloseableHttpResponse = null
    try {
      httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
      if (httpResp != null) {
        val entity: HttpEntity = httpResp.getEntity
        if (entity != null) {
          val sResponse: String = EntityUtils.toString(entity)
          val jsonObj = JSON.parseObject(sResponse)
          jsonObj
        } else null
      } else null
    }
    catch {
      case e: IOException => {
        logError("request synonym failed!", e)
        null
      }
    } finally {
      if (httpResp != null)
        HttpClientUtils.closeQuietly(httpResp)
    }

  }


  //index by minutes
  def indexNewsFromMinutes(conf: EsClientConf, client: EsClient, minutes: Int = 5): Long = {
    var indexCalendar = Calendar.getInstance()
    var min = minutes
    if (minutes <= 0 || minutes > 60) min = 5
    val toNowMinutes = new Date()
    indexCalendar.setTime(toNowMinutes)
    indexCalendar.add(Calendar.MINUTE, -min)
    val formNowMinutes = indexCalendar.getTime()
    println(s"formNowMinutes:${formNowMinutes},toNowMinutes:${toNowMinutes}")
    findAndIndexNews(conf,client,formNowMinutes, toNowMinutes)
    -1
  }

  def findAndIndexNews(conf: EsClientConf, client: EsClient, formNowTime: Date, toNowTime: Date): AnyVal = {
    val news = conf.mongoDataManager.findHotNews(formNowTime, toNowTime)
    val newsMapList = NewsUtil.newsToMapCollection(news)
    logInfo(s"total news ${newsMapList.size()} for current batch,formNowTime:${formNowTime},toNowTime:${toNowTime}")
    if (newsMapList != null && newsMapList.size() > 0) {
      client.addDocumentsWithMultiThreading(newsIndexName, newsTypName, newsMapList)
    }
  }

  def deleteNewsByRange(client: EsClient): Long = {
    var calendar = Calendar.getInstance()
    val currentDate = new Date()
    calendar.setTime(currentDate)
    if ("year".equalsIgnoreCase(newsDelInc) || "y".equalsIgnoreCase(newsDelInc) || "Y".equalsIgnoreCase(newsDelInc)) {
      calendar.add(Calendar.YEAR, -newsDelPeiord)
    } else if ("month".equalsIgnoreCase(newsDelInc) || "M".equalsIgnoreCase(newsDelInc)) {
      calendar.add(Calendar.MONTH, -(newsDelPeiord + 1))
    } else if ("day".equalsIgnoreCase(newsDelInc) || "d".equalsIgnoreCase(newsDelInc) || "D".equalsIgnoreCase(newsDelInc)) {
      calendar.add(Calendar.DATE, -newsDelPeiord)
    }
    val delteDateTime = calendar.getTime
    logInfo(s"delete news by range,inc: ${newsDelInc},period:${newsDelPeiord},lte date:${delteDateTime}")
    client.delByRange(newsIndexName, newsTypName, "create_on", null, delteDateTime)
    -1
  }


  def main(args: Array[String]) {
    //getAllFromGraphsToDictionary(graphNodeDataUrl)
    indexNewsFromMinutes(null,null,8)
  }
}
