package search.solr.client.control

import java.io.{FileInputStream, IOException}
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
    var kvN = "test1"
    list.add("a")
    list.add("abc")
    keywords.add(new IndexObjEntity(kvN, list))

    kvN = "test22"
    list = new java.util.ArrayList[String]()
    list.add("aabc")
    keywords.add(new IndexObjEntity(kvN, list))

    kvN = "test34"
    list = new java.util.ArrayList[String]()
    list.add("aabc chain")
    keywords.add(new IndexObjEntity(kvN, list))

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
    result.foreach { obj =>
      var rvkw: java.util.Collection[String] = null
      val obj1 = obj.asInstanceOf[JSONObject]
      keyWord = obj1.getString("keyword")
      if (obj1.containsKey("relevant_kws")) {
        val list = obj1.getJSONArray("relevant_kws").toList
        val sttList = list.map(_.toString)
        rvkw = sttList
      }
      keywords.add(new IndexObjEntity(keyWord, rvkw))
    }
    keywords
  }

  def testIndexByKeywordsWithRw {
    //val url: String = "http://localhost:8999/es/index/rws"
    val url: String = "http://54.222.222.172:8999/es/index/rws"
    val keywords = getIndexDataFromSerObj
    //val keywords = getIndexDataBySet
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
