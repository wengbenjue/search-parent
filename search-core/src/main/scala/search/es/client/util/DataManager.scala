package search.es.client.util

import java.util

import com.mongodb.{DBCollection, BasicDBObject, DBObject}
import org.bson.types.ObjectId
import search.common.mongo.base.{MongoTableConstants, MongoBase}
import search.common.util.Logging
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

  def getStockCollection: DBCollection = {
    getCollection(MongoTableConstants.ADA_BASE_STOCK)
  }

  def getCompanyCollection: DBCollection = {
    getCollection(MongoTableConstants.GRAPH_COMPANY_DIC)
  }

  //graph.topic_conp
  def getGraphTopicConpCollection: DBCollection = {
    getCollection(MongoTableConstants.GRAPH_TOPIC_CONP)
  }

  //news.dict_news_rule
  def getDictNewsRuleCollection:DBCollection = {
    getCollection(MongoTableConstants.NEWS_DICT_NEWS_RULE)
  }

  //news.sens_industry
  def getSensIndustryCollection:DBCollection = {
    getCollection(MongoTableConstants.NEWS_SENS_INDUSTRY)
  }









  def deleteAllData() = {
    try {
      val result = getCollection.remove(new BasicDBObject())
      true
    } catch {
      case e: Exception => false
    }
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

  def findBaseStock(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("code", Integer.valueOf(1))
    projection.put("name.en", Integer.valueOf(1))
    projection.put("name.szh", Integer.valueOf(1))
    projection.put("org.en", Integer.valueOf(1))
    projection.put("org.szh", Integer.valueOf(1))
    val query = new BasicDBObject()
    // query.put("name.szh", new BasicDBObject("$ne", null));
    val dbCurson = getStockCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }

  def findCompanyCode():java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("code", Integer.valueOf(1))
    projection.put("w", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getCompanyCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  def findGrapnTopicConp(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("code", Integer.valueOf(1))
    projection.put("w", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getGraphTopicConpCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }

  def findEvent(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("szh", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getDictNewsRuleCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  def findIndustry(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("c", Integer.valueOf(1))
    projection.put("w", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getSensIndustryCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }



  def queryALl() = {
    val cur = getCollection().find()
    while (cur.hasNext) {
      cur.next()
    }
  }

  def queryOneByKeyWord(keyword: String): java.util.Map[_, _] = {
    try {
      val dbResult = getCollection().findOne(new BasicDBObject("keyword", keyword))
      if (dbResult == null) return null
      else dbResult.toMap
    } catch {
      case e: Exception =>
        null
    }
  }

  def insert(docs: java.util.List[BasicDBObject]) = {
    try {
      getCollection.insert(docs)
    } catch {
      case e: Exception =>
    }
  }

  def findAndRemove(keyword: String): java.util.Map[_, _] = {
    try {
      val dbResult = getCollection().findAndRemove(new BasicDBObject("keyword", keyword))
      if (dbResult == null) return null
      else dbResult.toMap
    } catch {
      case e: Exception =>
        null
    }
  }


}

private[search] object DataManager {


  def main(args: Array[String]) {
    //testLoadGraphNodeDataToMongo
    //testQueryOneByKeyWord
    testFindBaseStock
  }

  def testFindBaseStock() = {
    val dm = new DataManager(new EsClientConf())
    val result = dm.findBaseStock()
    println(result)
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



