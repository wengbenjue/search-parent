package search.es.client.biz

import java.io.IOException

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
    getDicFromNewsKeywordDictToDictionary(conf)
    -1
  }


  def getDicFromNewsKeywordDictToDictionary(conf: EsClientConf): Unit = {
    val dics: java.util.List[DBObject] = conf.mongoDataManager.getDicFromNewsKeywordDict()
    logInfo("start add keywordDic data to dictionary")
    dics.foreach { dic =>
      val word = if (dic.get("word") != null) dic.get("word").toString.trim else null
      if(word!=null){
        UserDefineLibrary.insertWord(word, speechPost, freq)
        logInfo(s" keywordDic ${word} have loaded to dictionary")
      }
    }
    logInfo(s"finished add keywordDic data to dictionary,total number:${dics.size()}")
  }

  //get all data from graph
  def getAllFromGraphsToDictionary(graphNodeDataUrl: String): Unit = {
    val allNodeJsonObj = requestHttpByURL(graphNodeDataUrl)
    if (allNodeJsonObj != null) {
      val nodes = allNodeJsonObj.getJSONArray("message")
      if (nodes != null && nodes.size() > 0) {
        logInfo("start add graph data to dictionary")
        nodes.foreach { n =>
          UserDefineLibrary.insertWord(n.toString, speechPost, freq)
          logInfo(s" graph ${n.toString} have loaded to dictionary")
        }
        logInfo(s"finished add graph data to dictionary,total number:${nodes.size()}")
      }
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


  def main(args: Array[String]) {
    getAllFromGraphsToDictionary(graphNodeDataUrl)
  }
}
