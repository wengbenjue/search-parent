package search.common.redis


import search.solr.client.SolrClientConf
import search.common.config.{SolrConfiguration, Configuration}
import search.common.serializer.Serializer
import search.common.util.{JavaUtils, Logging}

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

/**
  * Created by soledede on 2016/4/8.
  */
private[search] class JRedisImpl extends Redis with Logging {
 // val jedis = JedisClient.getRedisFromPool()
  val jedis = JedisClient.createJredis()

  override def put[T: ClassTag](key: String, value: T, seconds: Int): Unit = {
    try {
      val objSerializeByte = Serializer("java", new SolrClientConf()).newInstance().serializeArray[T](value)
      val k = JavaUtils.toJavaByte(key.getBytes)
      jedis.set(k, JavaUtils.toJavaByte(objSerializeByte),"NX".getBytes,"EX".getBytes,seconds.toLong)
      //val l = jedis.expire(k, seconds)
    } catch {
      case e: Exception => logError("save object to redids faield!", e)
    }
  }

  override def get[T: ClassTag](key: String): T = {
    try {
      val obj = jedis.get(key.getBytes)
      if (obj != null)
        Serializer("java", new SolrClientConf()).newInstance().deserialize[T](obj)
      else null.asInstanceOf[T]
    } catch {
      case e: Exception => logError(s"get $key faield", e)
        null.asInstanceOf[T]
    }
  }
}

object JRedisImpl extends SolrConfiguration {
  var jRedisImpl: JRedisImpl = null

  def apply(): JRedisImpl = {
    if (jRedisImpl == null)
      jRedisImpl = new JRedisImpl()
    jRedisImpl
  }


}
