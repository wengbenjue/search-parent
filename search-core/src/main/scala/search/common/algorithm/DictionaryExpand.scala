package search.common.algorithm

/**
  * Created by soledede.weng on 2016/9/13.
  */
trait DictionaryExpand extends Serializable{
  def add(word: String,id: String): Unit

  /**
    *
    * @param prefix
    * @return (word,id)
    */
  def prefixWitId(prefix: String): java.util.List[(String,String)]

  def removeWithId(word: String): Unit

  def findWithId(word: String): String
}
