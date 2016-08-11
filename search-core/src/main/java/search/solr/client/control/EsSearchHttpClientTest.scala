package search.solr.client.control

import java.io.IOException
import java.util

import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import search.common.entity.bizesinterface.IndexObjEntity
import search.common.entity.searchinterface.parameter.IndexKeywordsParameter
import search.common.http.HttpClientUtil

import scala.io.Source

/**
  * Created by soledede.weng on 2016/8/5.
  */
object EsSearchHttpClientTest {
  val SEPARATOR = "->"

  def main(args: Array[String]) {
    //indexByKeywords
    testIndexByKeywordsWithRw
  }

  def testIndexByKeywordsWithRw {
    val url: String = "http://localhost:8999/es/index/rws"
    //val url: String = "http://54.222.222.172:8999/es/index/rws"
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

    //  keywords.add(new IndexObjEntity(keyword, rvw))
    }

    var list: java.util.List[String] = new java.util.ArrayList[String]()
    list.add("a")
    list.add("b")
    //list = new util.ArrayList[String]()
    val kvN =  "test"
    keywords.add(new IndexObjEntity(kvN, list))
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
