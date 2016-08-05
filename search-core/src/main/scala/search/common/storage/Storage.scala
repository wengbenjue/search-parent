package search.common.storage

import scala.reflect.ClassTag

/**
  * Created by soledede.weng on 2016/6/28.
  */
private[search] trait Storage {
  def getBykey[T: ClassTag](key: String): T

  def getStringBykey(key: String): String

  def getAllByKeyPreffix[T: ClassTag](preffix: String): Seq[T]

  def setStringByKey(key: String,value: String,seconds: Int=60*1000): String

  def del(keys: String*)

  def keys(keyPreffix: String):Set[String]

  def zrevrangeByScore(key: String, max: String, min: String): Seq[String]

}

private[search] object Storage {

  def apply(sType: String="redis"): Storage = {
    sType match {
      case "redis" => RedisStorage()
      case _ => null.asInstanceOf[Storage]
    }
  }

}
