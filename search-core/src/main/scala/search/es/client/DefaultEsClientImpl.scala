package search.es.client

import java.util
import java.util.concurrent.TimeUnit

import com.mongodb.{BasicDBObject, DBObject, DBCursor}
import org.elasticsearch.client.Client
import search.common.config.{RedisConfiguration, EsConfiguration}
import search.common.entity.bizesinterface.IndexObjEntity
import search.common.util.{Util, Logging}
import search.es.client.EsClient._
import search.es.client.util.EsClientConf
import scala.collection.JavaConversions._


/**
  * Created by soledede.weng on 2016/7/27.
  */
private[search] class DefaultEsClientImpl(conf: EsClientConf) extends EsClient with Logging with EsConfiguration with RedisConfiguration {

  val keyPreffix = s"$nameSpace:"

  var coreThreadsNumber = consumerCoreThreadsNum


  var currentThreadsNum = Util.inferCores() * coreThreadsNumber


  if (consumerThreadsNum > 0) currentThreadsNum = consumerThreadsNum
  val indexRunnerThreadPool = Util.newDaemonFixedThreadPool(currentThreadsNum, "index_runner_thread_excutor")

  import search.es.client.EsClient._

  override def createIndex(indexName: String, indexAliases: String, typeName: String): Boolean = {
    createIndexTypeMapping(getClientFromPool(), indexName, indexAliases, number_of_shards, number_of_replicas, typeName, null)
  }

  override def indexExists(indexName: String): Boolean = {
    EsClient.indexExists(getClientFromPool(), indexName)
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
    val docs = new java.util.ArrayList[java.util.Map[String, Object]]
    var list = new java.util.ArrayList[BasicDBObject]()
    var cnt = conf.mongoDataManager.count()
    data.foreach { k =>
      val doc = conf.mongoDataManager.queryOneByKeyWord(k)
      if (doc == null) {
        cnt += 1
        val dbObject = new BasicDBObject()
        val currentTime = System.currentTimeMillis()
        dbObject.append("_id", cnt)
        dbObject.append("keyword", k)
        dbObject.append("updateDate", currentTime)
        list.add(dbObject)

        val newDoc = new java.util.HashMap[String, Object]()
        newDoc.put("_id", cnt)
        newDoc.put("keyword", k)
        newDoc.put("updateDate", java.lang.Long.valueOf(currentTime))
        docs.add(newDoc)


      }
    }

    if (list.size() > 0) conf.mongoDataManager.getCollection.insert(list)

    if (docs.size() > 0) {
      addDocuments(indexName, typeName, docs)
    } else false

  }

  override def incrementIndexWithRw(indexName: String, typeName: String, data: java.util.Collection[IndexObjEntity]): Boolean = {
    val docs = new java.util.ArrayList[java.util.Map[String, Object]]
    var list = new java.util.ArrayList[BasicDBObject]()
    var cnt = conf.mongoDataManager.count()
    data.foreach { k =>
      val keyword = k.getKeyword
      val rvKw = k.getRvkw
      val doc = conf.mongoDataManager.queryOneByKeyWord(keyword)
      if (doc == null) {
        cnt += 1
        val dbObject = new BasicDBObject()
        val currentTime = System.currentTimeMillis()
        dbObject.append("_id", cnt)
        dbObject.append("keyword", keyword)
        if (rvKw != null)
          dbObject.append("relevant_kws", rvKw)
        dbObject.append("updateDate", currentTime)
        list.add(dbObject)

        val newDoc = new java.util.HashMap[String, Object]()
        newDoc.put("_id", cnt)
        newDoc.put("keyword", keyword)
        if (rvKw != null)
          newDoc.put("relevant_kws", rvKw)
        newDoc.put("updateDate", java.lang.Long.valueOf(currentTime))
        docs.add(newDoc)
      }
    }

    if (list.size() > 0) conf.mongoDataManager.getCollection.insert(list)

    if (docs.size() > 0) {
      addDocuments(indexName, typeName, docs)
    } else false

  }


  override def decrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean = {
    val m = conf.mongoDataManager
    var cnt = 0
    data.foreach { k =>
      val doc = m.findAndRemove(k)
      if (doc == null) {
        //add document to index
        if (incrementIndexOne(indexName, typeName, k)) cnt += 1
      } else {
        val id = doc.get("_id").toString
        //delete document from index by id
        val delBool = delIndexById(getClientFromPool(), indexName, typeName, id)
        if (delBool) cnt += 1
      }
    }
    val size = data.size()
    if (size != 0 && size == cnt) true
    else false
  }

  override def addDocument(indexName: String, typeName: String, doc: java.util.Map[String, Object]): Boolean = {
    val id = postDocument(getClientFromPool(), indexName, typeName, doc)
    if (null != id && !id.trim.equals("")) true
    else false
  }


  override def addDocuments(indexName: String, typeName: String, docs: java.util.List[java.util.Map[String, Object]]): Boolean = {
    try {
      bulkPostDocument(getClientFromPool(), indexName, typeName, docs)
      true
    } catch {
      case e: Exception => logError("add documents faield!", e)
        false
    }
  }

  override def deleteIndex(indexName: String): Boolean = {
    EsClient.deletIndex(getClientFromPool(), indexName)
  }


  override def delAllData(indexName: String, typeName: String): Boolean = {
    EsClient.delAllData(getClientFromPool(), indexName, typeName)
  }

  override def matchQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.matchQuery(getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }


  override def termQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.termQuery(getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }

  override def commonTermQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.commonTermQuery(getClientFromPool(), indexName, typeName, from, to, field, keyWords)
  }

  override def boolMustQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    EsClient.boolMustQuery(getClientFromPool(), indexName, typeName, from, to, field, keyWords)
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
      bulkPostDocument(getClientFromPool(), indexName, typeName, docs)

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
