package search.common.cache.pagecache

import search.common.config.{RedisConfiguration}
import search.common.util.{FinshedStatus, KnowledgeGraphStatus}
import search.es.client.biz.BizeEsInterface

import scala.collection.JavaConversions._
import scala.collection.mutable.StringBuilder

/**
  * Created by soledede on 2016/4/16.
  */
private[search] object ESSearchPageCache extends RedisConfiguration {
  val conf = BizeEsInterface.conf
  val cache = conf.esPageCache
  val separator = "&_&"
  val keyPreffix = s"$nameSpace:"


  def cacheRequestNlpByQuery(query: String, showLevel:Integer,block: => AnyRef): AnyRef = {
    val cache = getRequestNlpByQueryCache(query,showLevel)
    if (cache != null) {
      //BizeEsInterface.updateState(null, query, KnowledgeGraphStatus.NLP_PROCESS, FinshedStatus.FINISHED)
      cache
    }
    else {
      val result = block
      if (result != null)
        putRequestNlpByQueryCache(query,showLevel, result)
      result
    }
  }

  private def getRequestNlpByQueryCache(query: String,showLevel:Integer): AnyRef = {
    val stringBuilder = requestNlpGetByQueryCache(query,showLevel)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[AnyRef](stringBuilder.toString())
    else null
  }

  private def putRequestNlpByQueryCache(query: String, showLevel:Integer,result: AnyRef): Unit = {
    val stringBuilder = requestNlpGetByQueryCache(query,showLevel)
    if (stringBuilder != null && !stringBuilder.isEmpty && result != null)
      cache.put(stringBuilder.toString.trim, result, cacheTime)
  }

  private def requestNlpGetByQueryCache(query: String,showLevel:Integer): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("requestNlpGetByQueryCache").append(separator)
    if (query != null && !query.trim.equalsIgnoreCase(""))
      stringBuilder.append(query.trim).append(separator)
    if (showLevel != null && !showLevel.toString.trim.equalsIgnoreCase(""))
      stringBuilder.append(showLevel.toString.trim).append(separator)
    stringBuilder
  }


  /**
    *
    * @param query
    * @param block
    * @return
    */
  def cacheShowStateAndGetByQuery(query: String,showLevel:Integer, block: => AnyRef): AnyRef = {
    val cache = getShowStateAndGetByQueryCache(query,showLevel)
    if (cache != null) {
      //BizeEsInterface.updateState(null, query, KnowledgeGraphStatus.FETCH_PROCESS, FinshedStatus.FINISHED)
      cache
    }
    else {
      val result = block
      if (result != null)
        putShowStateAndGetByQueryCache(query,showLevel, result)
      result
    }
  }

  private def getShowStateAndGetByQueryCache(query: String,showLevel:Integer): AnyRef = {
    val stringBuilder = showStateAndGetByQueryCache(query,showLevel)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[AnyRef](stringBuilder.toString())
    else null
  }

  private def putShowStateAndGetByQueryCache(query: String,showLevel:Integer, result: AnyRef): Unit = {
    val stringBuilder = showStateAndGetByQueryCache(query,showLevel)
    if (stringBuilder != null && !stringBuilder.isEmpty && result != null){
      val key = stringBuilder.toString.trim
      cache.put(key, result, cacheTime)
    }

  }

  private def showStateAndGetByQueryCache(query: String,showLevel:Integer): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("showStateAndGetByQueryCache").append(separator)
    if (query != null && !query.trim.equalsIgnoreCase(""))
      stringBuilder.append(query.trim).append(separator)
    if (showLevel != null && !showLevel.toString.trim.equalsIgnoreCase(""))
      stringBuilder.append(showLevel.toString.trim).append(separator)
    stringBuilder
  }
}
