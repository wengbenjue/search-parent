package search.solr.client.control

import java.io.{FileInputStream, IOException}
import java.net.URLEncoder
import java.util

import com.alibaba.fastjson.{JSONObject, JSONArray, JSON}
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import search.common.entity.bizesinterface.{QueryEntityWithCnt, IndexObjEntity}
import search.common.entity.searchinterface.parameter.IndexKeywordsParameter
import search.common.http.HttpClientUtil
import search.common.serializer.JavaSerializer
import search.es.client.biz.BizeEsInterface._
import search.solr.client.SolrClientConf
import scala.collection.JavaConversions._

import scala.io.Source

/**
  * Created by soledede.weng on 2016/8/5.
  */
object EsSearchHttpClientTest {
  val SEPARATOR = "->"

  def main(args: Array[String]) {
    //indexByKeywords
    testIndexByKeywordsWithRw
    //getIndexDataFromSerObj()
    //testSearchFluid
    //testShowStateRedisCache()

  }

  def testShowStateRedisCache() = {
    val url = "http://localhost:8999/es/search/state/?keyword="
    val list = List("金融科技", "联创电子", "金融科技d", "金融科技dw", "金融科技1", "联创电子2", "金融科技d3", "金融科技d4w",
      "利亚德", "利亚德1", "利亚德2", "利亚德3", "利亚德4", "利亚德15", "利亚德22", "利亚德33",
      "三环集团", "三环集团1", "三环集团2", "三环集团3", "三环集团4", "三环集团15", "三环集团22", "三环集团33")

    for(j <- 0 until 1000){
      list.foreach { q =>
        for (i <- 0 until 100) {
          val thread = new Thread(new RequestTestRedis(q, url))
          thread.start()
        }
      }

    }



  }

  class RequestTestRedis(query: String, url: String) extends Runnable {
    override def run(): Unit = {
      requestHttp(query, url, null)
      println(s"request $query")
    }
  }

  def testSearchFluid() = {
    val url = "http://localhost:8999/es/search/state/?keyword="
    for (i <- 0 until 10000) {
      requestHttp("金融科技", url, null)
      println(s"request $i")
    }

  }

  def requestHttp(query: String, httpUrl: String, showLevel: Integer, reqType: String = "get"): AnyRef = {
    reqType match {
      case "get" =>
        var url: String = s"${httpUrl}${URLEncoder.encode(query, "UTF-8")}"
        if (showLevel != null) url = url + s"&l=$showLevel"
        var httpResp: CloseableHttpResponse = null
        try {
          httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
          val entity: HttpEntity = httpResp.getEntity
          val sResponse: String = EntityUtils.toString(entity)
          //println(sResponse)
          //JSON.parseObject(sResponse)
          null
        }
        catch {
          case e: IOException => {
            null
          }
        } finally {
          if (httpResp != null)
            HttpClientUtils.closeQuietly(httpResp)
        }
      case _ =>
        null
    }
  }

  def getIndexDataFromFile(): java.util.List[IndexObjEntity] = {
    val keywords: java.util.List[IndexObjEntity] = new java.util.ArrayList[IndexObjEntity]
    val filePath = "D:\\all_nodes_new.txt"
    for (line <- Source.fromFile(filePath).getLines if (!line.trim.equalsIgnoreCase(""))) {
      val keyWordsLine = line.trim
      val kvs = keyWordsLine.split(SEPARATOR)
      var keyword: String = null
      var rvw: String = null
      if (kvs.length > 1) {
        keyword = kvs(0).trim
        rvw = kvs(1).trim
      } else {
        keyword = kvs(0).trim
      }
      println(s"add${keyword} -> ${rvw} success")

      keywords.add(new IndexObjEntity(keyword, rvw))
    }
    keywords
  }

  def getIndexDataBySet(): java.util.List[IndexObjEntity] = {
    val keywords: java.util.List[IndexObjEntity] = new java.util.ArrayList[IndexObjEntity]
    var list: java.util.List[String] = new java.util.ArrayList[String]()
    var kvN = "巴安水务"
    keywords.add(new IndexObjEntity(kvN))

    list.add("a")
    list.add("abc")
    //keywords.add(new IndexObjEntity(kvN, list))

    /*kvN = "test22"
    list = new java.util.ArrayList[String]()
    list.add("aabc")
    keywords.add(new IndexObjEntity(kvN, list))

    kvN = "test34"
    list = new java.util.ArrayList[String]()
    list.add("aabc chain")
    keywords.add(new IndexObjEntity(kvN, list))*/

    keywords
  }

  def getIndexDataFromSerObj(): java.util.List[IndexObjEntity] = {
    val ser = JavaSerializer(new SolrClientConf()).newInstance()
    val keywords: java.util.List[IndexObjEntity] = new java.util.ArrayList[IndexObjEntity]
    val fIput = new FileInputStream("D:/java/es_index");
    val inputStream = ser.deserializeStream(fIput)
    val obj = inputStream.readObject[QueryEntityWithCnt]()
    inputStream.close()
    val result = JSON.toJSON(obj.getResult).asInstanceOf[JSONArray]
    var keyWord: String = null
    var cnt = 0
    result.foreach { obj =>
      cnt += 1
      var rvkw: java.util.Collection[String] = null
      val obj1 = obj.asInstanceOf[JSONObject]
      keyWord = obj1.getString("keyword")
      if (obj1.containsKey("relevant_kws")) {
        val list = obj1.getJSONArray("relevant_kws").toList
        val sttList = list.map(_.toString)
        rvkw = sttList
      }
      //if (cnt < 100)
      keywords.add(new IndexObjEntity(keyWord, rvkw))

    }
    keywords
  }

  def testIndexByKeywordsWithRw {
    //val url: String = "http://localhost:8999/es/index/rws"
    val url: String = "http://54.222.222.172:8999/es/index/rws"
    val keywords = getIndexDataFromSerObj
   // val keywords = getIndexDataBySet
    val headers: java.util.Map[String, String] = new java.util.HashMap[String, String]
    headers.put("Content-Type", "application/json")
    val httpResp: CloseableHttpResponse = HttpClientUtil.requestHttpSyn(url, "post", keywords, headers)
    try {
      val entity: HttpEntity = httpResp.getEntity
      val sResponse: String = EntityUtils.toString(entity)
      System.out.println(sResponse)
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    } finally {
      HttpClientUtils.closeQuietly(httpResp)
    }
  }

  def indexByKeywords() = {
    //String url = "http://localhost:8999/es/index/keywords";
    val url: String = "http://54.222.222.172:8999/es/index/keywords"
    val keywords: java.util.List[String] = new java.util.ArrayList[String]
    val filePath = "D:\\all_nodes_new_test.txt"
    for (line <- Source.fromFile(filePath).getLines if (!line.trim.equalsIgnoreCase(""))) {
      val keyWord = line.trim
      println("add" + keyWord + "success")
      keywords.add(keyWord)
    }

    val obj: IndexKeywordsParameter = new IndexKeywordsParameter
    obj.setKeywords(keywords)
    //obj.setOriginQuery("无人机对话");
    val headers: java.util.Map[String, String] = new java.util.HashMap[String, String]
    headers.put("Content-Type", "application/json")
    val httpResp: CloseableHttpResponse = HttpClientUtil.requestHttpSyn(url, "post", obj, headers)
    try {
      val entity: HttpEntity = httpResp.getEntity
      val sResponse: String = EntityUtils.toString(entity)
      System.out.println(sResponse)
    }
    catch {
      case e: IOException => {
        e.printStackTrace
      }
    } finally {
      HttpClientUtils.closeQuietly(httpResp)
    }

  }

}
