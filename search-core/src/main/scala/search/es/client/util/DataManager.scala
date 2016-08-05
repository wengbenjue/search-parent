package search.es.client.util

import java.util

import com.mongodb.{DBCollection, BasicDBObject, DBObject}
import org.bson.types.ObjectId
import search.common.mongo.base.{MongoTableConstants, MongoBase}
import scala.collection.JavaConversions._

import scala.io.Source

/**
  * Created by soledede.weng on 2016/7/27.
  */
private[search] class DataManager(conf: EsClientConf) extends MongoBase {


  override def getCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_GRAPH_KEYWORDS)
  }


  def getLogCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_SEARCH_LOGS)
  }

  def loadGraphNodeDataToMongo() = {
    //val filePath = "D:\\news_graph.dic"
    val filePath = "D:\\all_nodes.txt"
    var cnt = 0
    val collection = getCollection()
    var list = new util.ArrayList[BasicDBObject]()
    for (line <- Source.fromFile(filePath).getLines if (!line.trim.equalsIgnoreCase(""))) {
      val keyWord = line.trim
      println(s"get keyword from localfile $filePath")
      val dbObject = new BasicDBObject()
      dbObject.append("_id", cnt)
      dbObject.append("keyword", keyWord)
      dbObject.append("updateDate", System.currentTimeMillis())
      list.add(dbObject)
      if (list.size() % 2000 == 0) {
        collection.insert(list)

        list = new util.ArrayList[BasicDBObject]()
        println(s"insert keywords to monggo successfully,size:${list.size()}")
      }
      cnt += 1
    }
    if (list.size() > 0)
      collection.insert(list)

    println(s"All successfully insert to mongo,total number:$cnt")
  }

  def queryALl() = {
    val cur = getCollection().find()
    while (cur.hasNext) {
      cur.next()
    }
  }

  def queryOneByKeyWord(keyword: String): java.util.Map[_, _] = {
    val dbResult = getCollection().findOne(new BasicDBObject("keyword", keyword))
    if (dbResult == null) return null
    else dbResult.toMap
  }

  def findAndRemove(keyword: String): java.util.Map[_, _] = {
    val dbResult = getCollection().findAndRemove(new BasicDBObject("keyword", keyword))
    if (dbResult == null) return null
    else dbResult.toMap
  }


}

private[search] object DataManager {


  def main(args: Array[String]) {
    testLoadGraphNodeDataToMongo
    //testQueryOneByKeyWord
  }

  def testLoadGraphNodeDataToMongo() = {
    val dm = new DataManager(new EsClientConf())
    dm.loadGraphNodeDataToMongo()
    dm.queryALl()
  }

  def testQueryOneByKeyWord() = {
    val dm = new DataManager(new EsClientConf())
    // dm.loadGraphNodeDataToMongo()
    val data = dm.queryOneByKeyWord("通信设备")
    println(data)
  }
}



