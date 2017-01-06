package search.common.cache.impl

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, CacheLoader}
import search.common.cache.KVCache
import search.common.config.RedisConfiguration
import search.common.entity.bizesinterface._
import search.common.entity.state.ProcessState

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Created by soledede.weng on 2016/8/3.
  */
private[search] class LocalCache extends KVCache {


  import search.common.cache.impl.LocalCache._

  override def put[T: ClassTag](key: String, value: T, seconds: Int): Unit = {
    stateCacheManager.put(key, value.asInstanceOf[ProcessState])
    try {
     // stateCacheManager.refresh(key)
    } catch {
      case e: Exception =>
    }
  }


  override def getObj[T: ClassTag](key: String): T = {

    val result = stateCacheManager.getIfPresent(key).asInstanceOf[T]
    try {
     // stateCacheManager.refresh(key)
    } catch {
      case e: Exception =>
    }
    result
  }

  override def cleanAll(preffix:String = null): Boolean = {
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


  /**
    * Map(code->Set(topic1,topic2....))
    */
  final val codeToTopicSet = new mutable.HashMap[String, mutable.HashSet[String]]()

  /**
    * cache stock code mapping to stock name
    */
  final val codeToCompanyNameCache = new mutable.HashMap[String, String]()

  /**
    * cache stock name mapping to stock code
    */
  final val stockNameToCodeCache = new mutable.HashMap[String, String]()

  //cache Map(stockName->stockEntity)
  final val baseStockCache = new mutable.HashMap[String, BaseStock]()

  //cache Map(id->CompanyStock)
  final val companyStockCache = new mutable.HashMap[String,CompanyStock]()

  //cache Map(id->Industry)
  final val industryCache = new mutable.HashMap[String,Industry]()

  //cache Map(id->GraphEvent)
  final val eventCache = new mutable.HashMap[String,GraphEvent]()

  //cache Event cache
  final val eventSet = new mutable.HashSet[String]()

  //cache Map(id->GraphTopic)
  final val topicCache = new mutable.HashMap[String,GraphTopic]()


  //Map(companyName->weight)
  final val conmpanyWeightCache = new mutable.HashMap[String,Double]()

  //Map(topicName->weight)
   var topicHotWeightCache = new mutable.HashMap[String,Double]()

}
