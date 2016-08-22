package search.common.cache

import search.common.cache.impl.{JRedisCache, RedisCache}
import search.recommend.domain.entity.RecommendResult
import search.common.entity.searchinterface.FilterAttributeSearchResult

import scala.reflect.ClassTag


/**
  * Created by soledede on 2015/12/18.
  */
trait KVCache {

  //save one week
  def incrby(key: String, step: Int = 1, expiredTime: Long = 60 * 60 * 24 *5): Int = -1

  def get(key: String): Int = -1

  def getR(key: String): Seq[RecommendResult] = null

  def getObj[T: ClassTag](key: String): T = null.asInstanceOf[T]

  def put[T: ClassTag](key: String, value: T,seconds:Int): Unit = {}
  def putR(key: String, value: Seq[RecommendResult], expiredTime: Long = 60 * 60 * 2): Boolean = false

  def keys(preffixKey: String): Seq[String] = null.asInstanceOf[Seq[String]]

  def getCache[T: ClassTag](key: String): T = null.asInstanceOf[T]

  def putCache[T: ClassTag](key: String, value: T, expiredTime: Long = 60 * 60 * 2): Boolean = false

  def cleanAll(preffix: String = null): Boolean = false


}

object KVCache {
  def apply(kvType: String = "redis"): KVCache = {
    kvType match {
      case "redis" => RedisCache()
      case "jRedis" => JRedisCache()
      case _ => null
    }
  }

    def main(args: Array[String]) {
      testKV
    }

    def testKV() = {
      val kvCache = KVCache("jRedis")
      kvCache.put("t1", new FilterAttributeSearchResult(),5)
      val ko1 = kvCache.get("t1")
      println(ko1)
    }
}
