package search.common.algorithm

/**
  * Created by soledede.weng on 2016/9/12.
  */
trait Dictionary extends Serializable{
  def add(word: String): Unit

  def remove(word: String): Unit

  def removeAll(items: java.util.List[String]): Unit

  def clear(): Unit

  def prefix(preffix: String): java.util.List[String]

  def contains(word: String): Boolean

}
