package search.es.client

import java.util.concurrent.TimeUnit

import com.mongodb.{BasicDBObject, DBObject, DBCursor}
import org.elasticsearch.client.Client
import search.common.cache.impl.LocalCache
import search.common.config.{RedisConfiguration, EsConfiguration}
import search.common.entity.bizesinterface.IndexObjEntity
import search.common.util.{Util, Logging}
import search.es.client.util.EsClientConf
import scala.collection.JavaConversions._
import scala.collection.JavaConverters.asJavaListConverter


/**
  * Created by soledede.weng on 2016/7/27.
  */
private[search] class DefaultEsClientImpl(conf: EsClientConf) extends EsClient with Logging with EsConfiguration with RedisConfiguration {

  val keyPreffix = s"$nameSpace:"

  val keywordField = "keyword"

  var coreThreadsNumber = consumerCoreThreadsNum


  var currentThreadsNum = Util.inferCores() * coreThreadsNumber


  if (consumerThreadsNum > 0) currentThreadsNum = consumerThreadsNum
  val indexRunnerThreadPool = Util.newDaemonFixedThreadPool(currentThreadsNum, "index_runner_thread_excutor")


  override def count(indexName: String, typeName: String): Long = {
    val cnt = EsClient.count(EsClient.getClientFromPool(), indexName, typeName)._1
    cnt
  }


  override def createIndex(indexName: String, indexAliases: String, typeName: String): Boolean = {
    EsClient.createIndexTypeMapping(EsClient.getClientFromPool(), indexName, indexAliases, number_of_shards, number_of_replicas, typeName, null)
  }

  override def indexExists(indexName: String): Boolean = {
    EsClient.indexExists(EsClient.getClientFromPool(), indexName)
  }


  override def totalIndexRun(indexName: String, typeName: String): Unit = {
    val manager = conf.mongoDataManager
    val totalCnt = manager.count()
    var pageNum = if (totalCnt % pageSize == 0) totalCnt / pageSize else (totalCnt / pageSize) + 1
    val lastPageSize = totalCnt % pageSize
    logDebug(s"Total Count:${totalCnt}\t Total Page: ${pageNum}\t PageSize:${pageSize}")
    var actualPageSize = pageSize
    if (pageSize >= totalCnt) actualPageSize = totalCnt
    for (i <- 0 until pageNum) {
      val start = i * actualPageSize
      if (lastPageSize != 0 && i == pageNum - 1) {
        actualPageSize = lastPageSize
      }
      logDebug(s"Current Page Number: ${i}")

      indexRunnerThreadPool.execute(new EsIndexRunner(indexName, typeName, conf, start, actualPageSize))
      // Thread.sleep(200)
    }

  }


  override def bulkIndexRun(indexName: String, typeName: String, startDate: Long, endDate: Long): Unit = ???


  override def incrementIndexOne(indexName: String, typeName: String, data: String): Boolean = {
    val dataList = new java.util.ArrayList[String]()
    dataList.add(data)
    incrementIndex(indexName, typeName, dataList)
  }

  override def incrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean = {
    incrementIndexWithRw(indexName, typeName, data.map(new IndexObjEntity(_, null.asInstanceOf[java.util.List[String]])))
  }

  override def incrementIndexWithRw(indexName: String, typeName: String, data: java.util.Collection[IndexObjEntity]): Boolean = {
    val docs = new java.util.ArrayList[java.util.Map[String, Object]]
    var list = new java.util.ArrayList[BasicDBObject]()
    var cnt = conf.mongoDataManager.count()
    var logType = "added"
    var indexId = cnt
    data.foreach { k =>
      val keyword = k.getKeyword
      val rvKw = k.getRvkw
      //val doc = conf.mongoDataManager.queryOneByKeyWord(keyword)
      val doc = conf.mongoDataManager.findAndRemove(keyword)
      if (doc == null) {
        cnt += 1
        indexId = cnt
      } else {
        indexId = doc.get("_id").toString.toInt
        logType = "updated"
      }

      val dbObject = new BasicDBObject()
      val currentTime = System.currentTimeMillis()
      dbObject.append("_id", indexId)
      dbObject.append("keyword", keyword)
      if (rvKw != null && rvKw.size() > 0)
        dbObject.append("relevant_kws", rvKw)
      dbObject.append("updateDate", currentTime)




      val newDoc = new java.util.HashMap[String, Object]()
      newDoc.put("_id", indexId)
      newDoc.put("keyword", keyword)
      if (rvKw != null && rvKw.size() > 0)
        newDoc.put("relevant_kws", rvKw)
      newDoc.put("updateDate", java.lang.Long.valueOf(currentTime))


      //base stock
      if (LocalCache.baseStockCache.contains(keyword)) {
        val baseStock = LocalCache.baseStockCache(keyword)
        val company = baseStock.getCompany
        val comEn = baseStock.getComEn
        val comSim = baseStock.getComSim
        val comCode = baseStock.getComCode
        dbObject.append("s_com", company)
        dbObject.append("s_en", comEn)
        dbObject.append("s_zh", comSim)
        dbObject.append("stock_code", comCode)

        newDoc.put("s_com", company)
        newDoc.put("s_en", comEn)
        newDoc.put("s_zh", comSim)
        newDoc.put("stock_code", comCode)

      }

      //word2vec
      val similarityWords: java.util.Collection[String] = conf.similarityCaculate.word2Vec(keyword)

      if (similarityWords != null && similarityWords.size > 0) {
        newDoc.put("word2vec", similarityWords)
      }

      list.add(dbObject)


      docs.add(newDoc)

      if (rvKw != null && rvKw.size() > 0) {
        logInfo(s"$logType index,keyword:$keyword -> relevant keywords:${rvKw.mkString(",")}")
      } else {
        logInfo(s"$logType index,keyword:$keyword")
      }

    }

    if (list.size() > 0) conf.mongoDataManager.insert(list)

    if (docs.size() == 1) {
      addDocument(indexName, typeName, docs.head)
    } else if (docs.size() > 1) {
      addDocuments(indexName, typeName, docs)
    } else false

  }


  override def decrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean = {
    val m = conf.mongoDataManager
    var cnt = 0
    var delType = "deleted"
    data.foreach { k =>
      val doc = m.findAndRemove(k)
      if (doc == null) {
        if (EsClient.delByKeyword(EsClient.getClientFromPool(), indexName, typeName, keywordField, k)) {
          cnt += 1
          logInfo(s"$delType  keyword: $k from index,delByKeyword")
        }
        /* delType = "indexed"
         //add document to index
         if (incrementIndexOne(indexName, typeName, k)) cnt += 1*/
      } else {
        val id = doc.get("_id").toString
        //delete document from index by id
        val delBool = EsClient.delIndexById(EsClient.getClientFromPool(), indexName, typeName, id)
        if (delBool) {
          cnt += 1
          logInfo(s"$delType  keyword: $k from index,id: $id")
        }
      }
    }
    val size = data.size()
    if (size != 0 && size == cnt) true
    else false
  }

  override def addDocument(indexName: String, typeName: String, doc: java.util.Map[String, Object]): Boolean = {
    val id = EsClient.postDocument(EsClient.getClientFromPool(), indexName, typeName, doc)
    if (null != id && !id.trim.equals("")) true
    else false
  }


  override def addDocuments(indexName: String, typeName: String, docs: java.util.List[java.util.Map[String, Object]]): Boolean = {
    EsClient.bulkPostDocument(EsClient.getClientFromPool(), indexName, typeName, docs)
  }

  override def deleteIndex(indexName: String): Boolean = {
    EsClient.deletIndex(EsClient.getClientFromPool(), indexName)
  }


  override def delAllData(indexName: String, typeName: String): Boolean = {
    EsClient.delAllData(EsClient.getClientFromPool(), indexName, typeName)
  }


  override def multiMatchQuery(indexName: String, typeName: String, from: Int, to: Int, keyWords: Object, fields: String*): Array[java.util.Map[String, Object]] = {
    EsClient.multiMatchQuery(EsClient.getClientFromPool(), indexName, typeName, from, to, keyWords, fields: _*)
  }

  override def matchQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.matchQuery(EsClient.getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }

  override def matchAllQueryWithCount(indexName: String, typeName: String, from: Int, to: Int): (Long, Array[java.util.Map[String, Object]]) = {
    EsClient.matchAllQueryWithCount(EsClient.getClientFromPool(), indexName, typeName, from, to)
  }

  override def termQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.termQuery(EsClient.getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }

  override def commonTermQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.commonTermQuery(EsClient.getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }

  override def boolMustQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.boolMustQuery(EsClient.getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }


}

private[search] class EsIndexRunner(indexName: String, typeName: String, conf: EsClientConf, start: Int, rows: Int) extends Runnable with Logging {

  override def run(): Unit = {
    process(start, rows, indexName, typeName)
  }

  def process(start: Int, rows: Int, indexName: String, typeName: String): Unit = {
    logDebug(s"start=${start}--rows=${rows}")
    val cusor = conf.mongoDataManager.queryByPage(start, rows)
    var docs = new java.util.ArrayList[java.util.Map[String, Object]]()
    for (obj <- cusor) {
      val doc = obj.toMap
      docs.add(doc.asInstanceOf[java.util.Map[String, Object]])
      /*if (bulkCommitSize != -1 && docs.size() == bulkCommitSize) {
        bulkPostDocument(getClientFromPool(), indexName, typeName, docs)
        logDebug(s"post document ${docs.size()} successful!")
        docs = new java.util.ArrayList[java.util.Map[String, Object]]()
      }*/
      //postDocument(getClientFromPool(), indexName, typeName,doc.asInstanceOf[java.util.Map[String,Object]])
    }
    if (docs.size() > 0) {
      EsClient.bulkPostDocument(EsClient.getClientFromPool(), indexName, typeName, docs)
      logDebug(s"post document ${docs.size()} successful!")
    }
  }
}


object DefaultEsClientImpl {


  def main(args: Array[String]) {
    testAddDocuments
    //testFor
  }

  def testFor() = {
    for (i <- 0 until 4) {
      println(i)
    }
  }

  def testAddDocuments() = {
    val indexName = "nlp"
    val typeName = "graph"
    val conf = new EsClientConf()
    conf.init()
    val client = new DefaultEsClientImpl(conf)
    val docs = new java.util.ArrayList[java.util.Map[String, Object]]()
    val doc1 = new java.util.HashMap[String, Object]()
    doc1.put("keyword", "东方证券A")
    doc1.put("_id", 10.toString)
    docs.add(doc1)
    val doc2 = new java.util.HashMap[String, Object]()
    doc2.put("keyword", "东方证券B")
    doc2.put("_id", 11.toString)
    docs.add(doc2)
    if (client.addDocuments(indexName, typeName, docs)) {
      println("insert bulk index successfully")
    } else println("failed")
  }
}
