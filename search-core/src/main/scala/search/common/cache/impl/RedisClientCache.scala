package search.common.cache.impl

import com.alibaba.fastjson.JSON
import search.common.cache.KVCache
import search.common.config.RedisConfiguration
import search.common.entity.state.ProcessState
import search.es.client.util.EsClientConf

import scala.reflect._

/**
  * Created by soledede.weng on 2016/8/3.
  */
class RedisClientCache(conf: EsClientConf) extends KVCache with RedisConfiguration {
  val keyPreffix = s"$nameSpace:"

  override def put[T: ClassTag](key: String, value: T, seconds: Int): Unit = {
    conf.storage.setStringByKey(keyPreffix + key, JSON.toJSONString(value, false),seconds)
  }

  override def get(key: String): Int = {
    val result = conf.storage.getBykey[String](keyPreffix + key)
    if (result != null) result.toInt
    else -1
  }

  override def getObj[T: ClassTag](key: String): T = {
    val state = conf.storage.getBykey[String](keyPreffix + key)
    if (state == null) state.asInstanceOf[T]
    else{
      val result = JSON.parseObject[T](state, classTag[T].runtimeClass)
      result
    }

  }
}
