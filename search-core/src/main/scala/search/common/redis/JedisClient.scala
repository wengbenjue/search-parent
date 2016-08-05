package search.common.redis

import org.apache.commons.lang3.StringUtils
import redis.clients.jedis.{JedisPool, JedisPoolConfig, Jedis}
import redis.clients.jedis.Jedis
import search.common.config.{SolrConfiguration, Configuration}

/**
  * Created by soledede on 2016/4/16.
  */
private[search] object JedisClient extends SolrConfiguration {

  val lockPool = new Object()

  var pool: JedisPool = null

  var jSInRedis: Jedis = null

  def createJredis(): Jedis = {
    if (this.jSInRedis == null)
      this.jSInRedis = new Jedis(redisHost, redisPort)
    jSInRedis
  }

  def createJredis(host: String, port: Int): Jedis = {
    val jedis = new Jedis(host, port)
    jedis
  }

  def createJredis(host: String, port: Int, password: String): Jedis = {
    val jedis = new Jedis(host, port)
    if (!StringUtils.isNotBlank(password))
      jedis.auth(password)
    jedis
  }


  def createJedisPool(): JedisPool = {
    //collection pool config
    val config = new JedisPoolConfig()
    //setup maxmum collection number
    config.setMaxTotal(1000)
    //setup maxmum block
    config.setMaxWaitMillis(1000 * 2)
    config.setMaxIdle(10)
    val jedis = new JedisPool(config, redisHost, redisPort)
    jedis
  }


  def poolInit() = {
    if (this.pool == null) {
      this.lockPool.synchronized {
        this.pool = createJedisPool()
      }
    }
  }

  def getRedisFromPool(): Jedis = {
    if (this.pool == null)
      this.poolInit()
    this.pool.getResource
  }

  def returnRedis(redis: Jedis) = {
    this.pool.returnResource(redis)
  }
}
