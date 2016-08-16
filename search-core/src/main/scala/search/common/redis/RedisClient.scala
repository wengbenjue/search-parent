package search.common.redis

import akka.actor.ActorSystem
import com.aug3.storage.redisclient.{OneOffRedisAdaptor, RedisAdaptor}
import redis.RedisCommands
import search.common.config.{RedisConfiguration, SolrConfiguration}

/**
  * Created by soledede on 2015/12/18.
  */
object RedisClient   extends SolrConfiguration with RedisConfiguration {
  var redisClient: RedisCommands = _
  implicit var akkaSystem: ActorSystem = _
  if (useSolr) {
    akkaSystem = akka.actor.ActorSystem()
    //redisClient = redis.RedisClient(port = redisPort, host = redisHost)
  }


  def apply(pattern: String): RedisAdaptor = {
    pattern match {
      case "default" => RedisAdaptor.getClient(redisDbName)
      case "close" => OneOffRedisAdaptor.getClient(redisDbName)
    }

  }

  def apply() = {
    redisClient
  }

  def close() = {
    /*if (redisClient != null)
      redisClient.shutdown()*/
    if (akkaSystem != null)
      akkaSystem.shutdown()
  }

}
