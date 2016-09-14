package search.es.client.util

import java.util
import java.util.Date

import com.mongodb._
import org.bson.types.ObjectId
import search.common.mongo.base.{MongoBase, MongoTableConstants}
import search.common.util.{Logging, Util}

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
  def getDictNewsRuleCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_DICT_NEWS_RULE)
  }


  //topic.topic_hot
  def getTopicHotCollection: DBCollection = {
    getCollection(MongoTableConstants.TOPIC_TOPIC_HOT)
  }

  //news.sens_industry
  def getSensIndustryCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_SENS_INDUSTRY)
  }


  def getNewsEntityMultiRelCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_ENTITY_MULTI_REL)
  }

  def getNewsNewsKeywordDictCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_KEYWORD_DICT)
  }


  def getSougouRelateWordsCollection: DBCollection = {
    getCollection(MongoTableConstants.SOUGOU_RELATE_WORDS)
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

  def findCompanyCode(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("_id", Integer.valueOf(1))
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
    projection.put("_id", Integer.valueOf(1))
    projection.put("code", Integer.valueOf(1))
    projection.put("w", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getGraphTopicConpCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  //topic->tipic_hot
  def findTopicHot(loadAll: Boolean = true): java.util.List[DBObject] = {
    val data = new Date()
    val projection = new BasicDBObject()
    projection.put("topic_hot", Integer.valueOf(1))
    val query = new BasicDBObject()
    var dbCurson: DBCursor = null
    if (!loadAll) {
      val contitionDB = new BasicDBObject()
       contitionDB.append("$gte", Util.dataFomatStringYMD(new Date()))
      //contitionDB.append("$gte", "20160912")
      query.put("date",contitionDB)
    }
    dbCurson = getTopicHotCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }

  def findEvent(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("_id", Integer.valueOf(1))
    //event name
    projection.put("szh", Integer.valueOf(1))
    //event rule
    projection.put("rule", Integer.valueOf(1))
    //event weight
    projection.put("w", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getDictNewsRuleCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  def findIndustry(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("_id", Integer.valueOf(1))
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

  def saveOrUpdate(docs: java.util.List[BasicDBObject]) = {
    try {
      docs.foreach(getCollection.save(_))
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

  def findByKeyword(keyword: String): java.util.Map[_, _] = {
    try {
      val dbResult = getCollection().findOne(new BasicDBObject("keyword", keyword))
      if (dbResult == null) return null
      else dbResult.toMap
    } catch {
      case e: Exception =>
        null
    }
  }


  def getDicFromNewsEntityMultiRel(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("word", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getNewsEntityMultiRelCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  def getDicFromNewsKeywordDict(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("word", Integer.valueOf(1))
    projection.put("syno", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getNewsNewsKeywordDictCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  def getDicFromSougouRelateWords(): java.util.List[DBObject] = {
    val projection = new BasicDBObject()
    projection.put("taskUrl", Integer.valueOf(1))
    projection.put("words", Integer.valueOf(1))
    val query = new BasicDBObject()
    val dbCurson = getSougouRelateWordsCollection.find(query, projection)
    if (dbCurson == null) return null.asInstanceOf[java.util.List[DBObject]]
    val result = dbCurson.toArray
    result
  }


  def getSynonmDicFromNewsKeywordDict(): java.util.Map[String, java.util.Set[String]] = {
    val map = new java.util.HashMap[String, java.util.Set[String]]()
    val dm = new DataManager(new EsClientConf())
    val dics: java.util.List[DBObject] = dm.getDicFromNewsKeywordDict()
    dics.foreach { dic =>
      var synso = dic.get("syno")
      if (synso != null) {
        val syns = synso.asInstanceOf[BasicDBList]
        if (syns.size() > 0) {
          val word = if (dic.get("word") != null) dic.get("word").toString.trim else null
          if (word != null && !word.equalsIgnoreCase("")) {
            if (!map.containsKey(word)) {
              val set = new util.HashSet[String]()
              syns.foreach { s =>
                val syName = s.toString.trim
                if (!syName.equalsIgnoreCase(word) && !word.contains(syName) && !syName.contains(word))
                  set.add(syName)
              }
              if (set.size() > 0)
                map.put(word, set)
            } else {
              val set = new util.HashSet[String]()
              syns.foreach { s =>
                val syName = s.toString.trim
                if (!syName.equalsIgnoreCase(word) && !word.contains(syName) && !syName.contains(word))
                  set.add(syName)
              }
              if (set.size() > 0)
                map.get(word).addAll(set)
            }
          }

        }
      }

    }
    map
  }


  def getSynonmDicFromSynonymWords(): java.util.Map[String, java.util.Set[String]] = {
    val map = new java.util.HashMap[String, java.util.Set[String]]()
    val dm = new DataManager(new EsClientConf())
    val dics: java.util.List[DBObject] = dm.getDicFromSougouRelateWords()
    dics.foreach { dic =>
      var synso = dic.get("words")
      if (synso != null) {
        val syns = synso.asInstanceOf[BasicDBList]
        if (syns.size() > 0) {
          var word = if (dic.get("taskUrl") != null) dic.get("taskUrl").toString.trim else null
          if (word != null && !word.equalsIgnoreCase("")) {
            val regexWord = Util.regexExtract(word, "[\\s|\\S]*query=([\\w|\\u4e00-\\u9fa5]+)&[\\s|\\S]*", -1)
            if (regexWord != null && !regexWord.toString.trim.equalsIgnoreCase("")) {
              word = regexWord.toString.trim
              if (!map.containsKey(word)) {
                val set = new util.HashSet[String]()
                syns.foreach { s =>
                  val sWordDBObj = s.asInstanceOf[BasicDBObject]
                  val syName = sWordDBObj.getString("word").trim
                  if (!syName.equalsIgnoreCase(word) && !word.contains(syName) && !syName.contains(word))
                    set.add(syName)
                }
                if (set.size() > 0)
                  map.put(word, set)
              } else {
                val set = new util.HashSet[String]()
                syns.foreach { s =>
                  val sWordDBObj = s.asInstanceOf[BasicDBObject]
                  val syName = sWordDBObj.getString("word").trim
                  if (!syName.equalsIgnoreCase(word) && !word.contains(syName) && !syName.contains(word))
                    set.add(syName)
                }
                if (set.size() > 0)
                  map.get(word).addAll(set)
              }
            }
          }

        }
      }

    }
    map
  }

}

private[search] object DataManager {


  def main(args: Array[String]) {
    //testLoadGraphNodeDataToMongo
    //testQueryOneByKeyWord
    //testFindBaseStock
    //testFindByKeyword
    //testSaveOrUpdate
    //testGetDicFromNewsKeywordDict()
    //testGetSynonmDicFromSynonymWords()
    testFindTopicHot()
  }

  def testFindTopicHot() = {
    val dm = new DataManager(new EsClientConf())
    val result = dm.findTopicHot(false)
    println(result)
  }

  def testGetSynonmDicFromSynonymWords() = {
    val dm = new DataManager(new EsClientConf())
    dm.getSynonmDicFromSynonymWords()
  }

  def testGetDicFromNewsKeywordDict() = {
    val map = new java.util.HashMap[String, java.util.Set[String]]()
    val dm = new DataManager(new EsClientConf())
    val dics: java.util.List[DBObject] = dm.getDicFromNewsKeywordDict()
    dics.foreach { dic =>
      var synso = dic.get("syno")
      if (synso != null) {
        val syns = synso.asInstanceOf[BasicDBList]
        if (syns.size() > 0) {
          val word = if (dic.get("word") != null) dic.get("word").toString.trim else null
          if (word != null && !word.equalsIgnoreCase("")) {
            if (!map.containsKey(word)) {
              val set = new util.HashSet[String]()
              syns.foreach { s =>
                val syName = s.toString.trim
                if (!syName.equalsIgnoreCase(word) && !word.contains(syName) && !syName.contains(word))
                  set.add(syName)
              }
              if (set.size() > 0)
                map.put(word, set)
            } else {
              val set = new util.HashSet[String]()
              syns.foreach { s =>
                val syName = s.toString.trim
                if (!syName.equalsIgnoreCase(word) && !word.contains(syName) && !syName.contains(word))
                  set.add(syName)
              }
              if (set.size() > 0)
                map.get(word).addAll(set)
            }
          }

        }
      }

    }
    println(map)
  }

  def testSaveOrUpdate() = {
    val dm = new DataManager(new EsClientConf())
    val docs = new java.util.ArrayList[BasicDBObject]()
    var dbObject = new BasicDBObject()
    dbObject.append("_id", 0)
    dbObject.append("keyword", "haha")
    docs.add(dbObject)
    dbObject = new BasicDBObject()
    dbObject.append("_id", 1)
    dbObject.append("keyword", "haha1")
    docs.add(dbObject)
    val result = dm.saveOrUpdate(docs)
    println(result)
  }

  def testFindByKeyword() = {
    val dm = new DataManager(new EsClientConf())
    val result = dm.findByKeyword("中科创达")
    println(result)
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



