package search.common.storage

import java.util.Calendar

import com.alibaba.fastjson.JSON
import com.aug3.storage.redisclient.cache.CacheCluster
import search.common.config.RedisConfiguration
import search.common.redis.RedisClient
import search.common.util.Logging
import scala.collection.JavaConversions._

import scala.reflect.ClassTag

/**
  * Created by soledede.weng on 2016/6/29.
  */
private[search] class RedisStorage private extends Storage with Logging with RedisConfiguration {


  override def getStringBykey(key: String): String = {
    val client = RedisClient("close")
    client.get(key)
  }


  override def setStringByKey(key: String, value: String, seconds: Int = expireTime): String = {
    val client = RedisClient("default")
    var result: String = null
    try {
      result = client.set(key, value)
      client.expire(key, seconds)
    } catch {
      case e: Exception =>
    } finally {
      client.close()
    }
    result
  }


  override def del(keys: String*): Boolean = {
    val client = RedisClient("default")
    try {
        //val client = RedisClient("close")
      client.del(keys: _*)
      true
    } catch {
      case e: Exception =>
        false
    }finally {
      client.close()
    }
  }


  override def keys(keyPreffix: String): Set[String] = {
    val client = RedisClient("default")
    try {
      client.keys(keyPreffix).toSet
    } catch {
      case e: Exception =>
        null
    }finally {
      client.close()
    }
  }


  override def zrevrangeByScore(key: String, max: String, min: String): Seq[String] = {
    val client = RedisClient("close")
    val result = client.zrevrangeByScore(key, max, min)
    if (result == null) null
    else result.toSet.toSeq

  }

  override def getBykey[T: ClassTag](key: String): T = {
    val client = RedisClient("default")
    try {
      val result = client.get(key).asInstanceOf[T]
      client.expire(key, expireTime)
      result
    } catch {
      case e: Exception =>
        logError("get cache from redis faield!", e)
        null.asInstanceOf[T]
    } finally {
      client.close()
    }

  }

  override def getAllByKeyPreffix[T: ClassTag](preffix: String): Seq[T] = ???
}

object RedisStorage {
  var redisStorage: RedisStorage = null

  def apply(): RedisStorage = {
    if (redisStorage == null) {
      this.synchronized {
        if (redisStorage == null) {
          redisStorage = new RedisStorage()
        }
      }
    }
    redisStorage
  }

  def main(args: Array[String]) {
    //testDelKeys
    testGetRedisPool
  }

  def testGetRedisPool() = {
    var cnt = 0
    val storage =  RedisStorage()
    for(i <- 0 to 1000000*2){
      println(storage.getBykey[String]("cache:otc:visit_history_12374"))
      cnt += 1
      println(cnt)
    }


  }

  def testDelKeys(): Unit = {
    val sets = RedisStorage().keys("graph_state:*")
    sets.foreach(RedisStorage().del(_))
  }
}
