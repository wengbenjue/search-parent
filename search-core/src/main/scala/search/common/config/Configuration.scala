package search.common.config

import com.typesafe.config.ConfigFactory
import search.common.util.Util

import scala.util.Try

/**
  * Created by soledede on 2016/2/14.
  */
private[search] trait Configuration {
  /**
    * Application config object.
    */
  val config = ConfigFactory.load()


  /**
    * log4j
    */

  lazy val logShow = Try(config.getBoolean("log.show")).getOrElse(true)

  /**
    * kafka
    */

  lazy val startKafka = Try(config.getBoolean("kafka.brokers")).getOrElse(false)
  lazy val brokers = Try(config.getString("kafka.brokers")).getOrElse("localhost:9092")
  lazy val productTopic = Try(config.getString("kafka.producter.topic")).getOrElse("test")
  lazy val productType = Try(config.getString("kafka.producter.type")).getOrElse("async")
  lazy val serializerClass = Try(config.getString("kafka.serializer.class")).getOrElse("kafka.serializer.StringEncoder")
  lazy val consumerTopic = Try(config.getString("kafka.consumer.topic")).getOrElse("test")
  lazy val zk = Try(config.getString("zk")).getOrElse("localhost:2181")
  lazy val groupId = Try(config.getString("kafka.groupid")).getOrElse("group1")
  /**
    * zk backup for search balance
    */
  lazy val zkBackUp = Try(config.getString("zkBackUp")).getOrElse("localhost:2181")


  //cache time  second

  lazy val cacheTime = Try(config.getInt("cache.time")).getOrElse(5)


  //redis
  lazy val redisHost = Try(config.getString("redis.host")).getOrElse("localhost")
  lazy val redisPort = Try(config.getInt("redis.port")).getOrElse(6379)


  //monitoru host
  lazy val monitorConfigHost = Try(config.getString("monitor.host")).getOrElse(Util.localHostNameForURI())
  var monitorHost = "127.0.0.1"
  if (monitorConfigHost != null) monitorHost = monitorConfigHost
  lazy val monitorPort = Try(config.getInt("monitor.port")).getOrElse(9999)

  //web
  val WEB_STATIC_RESOURCE_DIR = "static"
  lazy val webStart = Try(config.getBoolean("web.start")).getOrElse(false)

  /**
    * threads pool
    */

  lazy val consumerThreadsNum = Try(config.getInt("consumer.threads.number")).getOrElse(0)

  lazy val consumerCoreThreadsNum = Try(config.getInt("consumer.core.threads.number")).getOrElse(2)

  lazy val threadsWaitNum = Try(config.getInt("threads.wait.number")).getOrElse(50000)

  lazy val threadsSleepTime = Try(config.getInt("threads.sleep")).getOrElse(1000)

  lazy val word2VecNewsfinalPath = Try(config.getString("word2vec.path.newsfinal")).getOrElse(null)

  lazy val topN = Try(config.getInt("word2vec.topN")).getOrElse(5)
  /**
    * bloom filter
    */
  lazy val expectedElements: Long = Try(config.getLong("bloomfilter.expectedElements")).getOrElse(10000)

  lazy val falsePositiveRate: Double = Try(config.getDouble("bloomfilter.falsePositiveRate")).getOrElse(0.1)

  lazy val fluidLimit = Try(config.getInt("fluid.limit")).getOrElse(2000)

  lazy val firstCharacterIndexLength = Try(config.getInt("dictionary.index.first_character_index_length")).getOrElse(20000)

}
