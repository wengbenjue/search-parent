package search.solr.client.index.manager.impl

import java.io.PrintWriter
import java.util


import com.alibaba.fastjson.JSON
import org.apache.http.HttpResponse
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.{HttpRequestBase, HttpEntityEnclosingRequestBase}
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.{BasicHttpContext, HttpContext}
import org.apache.http.util.EntityUtils
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import org.codehaus.jackson.map.ObjectMapper._
import search.common.listener.trace.{AddIndex, ManagerListenerWaiter, IndexTaskTraceListener}
import search.solr.client.consume.Consumer._
import search.solr.client.{ObjectJson, SolrClientConf, SolrClient}
import search.common.config.{SolrConfiguration, Configuration}
import search.common.entity.enumeration.HttpRequestMethodType
import search.common.http.{HttpClientUtil, HttpRequstUtil}
import search.solr.client.index.manager.{IndexManagerRunner, IndexManager}
import search.solr.client.product.Producter
import search.common.util.{Util, Logging}
import scala.collection.mutable
import scala.util.control.Breaks._
import scala.collection.JavaConversions._


/**
  * Created by soledede on 2016/2/15.
  */
class DefaultIndexManager private extends IndexManager with Logging with SolrConfiguration {
  val C_OR_UPDATE_XML = "_c_u.xml"
  val DELETE_XML = "_d.xml"


  // "http://192.168.51.54:8088/mergecloud"
  val MERGECLOUD_URL: String = mergesCloudsUrl
  val SCREEN_URL: String = screenCloudsUrl

  val needSenseCharcter = Array("|") //if you need use new sense charcter  ,you need add it to array

  var arrayObj: Array[String] = null

  if (multiValuedString != null && !multiValuedString.trim.equalsIgnoreCase("")) {
    arrayObj = multiValuedString.split("&")
  }

  var solrClient: SolrClient = _

  if (useSolr) init

  def init = {
    solrClient = SolrClient(new SolrClientConf())
  }


  logInfo(s"***************************************current threads number:${DefaultIndexManager.currentThreadsNum}***************************************")


  override def requestData(message: String): AnyRef = {
    var obj: AnyRef = null //return jsonNode
    if (message != null && !message.trim.equalsIgnoreCase("")) {
      logInfo(s"recieve$message")
      val msgArray = message.split(Producter.separator)
      if (msgArray.length <= 2) logError("input format not right,should 'mergescloud-1423454543243-456  or mergescloud-23545421334-34534534-5643 or mergescloud-delete-324234-4343423'")
      else if (msgArray(1).trim.equals(Producter.DELETE)) {
        //command delete index
        logInfo(s"recieveDeleteMessage-$message")
        indexOrDelteData(msgArray, msgArray(0))
        obj = msgArray

      } else if (msgArray(1).trim.equals(Producter.DELETE_BY_QUERY)) {
        //command delete index by query
        logInfo(s"recieveDeleteMessage-$message")
        val query = msgArray(2)
        if (query != null && !query.equalsIgnoreCase(""))
          deleteByQuery(query, msgArray(0))
        //obj = msgArray
      } else {
        //add or update index
        if (msgArray.length == 3) {
          //first represent collection ,mergescloud-234343211-34
          //have minimum update time
          val collection = msgArray(0)
          val minUpdateTime = msgArray(1)
          val totalNum = msgArray(2)
          logInfo(s"recieveMessage-minUpdateTime:$minUpdateTime-totalNum:$totalNum")

          try {
            requestUrl(collection, totalNum, minUpdateTime, null)
          } catch {
            case e: Exception =>
              logError("request faield!", e)
              logInfo(s"faieldMessage-minUpdateTime:$minUpdateTime-totalNum:$totalNum")
          }
        } else if (msgArray.length == 4) {
          //first represent collection ,mergescloud-2343433212-234343211-34
          //it's time quantum
          val collection = msgArray(0).trim
          val startUpdateTime = msgArray(1).trim
          val endUpdataTime = msgArray(2).trim
          val totalNum = msgArray(3).trim
          logInfo(s"sendMessage-startTime:$startUpdateTime-endTime:$endUpdataTime-totalNum:$totalNum")
          try {
            requestUrl(collection, totalNum, startUpdateTime, endUpdataTime)
          } catch {
            case e: Exception =>
              logError("request faield!", e)
              logInfo(s"faieldMessage-startTime:$startUpdateTime-endTime:$endUpdataTime-totalNum:$totalNum")
          }
        }
      }
    }

    /**
      *
      * @param totalNum
      * @param startUpdateTime
      * @param endUpdataTime
      */
    def requestUrl(collection: String, totalNum: String, startUpdateTime: String, endUpdataTime: String): Unit = {
      var url = MERGECLOUD_URL

      if (collection.contains("screencloud")) url = SCREEN_URL
      /*  collection match {
          case "screencloud" => url = SCREEN_URL
          case "mergescloud" => url = MERGECLOUD_URL
          case _ =>
        }*/


      var more = 0
      if (totalNum.toInt % pageSize != 0) more = totalNum.toInt % pageSize
      var onePage = 0
      if (more > 0) onePage = 1
      val requestCounts = (totalNum.toInt / pageSize) + onePage

      logInfo(s"start loop,total counts$requestCounts")
      for (i <- 0 to requestCounts - 1) {
        val paremeters = new mutable.HashMap[String, String]()
        if (startUpdateTime != null && !startUpdateTime.trim.equalsIgnoreCase("null") && !startUpdateTime.trim.equalsIgnoreCase("") && !startUpdateTime.trim.equalsIgnoreCase("0"))
          paremeters("startUpdateTime") = startUpdateTime
        if (endUpdataTime != null && !startUpdateTime.trim.equalsIgnoreCase("null") && !endUpdataTime.trim.equalsIgnoreCase("") && !endUpdataTime.trim.equalsIgnoreCase("0"))
          paremeters("endUpdateTime") = endUpdataTime
        paremeters("start") = (i * pageSize).toString
        if (i == (requestCounts - 1)) {
          //the last pages
          if (more > 0) paremeters("rows") = more.toString
          else paremeters("rows") = pageSize.toString
        } else paremeters("rows") = pageSize.toString
        logInfo(s"request url${i + 1} $url,\t parameters:start=${paremeters("start")},rows=${paremeters("rows")}")

        requestHttp(collection, url, HttpRequestMethodType.POST, paremeters, callback)
        if (i > 0 && ((i + 1 % threadsWaitNum) == 0)) {
          logInfo(s"thread sleeping...\n current loop i=$i,threadsWaitNum=$threadsWaitNum,threadsSleepTime:$threadsSleepTime")
          this.synchronized {
            Thread.sleep(threadsSleepTime)
          }
          logInfo("thread wakeuped")
        }
      }

    }
    //  obj
    null
  }


  override def execute(request: HttpRequestBase, context: HttpClientContext, callback: (HttpContext, HttpResponse) => Unit): Unit = {
    HttpClientUtil.getInstance().execute(request, context, callback)
  }


  //have get data
  def callback(context: HttpContext, httpResp: HttpResponse) = {
    var obj: AnyRef = null
    val entTime = System.currentTimeMillis()
    val startTime = context.getAttribute(DefaultIndexManager.requestStartTime_key).toString.toLong
    val params = context.getAttribute(DefaultIndexManager.params_key)
    logInfo(s"request end time(ms):$entTime,\t all cost:${entTime - startTime},\trequest count(pagesize):$pageSize,\tcurrentThreadName:${Thread.currentThread().getName}\tparameters:$params")

    val collection = context.getAttribute("collection").toString
    val responseData = EntityUtils.toString(httpResp.getEntity)

    DefaultIndexManager.indexProcessThreadPool.execute(new IndexManageProcessrRunner(collection, responseData))
    //startGeneralXmlAndIndex(collection, responseData)  //change to use thread pool
  }

  def indexOrDelteData(obj: AnyRef, collection: String): Unit = {
    val xmlBool = geneXml(obj, collection)
    if (xmlBool != null) {
      indexesData(collection, xmlBool)
    }
  }


  /* def indexOrDelteData(obj: AnyRef, collection: String): Unit = {
     val xmlBool = geneXml(obj, collection)
     if (xmlBool != null) {
       indexesData(collection, xmlBool)
       if (xmlBool.isInstanceOf[util.ArrayList[String]]) {
       deleteIndexData(collection, xmlBool)
     }
     else {
       indexData(collection, xmlBool)
     }
     }
   }*/

  /**
    * index data
    *
    * @param collection
    * @param xmlBool
    */
  def indexesData(collection: java.lang.String, xmlBool: AnyRef): Unit = {
    try {
      val indexDatas = xmlBool.asInstanceOf[java.util.ArrayList[java.util.Map[java.lang.String, Object]]]
      indexDatas(0).asInstanceOf[java.util.Map[java.lang.String, Object]]
      if (indexData(indexDatas, collection)) logInfo(" index success!")
      else {
        logError("index faield!Ids:")
        indexDatas.foreach { doc =>
          logInfo(s"index faield id:\t${doc.get("id")}")
        }
      }
    } catch {
      case castEx: java.lang.ClassCastException =>
        deleteIndexData(collection, xmlBool)
      // case e: Exception => logError("index faield", e)
    }

  }

  def requestHttp(collection: String, url: String, requestType: HttpRequestMethodType.Type, paremeters: mutable.Map[String, String], callback: (HttpContext, HttpResponse) => Unit): Unit = {
    val request = HttpRequstUtil.createRequest(requestType, url)
    val context: HttpClientContext = HttpClientContext.adapt(new BasicHttpContext)
    context.setAttribute(DefaultIndexManager.collection_key, collection.trim)
    if (paremeters != null && !paremeters.isEmpty) {
      val formparams: java.util.List[BasicNameValuePair] = new java.util.ArrayList[BasicNameValuePair]()
      val pString = new StringBuilder
      paremeters.foreach { p =>
        pString.append(s"key:${p._1},value:${p._2}\n")
        formparams.add(new BasicNameValuePair(p._1, p._2))
      }
      context.setAttribute(DefaultIndexManager.params_key, pString.toString())
      val entity: UrlEncodedFormEntity = new UrlEncodedFormEntity(formparams, "utf-8");
      request.asInstanceOf[HttpEntityEnclosingRequestBase].setEntity(entity)
    }
    DefaultIndexManager.consumerManageThreadPool.execute(new IndexManagerRunner(this, request, context, callback))
    //HttpClientUtil.getInstance().execute(request, context, callback)
  }


  class IndexManageProcessrRunner(collection: String, responseData: String) extends Runnable {
    override def run(): Unit = {
      startGeneralXmlAndIndex(collection, responseData)

    }
  }


  def startGeneralXmlAndIndex(collection: String, responseData: String): Unit = {
    if (responseData != null && !responseData.equalsIgnoreCase("")) {
      val om = new ObjectMapper()
      try {
        val obj = om.readTree(responseData)
        if (obj == null) logInfo(s"response null,size:0")
        else {
          if (obj.isInstanceOf[JsonNode]) {
            //generate add index xml
            val dataJsonNode = obj.asInstanceOf[JsonNode]
            logInfo(s"response size:${dataJsonNode.get("data").size()}")
          }
        }
        indexOrDelteData(obj, collection)
      } catch {
        case ex: Exception =>
          logError(s"json pase faield:data:${responseData}", ex)
      }
    }
  }


  //post to event queue
  private def postKV(key: String, value: String) = {
    if (key != null && value != null && (key.equalsIgnoreCase("sku") || key.equalsIgnoreCase("delete")))
      DefaultIndexManager.bus.post(AddIndex(value))
  }

  override def geneXml(data: AnyRef, collection: String): AnyRef = {
    var listMap: java.util.List[java.util.Map[java.lang.String, Object]] = new java.util.ArrayList[java.util.Map[java.lang.String, Object]]()
    var delList: java.util.List[java.lang.String] = null
    var writeToFileCnt = 0 //for loginfo
    var fileNamePreffix = C_OR_UPDATE_XML

    if (data != null) {
      val xml = new StringBuilder
      if (data.isInstanceOf[JsonNode]) {
        //generate add index xml
        val dataJsonNode = data.asInstanceOf[JsonNode]

        if (!dataJsonNode.isNull && dataJsonNode.size() > 0) {

          val rootJsonNode = dataJsonNode.get("data")
          val rootJsonNodeSize = rootJsonNode.size()
          if (!rootJsonNode.isNull && rootJsonNodeSize > 0) {
            logInfo(s"actual return result size:$rootJsonNodeSize")

            xml.append("<?xml version='1.0' encoding='UTF-8'?>")
            xml.append("\n")

            xml.append("<add>")
            xml.append("\n")

            if (rootJsonNode.isArray) {
              writeToFileCnt = rootJsonNode.size()
              val arrayIt = rootJsonNode.iterator()
              while (arrayIt.hasNext) {
                val objNode = arrayIt.next()
                geneAddXml(objNode, xml, listMap)
              }
            } else {
              writeToFileCnt = 1
              geneAddXml(rootJsonNode, xml, listMap) //single document
            }


            xml.append("</add>")
          }
        }
      } else if (data.isInstanceOf[Array[String]]) {
        //generate delete index xml
        //delete xml
        fileNamePreffix = DELETE_XML
        val deleteArray = data.asInstanceOf[Array[String]]
        // deleteArray.
        if (deleteArray.length > 2) {
          writeToFileCnt = deleteArray.length - 2
          xml.append("<?xml version='1.0' encoding='UTF-8'?>")
          xml.append("\n")
          xml.append("<delete>")
          xml.append("\n")
          delList = new java.util.ArrayList[java.lang.String]()
          for (i <- 2 to deleteArray.length - 1) {
            //index 0 reoresent command DELETE
            xml.append("<id>")
            postKV("delete", deleteArray(i).trim)
            xml.append(deleteArray(i).trim)
            xml.append("</id>")
            xml.append("\n")
            delList.add(deleteArray(i).trim)
          }
          xml.append("</delete>")
          // delArray = deleteArray.drop(1)
        }
      }
      if (!xml.isEmpty) {
        // this.synchronized {
        val fileName = collection.trim + "_" + System.currentTimeMillis() + fileNamePreffix
        var filePath = filedirMergeCloud + fileName
        if (collection.contains("screencloud")) filePath = filedirScreenCloud + fileName
        writeToDisk(xml.toString(), filePath)
        logInfo(s"write file $filePath success,Total ${writeToFileCnt} documents！")
        //}
      }

    }
    if (listMap.size() > 0) listMap
    else if (delList != null && delList.size() > 0) delList
    else null
  }

  def geneAddXml(obj: JsonNode, xml: StringBuilder, listMap: java.util.List[java.util.Map[java.lang.String, Object]]) = {
    xml.append("<doc>")
    xml.append("\n")

    val objMap: java.util.Map[java.lang.String, AnyRef] = new java.util.HashMap[java.lang.String, AnyRef]() //for add index

    val fields = obj.getFields //get all fields
    while (fields.hasNext()) {
      val it = fields.next()
      val key = it.getKey.trim
      var value = it.getValue.asText().trim



      if (key != null && value != null && !value.trim.equalsIgnoreCase("") && !value.trim.equalsIgnoreCase("\"\"") && !value.trim.equalsIgnoreCase("''") && !value.trim.equalsIgnoreCase("null")) {
        var isMultiValued = false
        if (arrayObj != null && arrayObj.length > 0) {
          //arrayObj represent have mutivalued field config
          breakable {
            for (i <- 0 to arrayObj.length - 1) {
              if (arrayObj(i).contains(key.trim)) {
                val multiValuedKeySeparator = arrayObj(i).split("=>")
                val separator = multiValuedKeySeparator(1).trim
                var needSense = false
                breakable {
                  //judge whether need sense
                  for (i <- 0 to needSenseCharcter.length - 1) {
                    if (needSenseCharcter(i).trim.equalsIgnoreCase(separator)) {
                      needSense = true
                      break
                    }
                  }
                }
                var vals: Array[String] = null
                if (needSense)
                  vals = value.split(s"\\$separator") //multiValued Array
                else vals = value.split(s"$separator") //multiValued Array
                val listMutivalued = new util.ArrayList[java.lang.String]()
                vals.foreach { f =>
                  if (!f.equalsIgnoreCase("") && !f.equalsIgnoreCase("null") && !f.equalsIgnoreCase("\"\"")) {
                    var fV: String = f.trim.replaceAll("^\"([\\S|\\s]+)\"$", "$1")
                    fV = fV.replaceAll("^\"([\\S|\\s]+)", "$1")
                    fV = fV.replaceAll("([\\S|\\s]+)\"$", "$1")
                    fV = fV.trim
                    if (!fV.equalsIgnoreCase("") && !fV.equalsIgnoreCase("\"") && !fV.equalsIgnoreCase("\\\"")) {
                      listMutivalued.add(fV)
                      postKV(key, fV)
                      fieldAdd(xml, key, fV)
                    }
                  }
                }
                objMap.put(key, listMutivalued)
                isMultiValued = true
                break
              }
            }
          }
        }
        if (!isMultiValued) {
          //if not multivalued,need save singleValue to xml
          if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase("null") && !value.equalsIgnoreCase("\"\"")) {
            value = value.replaceAll("^\"([\\S|\\s]+)\"$", "$1")
            value = value.replaceAll("^\"([\\S|\\s]+)", "$1")
            value = value.replaceAll("(\\[\\S|\\s]+)\"$", "$1")
            value = value.trim
            if (!value.equalsIgnoreCase("") && !value.equalsIgnoreCase("\"")) {
              postKV(key, value)
              fieldAdd(xml, key, value)
              objMap.put(key, value)
            }
          }
        }

      }
    }
    if (!objMap.isEmpty) listMap.add(objMap)
    xml.append("</doc>")
    xml.append("\n")
  }

  def fieldAdd(xml: StringBuilder, key: String, value: Object) = {
    xml.append("<field name=\"")
    xml.append(key)
    xml.append("\">")
    xml.append("<![CDATA[")
    xml.append(value)
    xml.append("]]>")
    xml.append("</field>")
    xml.append("\n")
  }

  override def indexData(data: AnyRef, collection: String): Boolean = {
    try {
      val r = solrClient.addIndices(data, collection)
      //solrClient.close()
      r
      true
    } catch {
      case e: Exception => false
    }
  }

  /**
    * delete index
    * eg: delete-234523-34534
    *
    * @param ids
    * @return
    */
  override def delete(ids: java.util.ArrayList[java.lang.String], collection: String): Boolean = {
    val r = solrClient.delete(ids, collection)
    //solrClient.close()
    r
  }

  private def deleteByQuery(query: String, collection: String): Boolean = {
    val r = solrClient.deleteByQuery(query, collection)
    r
  }

  private def writeToDisk(xml: String, filePath: String) = {
    val w = new PrintWriter(filePath)
    w.println(xml)
    w.close()
  }
}

object DefaultIndexManager extends SolrConfiguration {
  var indexManager: DefaultIndexManager = null

  var coreThreadsNumber = consumerCoreThreadsNum


  var currentThreadsNum = Util.inferCores() * coreThreadsNumber


  if (consumerThreadsNum > 0) currentThreadsNum = consumerThreadsNum
  val consumerManageThreadPool = Util.newDaemonFixedThreadPool(currentThreadsNum, "consumer_index_manage_thread_excutor")


  val indexProcessThreadPool = Util.newDaemonFixedThreadPool(currentThreadsNum, "index_process_thread_excutor")
  var bus: ManagerListenerWaiter = _

  def apply(): DefaultIndexManager = {
    if (indexManager == null) indexManager = new DefaultIndexManager()
    indexManager
  }

  if (useSolr) {
    bus= ManagerListenerWaiter()
    bus.listeners.add(new IndexTaskTraceListener())
    bus.start()
  }


  val requestStartTime_key = "requestStartTime"


  val collection_key = "collection"

  val params_key = "params"


  val waiteThreadsNum = 3000

}


object TestIndexManger extends Logging {
  def main(args: Array[String]) {
    // forTest
    //testRegext
    //testJsonParser
    testStartGeneralXmlAndIndex
  }


  def testStartGeneralXmlAndIndex() = {
    val d = DefaultIndexManager()
    val responseData = "{\"data\":[{\"brandEn\":\"Greatwall\",\"brandId\":6384,\"brandZh\":\"长城精工\",\"category1\":\"工具\",\"category2\":\"扳手/手动套筒\",\"category3\":\"扳手/手动套筒\",\"category4\":\"活动扳手\",\"categoryId\":2955,\"categoryId1\":5,\"categoryId2\":71,\"categoryId3\":16887,\"categoryId4\":3883,\"cityId\":\"\",\"createDate\":1447127123210,\"deliveryTime\":\"3\",\"id\":\"1444999\",\"isRestrictedArea\":0,\"keywords\":\"长城精工,豪华型带刻度活扳手,375mm(15\\\"),300635\",\"minOrder\":\"1\",\"original\":\"300635\",\"picUrl\":\"http://image.ehsy.com/uploadfile/T/SFC073.jpg|http://image.ehsy.com/uploadfile/T/SFC073_1.jpg\",\"price\":84.0000,\"salesUnit\":\"个\",\"sku\":\"SFC073\",\"title\":\"长城精工,豪华型带刻度活扳手,375mm(15\\\"),300635\",\"updateDate\":1459052860673}],\"mark\":\"0\",\"message\":\"SUCCESS\",\"totalCount\":259417}"
    d.startGeneralXmlAndIndex("mergescloud", responseData)

  }

  def testJsonParser() = {
    val om = new ObjectMapper()
    val responseData = "{\"data\":[{\"brandEn\":\"Greatwall\",\"brandId\":6384,\"brandZh\":\"长城精工\",\"category1\":\"工具\",\"category2\":\"扳手/手动套筒\",\"category3\":\"扳手/手动套筒\",\"category4\":\"活动扳手\",\"categoryId\":2955,\"categoryId1\":5,\"categoryId2\":71,\"categoryId3\":16887,\"categoryId4\":3883,\"cityId\":\"\",\"createDate\":1447127123210,\"deliveryTime\":\"3\",\"id\":\"1444999\",\"isRestrictedArea\":0,\"keywords\":\"长城精工,豪华型带刻度活扳手,375mm(15\\\"),300635\",\"minOrder\":\"1\",\"original\":\"300635\",\"picUrl\":\"http://image.ehsy.com/uploadfile/T/SFC073.jpg|http://image.ehsy.com/uploadfile/T/SFC073_1.jpg\",\"price\":84.0000,\"salesUnit\":\"个\",\"sku\":\"SFC073\",\"title\":\"长城精工,豪华型带刻度活扳手,375mm(15\\\"),300635\",\"updateDate\":1459052860673}],\"mark\":\"0\",\"message\":\"SUCCESS\",\"totalCount\":259417}"
    try {
      val obj = om.readTree(responseData)
      if (obj == null) logInfo(s"response null,size:0")
      else {
        if (obj.isInstanceOf[JsonNode]) {
          //generate add index xml
          val dataJsonNode = obj.asInstanceOf[JsonNode]
          logInfo(s"response size:${dataJsonNode.get("data").size()}")
        }
      }

      val jsonObject = JSON.parseObject(responseData)
      println(jsonObject)


      val value = ""
      val v = new ObjectJson()
      val vString = om.writeValueAsString(v)
      println(vString)
    }
  }

  def testRegext() = {
    var fV: String = "8\\”".trim.replaceAll("^\"([\\S|\\s]+)\"$", "$1")
    println(fV)
    println("8\\\"")
  }

  def forTest() = {
    for (i <- 0 to 3) {
      println(i)
    }
  }

}
