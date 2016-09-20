package search.common.http

import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpEntityEnclosingRequestBase, HttpUriRequest, CloseableHttpResponse}
import org.apache.http.entity.StringEntity
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.HttpContext
import org.apache.http.concurrent.FutureCallback
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.utils.HttpClientUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.HttpHost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.HttpEntity
import org.apache.http.util.EntityUtils
import org.apache.http.HttpResponse
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import search.common.entity.enumeration.HttpRequestMethodType
import search.solr.client.index.manager.IndexManagerRunner
import search.common.util.Logging
import scala.collection.JavaConversions._

import scala.collection.mutable

class HttpClientUtil private extends Logging {

  // use Proxy
  def execute(request: HttpUriRequest, proxy: HttpHost, callback: (HttpContext, CloseableHttpResponse) => Unit): Unit = {
    val context: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext);
    val reqCfg = RequestConfig.custom().setProxy(proxy).build();
    context.setRequestConfig(reqCfg)
    this.execute(request, context, callback)
  }


  def execute(request: HttpUriRequest, context: HttpContext, callback: (HttpContext, CloseableHttpResponse) => Unit): Unit = {
    var httpResp: CloseableHttpResponse = null
    try {
      if (context == null) {
        httpResp = HttpClientUtil.httpClient.execute(request)
      } else {
        httpResp = HttpClientUtil.httpClient.execute(request, context)
      }
      callback(context, httpResp)
    } catch {
      case t: Throwable => logError("Http Error", t)
    }
  }

  def execute(request: HttpUriRequest, callback: (HttpContext, CloseableHttpResponse) => Unit): Unit = {
    this.execute(request, null.asInstanceOf[HttpContext], callback)
  }

  def executeSyn(request: HttpUriRequest, context: HttpContext): CloseableHttpResponse = {
    var httpResp: CloseableHttpResponse = null
    try {
      if (context == null) {
        httpResp = HttpClientUtil.httpClient.execute(request)
      } else {
        httpResp = HttpClientUtil.httpClient.execute(request, context)
      }
    } catch {
      case t: Throwable => logError("Http Error", t)
    }
    httpResp
  }

  def executeSyn(request: HttpUriRequest): CloseableHttpResponse = {
    this.executeSyn(request, null.asInstanceOf[HttpContext])
  }


}

object HttpClientUtil {

  private val httpClient: CloseableHttpClient = HttpClients.createDefault();

  private val instance: HttpClientUtil = new HttpClientUtil

  def getInstance(): HttpClientUtil = {
    return instance
  }

  def closeHttpClient = HttpClientUtils.closeQuietly(httpClient);


  def requestHttpSyn(url: String, requestType: String, obj: Object, headers: java.util.Map[String, String]): CloseableHttpResponse = {
    var rType = HttpRequestMethodType.GET
    requestType match {
      case "post" => rType = HttpRequestMethodType.POST
      case _ => null
    }
    if (obj.isInstanceOf[java.util.Map[String, Object]])
      requestHttpSyn(url, rType, obj.asInstanceOf[java.util.Map[String, Object]], headers, null)
    else requestHttpSyn(url, rType, obj, headers, null)
  }

  def requestHttpSyn(url: String, requestType: HttpRequestMethodType.Type, paremeters: java.util.Map[String, Object], headers: java.util.Map[String, String], contexts: java.util.Map[String, String]): CloseableHttpResponse = {
    requestHttpSyn(url, requestType, paremeters, null, headers, contexts)
  }

  def requestHttpSyn(url: String, requestType: HttpRequestMethodType.Type, obj: Object, headers: java.util.Map[String, String], contexts: java.util.Map[String, String]): CloseableHttpResponse = {
    requestHttpSyn(url, requestType, null, obj, headers, contexts)
  }

  def requestHttpSyn(url: String, requestType: HttpRequestMethodType.Type, paremeters: java.util.Map[String, Object], obj: Object, headers: java.util.Map[String, String], contexts: java.util.Map[String, String]): CloseableHttpResponse = {
    val request = HttpRequstUtil.createRequest(requestType, url)
    var isJson = false
    if (headers != null && !headers.isEmpty) {
      headers.foreach { header =>
        val key = header._1
        val value = header._2
        if (key.toLowerCase.trim.equalsIgnoreCase("content-type")) {
          if (value.toLowerCase.trim.equalsIgnoreCase("application/json")) isJson = true
        }
        request.addHeader(key, value)
      }
    }
    val context: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext)
    if (contexts != null && !contexts.isEmpty) {
      contexts.foreach { attr =>
        context.setAttribute(attr._1, attr._2)
      }
    }

    var formparams: java.util.List[BasicNameValuePair] = null
    if (!isJson) {
      if (paremeters != null && !paremeters.isEmpty) {
        formparams = new java.util.ArrayList[BasicNameValuePair]()
        paremeters.foreach { p =>
          formparams.add(new BasicNameValuePair(p._1, p._2.toString))
        }
      }
    }

    var entity: StringEntity = null
    val mapper = new ObjectMapper()
    var jStirng: String = null
    if (paremeters != null && !paremeters.isEmpty) {
      jStirng = mapper.writeValueAsString(paremeters)
    } else if (obj != null) {
      jStirng = mapper.writeValueAsString(obj)
    }
    if(jStirng!=null)
    println("string json:" + jStirng)

    if (isJson) {
      if (jStirng != null)
        entity = new StringEntity(jStirng, "utf-8")
    } else {
      if (jStirng != null)
        entity = new StringEntity(jStirng, "utf-8")
      if (formparams != null && !formparams.isEmpty)
        entity = new UrlEncodedFormEntity(formparams, "utf-8")
    }
    requestType match {
      case HttpRequestMethodType.POST =>
        request.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
      case _ =>
    }
    HttpClientUtil.getInstance().executeSyn(request, context)

  }


  def requestHttp(url: String, requestType: String, paremeters: java.util.Map[String, Object], headers: java.util.Map[String, String], callback: (HttpContext, HttpResponse) => Unit): Unit = {
    var rType = HttpRequestMethodType.GET
    requestType match {
      case "post" => rType = HttpRequestMethodType.POST
      case _ => null
    }
    requestHttp(url, rType, paremeters, headers, null, callback)
  }

  def requestHttp(url: String, requestType: HttpRequestMethodType.Type, paremeters: java.util.Map[String, Object], headers: java.util.Map[String, String], contexts: java.util.Map[String, String], callback: (HttpContext, HttpResponse) => Unit): Unit = {
    val request = HttpRequstUtil.createRequest(requestType, url)
    var isJson = false
    if (headers != null && !headers.isEmpty) {
      headers.foreach { header =>
        val key = header._1
        val value = header._2
        if (key.toLowerCase.trim.equalsIgnoreCase("content-type")) {
          if (value.toLowerCase.trim.equalsIgnoreCase("application/json")) isJson = true
        }
        request.addHeader(key, value)
      }
    }
    val context: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext)
    if (contexts != null && !contexts.isEmpty) {

      contexts.foreach { attr =>
        context.setAttribute(attr._1, attr._2)
      }
    }

    if (paremeters != null && !paremeters.isEmpty) {
      val formparams: java.util.List[BasicNameValuePair] = new java.util.ArrayList[BasicNameValuePair]()
      paremeters.foreach { p =>
        formparams.add(new BasicNameValuePair(p._1, p._2.toString))
      }

      var entity: StringEntity = null
      if (isJson) {
        val mapper = new ObjectMapper()
        val jStirng = mapper.writeValueAsString(paremeters)
        println("string json:" + jStirng)
        entity = new StringEntity(jStirng, "utf-8");
      } else entity = new UrlEncodedFormEntity(formparams, "utf-8");

      requestType match {
        case HttpRequestMethodType.POST =>
          request.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
        case _ =>
      }

    }
    HttpClientUtil.getInstance().execute(request, context, callback)
  }
}

object TestHttpClientUtil {

  def main(args: Array[String]): Unit = {
    //testHttp
    //testHttpRecommend
    testHttpRecommend1
  }

  def testHttpRecommend1: Unit = {
    val url = "http://218.244.132.8:8088/recommend/sku"
    //val url = "http://192.168.51.161:8088/recommend/sku"
    val parametersMap = new java.util.HashMap[String, java.lang.Object]()
    // parametersMap.put("userId", "58438")
    //parametersMap.put("catagoryId", "null")
    //parametersMap.put("brandId", "1421")
    // parametersMap.put("docId", "614547")
    parametersMap.put("docId", "MCA839")
    parametersMap.put("number", Integer.valueOf(70))
    val headers = new java.util.HashMap[String, String]()
    headers("Content-Type") = "application/json"
    HttpClientUtil.requestHttp(url, HttpRequestMethodType.POST, parametersMap, headers, null, callback)
    def callback(context: HttpContext, httpResp: HttpResponse) = {
      try {
        println(Thread.currentThread().getName)
        println(httpResp)
        val sResponse = EntityUtils.toString(httpResp.getEntity)
        println(sResponse)
      } finally {
        HttpClientUtils.closeQuietly(httpResp)
      }
    }
  }

  def testHttpRecommend: Unit = {
    val url = "http://218.244.132.8:8088/recommend/sku"
    val parametersMap = new java.util.HashMap[String, java.lang.Object]()
    parametersMap.put("userId", "null")
    parametersMap.put("catagoryId", "null")
    parametersMap.put("brandId", "1421")
    parametersMap.put("number", Integer.valueOf(30))
    val headers = new java.util.HashMap[String, String]()
    headers("Content-Type") = "application/json"
    HttpClientUtil.requestHttp(url, HttpRequestMethodType.POST, parametersMap, headers, null, callback)
    def callback(context: HttpContext, httpResp: HttpResponse) = {
      try {
        println(Thread.currentThread().getName)
        println(httpResp)
        val sResponse = EntityUtils.toString(httpResp.getEntity)
        println(sResponse)
      } finally {
        HttpClientUtils.closeQuietly(httpResp)
      }
    }
  }

  def testHttp = {
    var start: Long = -1
    var end: Long = -1
    val request = HttpRequstUtil.createRequest(HttpRequestMethodType.GET, "http://121.40.241.26/recommend/0/5")
    def callback(context: HttpContext, httpResp: HttpResponse) = {
      try {
        println(Thread.currentThread().getName)
        println(httpResp)
        val sResponse = EntityUtils.toString(httpResp.getEntity)
        println()
        end = System.currentTimeMillis()
        printf("start %15d, end %15d, cost %15d \n", start, end, end - start)
        start = System.currentTimeMillis()
        val om = new ObjectMapper()
        val obj = om.readTree(sResponse)
        val rootJsonNode = obj.asInstanceOf[JsonNode]
        if (rootJsonNode.isArray) {
          val node = rootJsonNode.iterator()
          while (node.hasNext) {
            val obj = node.next()
            println(obj)
            val jnode = obj.getFields
            while (jnode.hasNext) {
              val it = jnode.next()
              val key = it.getKey
              val value = it.getValue
              println("key:" + key + "\n" + "value:" + value)
            }

          }

        }
        println(obj)
      } finally {
        HttpClientUtils.closeQuietly(httpResp)
      }
    }
    start = System.currentTimeMillis()

    //for(i <- 1 to 10){
    HttpClientUtil.getInstance().execute(request, callback)
    // }
    HttpClientUtil.closeHttpClient
  }

}