package search.solr.client.control

import java.io.IOException
import java.util
import java.util.List

import com.mongodb.BasicDBObject
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.util.EntityUtils
import search.common.entity.searchinterface.parameter.IndexKeywordsParameter
import search.common.http.HttpClientUtil

import scala.io.Source

/**
  * Created by soledede.weng on 2016/8/5.
  */
object EsSearchHttpClientTest {

  def main(args: Array[String]) {
    indexByKeywords
  }

  def indexByKeywords() = {
    //String url = "http://localhost:8999/es/index/keywords";
    val url: String = "http://54.222.222.172:8999/es/index/keywords"
    val keywords: java.util.List[String] = new java.util.ArrayList[String]
    val filePath = "D:\\all_nodes.txt"
    for (line <- Source.fromFile(filePath).getLines if (!line.trim.equalsIgnoreCase(""))) {
      val keyWord = line.trim
      println("add" + keyWord + "success")
      keywords.add(keyWord)
    }

    val obj: IndexKeywordsParameter = new IndexKeywordsParameter
    obj.setKeywords(keywords)
    //obj.setOriginQuery("无人机对话");
    val headers: util.Map[String, String] = new util.HashMap[String, String]
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
