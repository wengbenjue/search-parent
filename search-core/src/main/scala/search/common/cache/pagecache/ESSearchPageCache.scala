package search.common.cache.pagecache

import search.common.config.{RedisConfiguration}
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

  /**
    *
    * @param query
    * @param block
    * @return
    */
  def cacheShowStateAndGetByQuery(query: String, block: => AnyRef): AnyRef = {
    val cache = getShowStateAndGetByQueryCache(query)
    if (cache != null) cache
    else {
      val result = block
      if (result != null)
        putShowStateAndGetByQueryCache(query, result)
      result
    }
  }

  private def getShowStateAndGetByQueryCache(query: String): AnyRef = {
    val stringBuilder = showStateAndGetByQueryCache(query)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[AnyRef](stringBuilder.toString())
    else null
  }

  private def putShowStateAndGetByQueryCache(query: String, result: AnyRef): Unit = {
    val stringBuilder = showStateAndGetByQueryCache(query)
    if (stringBuilder != null && !stringBuilder.isEmpty && result != null)
      cache.put(stringBuilder.toString.trim, result, cacheTime)
  }

  private def showStateAndGetByQueryCache(query: String): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("showStateAndGetByQueryCache").append(separator)
    if (query != null && !query.trim.equalsIgnoreCase(""))
      stringBuilder.append(query.trim).append(separator)
    stringBuilder
  }
}
