package search.es.client


import com.alibaba.fastjson.JSON
import org.elasticsearch.index.query.QueryBuilders
import org.junit.Test
import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2016/8/12.
  */
object EsClientTest {
  val indexName = "es-cloud"
  val typeName = "test"
  val client = EsClient.getClientFromPool()

  @Test
  def testIndexDoc() = {
    // 创建索引
    val kvMap = new java.util.HashMap[String, Object]()
    kvMap.put("name", "soledede");
    kvMap.put("gender", "m");
    EsClient.indexDoc(client, indexName, typeName, kvMap);

    // 创建索引
    val json = "{\"id\":\"55\",\"bb\":\"bbs\"}"
    EsClient.indexDoc(client, indexName, typeName, json);


    Thread.sleep(3000)

    // 查询结果
    val queryBuilder = QueryBuilders.matchAllQuery()
    val kvList = EsClient.search(client, indexName, typeName, queryBuilder, null)
    for (kv <- kvList) {
      println(JSON.toJSONString(kv,false))
    }
  }


  @Test
  def testBatchIndexDocsForJson() = {
    val json1 = "{\"id\":\"1\",\"a\":\"a1\",\"a2\":\"a4\"}";
    val json2 = "{\"id\":\"2\",\"b\":\"b2\",\"b4\":\"b6\"}";

    val jsonList = new java.util.ArrayList[String]()
    jsonList.add(json1);
    jsonList.add(json2);

    // 创建索引
    EsClient.batchIndexDocsForJson(client, indexName, typeName, jsonList);

    Thread.sleep(3000);

    // 查询结果
    val queryBuilder = QueryBuilders.matchAllQuery()
    val kvList = EsClient.search(client, indexName, typeName, queryBuilder, null)
    for (kv <- kvList) {
      System.out.println(JSON.toJSONString(kv,false));
    }
  }


  @Test
  def testBatchIndexDocsForMap() = {
    val kvList = new java.util.ArrayList[java.util.Map[String, Object]]()

    val kvMap = new java.util.HashMap[String, Object]();
    kvMap.put("id", "3");
    kvMap.put("name", "soledede");
    kvMap.put("gender", "m");
    kvList.add(kvMap)

    val kvMap2 = new java.util.HashMap[String, Object]();
    kvMap2.put("id", "2");
    kvMap2.put("name", "sdwe");
    kvMap2.put("gender", "f");
    kvList.add(kvMap2)

    // 创建索引
    EsClient.batchIndexDocsForMap(client, indexName, typeName, kvList)

    Thread.sleep(3000);

    // 查询结果
    val queryBuilder = QueryBuilders.matchAllQuery();
    val kvLists = EsClient.search(client, indexName, typeName, queryBuilder, null)
    for (kv <- kvLists) {
      println(JSON.toJSONString(kv,false));
    }

  }


  @Test
  def testSearch() = {

    val queryBuilder = QueryBuilders.termQuery("gender", "m");
    val filterMap = new java.util.HashMap[String, Object]()
    filterMap.put("name", "soledede")

    val kvList = EsClient.search(client, indexName, typeName, queryBuilder, filterMap)
    for (kv <- kvList) {
      println(JSON.toJSONString(kv,false))
    }

  }


  @Test
  def testDeleteDoc() = {

    EsClient.deleteDoc(client, indexName, typeName, "1");

    Thread.sleep(3000);

    // 查询结果
    val queryBuilder = QueryBuilders.matchAllQuery();
    val kvLists = EsClient.search(client, indexName, typeName, queryBuilder, null)
    for (kv <- kvLists) {
      println(JSON.toJSONString(kv,false));
    }

  }


  @Test
  def testDeleteDocs() = {
    // 删除文档
    var queryBuilder = QueryBuilders.termQuery("name", "soledede");
    EsClient.deleteDocsByQuery(client, indexName, typeName, queryBuilder);
    // 查询结果
    val queryBuilder1 = QueryBuilders.matchAllQuery();
    val kvLists = EsClient.search(client, indexName, typeName, queryBuilder1, null)
    for (kv <- kvLists) {
      println(JSON.toJSONString(kv,false));
    }
  }

}
