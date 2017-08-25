package search.common.config

import scala.util.Try

/**
  * Created by soledede.weng on 2016/6/6.
  */
trait RedisConfiguration extends Configuration {
  lazy val redisDbName: String = Try(config.getString("redis.dbName")).getOrElse("default")
  lazy val expireTime: Int = Try(config.getInt("redis.expire")).getOrElse(604800)
  lazy val nameSpace: String = Try(config.getString("redis.namespace")).getOrElse("graph_state")

}
