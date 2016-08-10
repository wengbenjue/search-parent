package search.common.cache.impl

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader}
import search.common.cache.KVCache
import search.common.config.RedisConfiguration
import search.common.entity.state.ProcessState

import scala.reflect.ClassTag

/**
  * Created by soledede.weng on 2016/8/3.
  */
private[search] class LocalCache extends KVCache {


  import search.common.cache.impl.LocalCache._

  override def put[T: ClassTag](key: String, value: T, seconds: Int): Unit = {
    stateCacheManager.put(key, value.asInstanceOf[ProcessState])

  }


  override def getObj[T: ClassTag](key: String): T = {
    stateCacheManager.getIfPresent(key).asInstanceOf[T]
  }

  override def cleanAll(): Boolean = {
    try {
      stateCacheManager.invalidateAll()
      true
    } catch {
      case e: Exception =>
        false
    }
  }
}

object LocalCache extends RedisConfiguration {

  private val stateCacheLoader: CacheLoader[java.lang.String, ProcessState] =
    new CacheLoader[java.lang.String, ProcessState]() {
      def load(key: java.lang.String): ProcessState = {
        null
      }
    }

  val stateCacheManager = CacheBuilder.newBuilder().expireAfterWrite(expireTime, TimeUnit.MINUTES).build(stateCacheLoader)
}
