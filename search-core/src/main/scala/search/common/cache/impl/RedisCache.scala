package search.common.cache.impl

import akka.util.ByteString
import net.liftweb.json.DefaultFormats
import net.liftweb.json.Serialization._
import redis.ByteStringFormatter
import search.common.cache.KVCache
import search.recommend.domain.entity.RecommendResult
import search.common.redis.Redis
import search.common.util.Logging

import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/12/18.
  */
class RedisCache private extends KVCache with Logging {

  val redis = Redis()


  implicit val formats = DefaultFormats

  implicit val byteStringFormatter = new ByteStringFormatter[Seq[RecommendResult]] {
    def serialize(recomends: Seq[RecommendResult]): ByteString = {
      ByteString(
        write(recomends)
      )
    }

    def deserialize(bs: ByteString): Seq[RecommendResult] = {
      val r = bs.utf8String
      read[List[RecommendResult]](r)
    }
  }


  private val Seperator = "#&#&#"
  implicit val stringFormatter = new ByteStringFormatter[Seq[String]] {
    def serialize(vS: Seq[String]): ByteString = {
      ByteString(
        write(vS)
      )
    }

    def deserialize(bs: ByteString): Seq[String] = {
      val r = bs.utf8String
      read[Seq[String]](r)
    }
  }


  //save one week
  override def incrby(key: String, step: Int, expiredTime: Long = 60 * 60 * 24 * 5): Int = {
    redis.incrBy(key, step)
  }

  override def get(key: String): Int = {
    val v = redis.getValue[String](key)
    if (v == null) 0
    else {
      val value = v.getOrElse("0")
      if (value == null || value.equalsIgnoreCase("") || value.equalsIgnoreCase("0")) 0
      else Integer.valueOf(value)
    }
  }

  override def getR(key: String): Seq[RecommendResult] = {
    val oR = redis.getValue[Seq[RecommendResult]](key)
    if (oR == null) null
    else oR.getOrElse(null)
  }

  override def keys(preffixKey: String): Seq[String] = {
    redis.keys(preffixKey).getOrElse(null)
  }

  override def putR(key: String, value: Seq[RecommendResult], expiredTime: Long = 60 * 60 * 2): Boolean = {
    redis.setValue[Seq[RecommendResult]](key, value, expiredTime)
  }

  override def getCache[T: ClassTag](key: String): T = {
    val oR = redis.getValue[String](key)
    if (oR == null) null.asInstanceOf[T]
    else oR.getOrElse(null).asInstanceOf[T]
  }

  override def putCache[T: ClassTag](key: String, value: T, expiredTime: Long = 60 * 60 * 2): Boolean = {
    redis.setValue[String](key, value.toString, expiredTime)
  }

}

object RedisCache {
  var redisCache: RedisCache = null

  def apply(): RedisCache = {
    if (redisCache == null) redisCache = new RedisCache
    redisCache
  }

}

