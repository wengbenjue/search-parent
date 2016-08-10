package search.es.client

import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.google.common.collect.Maps
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.ActionFuture
import org.elasticsearch.action.admin.indices.alias.Alias
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.bulk.{BulkResponse, BulkRequest}
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.deletebyquery.{DeleteByQueryAction, DeleteByQueryRequestBuilder}
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.{SearchType, SearchResponse}
import org.elasticsearch.action.update.{UpdateResponse, UpdateRequestBuilder}
import org.elasticsearch.client.{Requests, Client}
import org.elasticsearch.client.transport.{NoNodeAvailableException, TransportClient}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import org.elasticsearch.index.query._
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin
import org.elasticsearch.search.SearchHit
import search.common.config.EsConfiguration
import search.common.entity.bizesinterface.IndexObjEntity
import search.common.util.Logging
import scala.collection.JavaConversions._

import scala.util.Random

/**
  * Created by soledede.weng on 2016/7/26.
  */
private[search] trait EsClient extends EsConfiguration {

  def createIndex(indexName: String, indexAliases: String, typeName: String): Boolean

  def indexExists(indexName: String): Boolean

  def deleteIndex(indexName: String): Boolean

  def totalIndexRun(indexName: String, typeName: String): Unit

  def bulkIndexRun(indexName: String, typeName: String, startDate: Long, endDate: Long): Unit

  def incrementIndexOne(indexName: String, typeName: String, data: String): Boolean

  def incrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean

  def incrementIndexWithRw(indexName: String, typeName: String, data: java.util.Collection[IndexObjEntity]): Boolean

  def decrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean

  def addDocument(indexName: String, typeName: String, doc: java.util.Map[String, Object]): Boolean

  def addDocuments(indexName: String, typeName: String, docs: java.util.List[java.util.Map[String, Object]]): Boolean

  def matchQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def termQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def commonTermQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def delAllData(indexName: String, typeName: String): Boolean

  def boolMustQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

}

private[search] object EsClient extends EsConfiguration with Logging {


  private val cnt = new AtomicInteger()
  private val esClientPool = new Array[Client](esClients)

  val settings = Settings.settingsBuilder()
    .put("client.transport.sniff", true)
    .put("number_of_shards", number_of_shards)
    .put("number_of_replicas", number_of_replicas)
    .put("cluster.name", esClusterName)
    .build()

  def getClient(clientType: String = "transport"): Client = {
    clientType match {
      case "transport" =>
        val client = TransportClient.builder().settings(settings)
          .addPlugin(classOf[DeleteByQueryPlugin])
          .build()
        val nodes = esHosts.split(",")
        nodes.foreach { node =>
          if (node.trim.length > 0) {
            val hostPort = node.split(":")
            client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostPort(0)), hostPort(1).toInt))
            logInfo(s"client: host:${hostPort(0)},port:${hostPort(1)}")
          }
        }
        client
      case _ => null
    }
  }

  def initClientPool() = {
    for (i <- 0 until esClients) {
      esClientPool(i) = getClient()
    }
  }

  def getClientFromPool(): Client = {
    esClientPool(cnt.getAndSet((cnt.get() + 1) % esClientPool.length))
  }


  def close(client: Client) = {
    client.close()
  }

  /**
    * whether index exists
    *
    * @param client
    * @param indexName
    * @return
    */
  def indexExists(client: Client, indexName: String): Boolean = {
    try {
      val response: ActionFuture[IndicesExistsResponse] = client.admin.indices.exists(Requests.indicesExistsRequest(indexName))
      response.actionGet.isExists
    } catch {
      case e: Exception =>
        logError("index exists check faield!", e)
        false
    }
  }

  /**
    * delete the index
    *
    * @param client
    * @param indexName
    */
  def deletIndex(client: Client, indexName: String): Boolean = {
    val response: ActionFuture[DeleteIndexResponse] = client.admin.indices.delete(Requests.deleteIndexRequest(indexName))
    response.actionGet.isAcknowledged
  }

  def delIndexById(client: Client, indexName: String, typeName: String, id: String): Boolean = {
    //val response: ActionFuture[DeleteResponse] = client.delete(Requests.deleteRequest(indexName).`type`(typeName).id(id))
    val response = client.prepareDelete(indexName, typeName, id).execute()
    response.get.isFound
  }

  def delAllData(client: Client, indexName: String, typeName: String): Boolean = {

    try {
      val response = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE)
        .setIndices(indexName)
        .setTypes(typeName)
        .setQuery(QueryBuilders.matchAllQuery())
        .execute().actionGet()
      true
    } catch {
      case e: Exception =>
        logError(s"delete index ${indexName} with type ${typeName} failed", e)
        false
    }

  }

  def createIndexTypeMapping(client: Client, indexName: String, indexAlias: String, numShards: Int, numReplicas: Int, typeName: String, builderMapping: XContentBuilder): Boolean = {
    try {
      var builder_mapping: XContentBuilder = builderMapping
      if (builderMapping == null) {

        /**
          * curl -XPOST http://192.168.100.11:9200/nlp/graph/_mapping -d'
          * {
          * "graph": {
          * "_all": {
          * "analyzer": "ik",
          * "search_analyzer": "ik",
          * "term_vector": "no",
          * "store": "false"
          * },
          * "properties": {
          * "keyword": {
          * "type": "string",
          * "store": "no",
          * "term_vector": "with_positions_offsets",
          * "analyzer": "ik",
          * "search_analyzer": "ik",
          * "include_in_all": "true",
          * "doc_values":false,
          * "fielddata":{"format":"disabled"}
          * }
          * }
          * }
          * }'
          */
        builder_mapping = XContentFactory.jsonBuilder
          .startObject
          .startObject(typeName)
          .startObject("_all")
          .field("analyzer", "ik")
          .field("search_analyzer", "ik")
          .field("term_vector", "no")
          .field("store", "false")
          .endObject()
          .startObject("properties")
          .startObject("keyword")
          .field("type", "string")
          .field("store", "no")
          .field("term_vector", "with_positions_offsets")
          .field("analyzer", "ik")
          .field("search_analyzer", "ik")
          .field("include_in_all", "true")
          .field("doc_values", "false")
          .startObject("fielddata")
          .field("format", "disabled")
          .endObject()
          .endObject
      }
      val settings: java.util.Map[String, String] = Maps.newHashMap[String, String]()
      settings.put("number_of_shards", numShards.toString)
      settings.put("number_of_replicas", numReplicas.toString)
      val response: ActionFuture[CreateIndexResponse] = client.admin.indices.create(Requests.createIndexRequest(indexName).settings(settings).alias(new Alias(indexAlias)).mapping(typeName, builder_mapping))
      response.actionGet.isAcknowledged
    } catch {
      case e: Exception =>
        logError("create mapping failed!", e)
        false
    }
  }

  /**
    *
    * add one document to `type` in `index`
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param doc
    * @return
    */
  def postDocument(client: Client, indexName: String, typeName: String, doc: java.util.Map[String, Object]): String = {
    try {
      var response: ActionFuture[IndexResponse] = null

      var id = doc.get("id")
      if (id == null) {
        id = doc.get("_id")
      }
      doc.remove("id")
      doc.remove("_id")

      if (id == null)
        response = client.index(Requests.indexRequest(indexName).`type`(typeName).source(doc))
      else response = client.index(Requests.indexRequest(indexName).`type`(typeName).id(id.toString.trim).source(doc))
      response.get().getId
    } catch {
      case e: Exception =>
        logError("post document faield!", e)
        null
    }
  }

  /**
    * add more than on document to index
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param docs
    */
  def bulkPostDocument(client: Client, indexName: String, typeName: String, docs: java.util.List[java.util.Map[String, Object]]): Unit = {

    import scala.collection.JavaConversions._
    try {

      val request: BulkRequest = Requests.bulkRequest
      for (doc <- docs) {
        var id = doc.get("id")
        if (id == null) {
          id = doc.get("_id")
        }
        doc.remove("id")
        doc.remove("_id")
        if (id == null) request.add(Requests.indexRequest(indexName).`type`(typeName).source(doc))
        else request.add(Requests.indexRequest(indexName).`type`(typeName).id(id.toString.trim).source(doc))
      }
      //val response: ActionFuture[BulkResponse] = client.bulk(request)
      val response = client.bulk(request).actionGet()
      if (response.hasFailures) {
        logError(s"build index failed with bulk [${response.buildFailureMessage()}]")
      }
      logDebug(s"build index,size:[${docs.size()}],costPeriod:[${response.getTookInMillis} ms]")

    } catch {
      case e: Exception => logError("bulk post document failed!", e)
    }

  }

  /**
    * update index by document id
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param docId
    * @param doc
    * @return
    */
  def updateIndexById(client: Client, indexName: String, typeName: String, docId: String, doc: java.util.Map[String, Object]): String = {
    val request: UpdateRequestBuilder = client.prepareUpdate(indexName, typeName, docId).setDoc(doc).setDocAsUpsert(true).setFields("_source")
    val response: UpdateResponse = request.get
    response.getGetResult.getId
  }

  /**
    * update more than on docment in index
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param id2Docs
    * @return
    */
  def bulkUpdateIndex(client: Client, indexName: String, typeName: String, id2Docs: java.util.Map[String, java.util.Map[String, Object]]): Int = {
    val request: BulkRequest = Requests.bulkRequest
    id2Docs.foreach { case (id, doc) =>
      request.add(Requests.indexRequest(indexName).`type`(typeName).id(id).source(doc))
    }
    val response: ActionFuture[BulkResponse] = client.bulk(request)
    response.get.getItems.length
  }


  /**
    * search by query
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param from
    * @param to
    * @param qb
    * @return
    */
  def query(client: Client, indexName: String, typeName: String, from: Int, to: Int, qb: QueryBuilder): Array[SearchHit] = {
    try {
      val res: SearchResponse = client.
        prepareSearch(indexName)
        .setTypes(typeName)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(qb).setFrom(from).setSize(to).execute.actionGet
      res.getHits.getHits
    } catch {
      case e: Exception =>
        logError(s"es cliet error:${e.getMessage}", e)
        null
    }
  }


  def queryAsMap(client: Client, indexName: String, typeName: String, from: Int, to: Int, qb: QueryBuilder): Array[java.util.Map[String, Object]] = {
    val hits = query(client, indexName, typeName, from, to, qb)
    if (hits == null || hits.size == 0) return null
    val result = hits.map { hit =>
      val _id = hit.getId
      val _score = java.lang.Float.valueOf(hit.getScore)
      val doc = hit.sourceAsMap()
      doc.put("id", _id)
      doc.put("score", _score)
      doc
    }
    result
  }

  /**
    * search all
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param from
    * @param to
    * @return
    */
  def matchAllQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.matchAllQuery)
  }

  def boolMustQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    val qb: QueryBuilder = QueryBuilders.boolQuery.must(QueryBuilders.termQuery(field, keyWords)).filter(QueryBuilders.termQuery(field, keyWords))
    queryAsMap(client, indexName, typeName, from, to, qb)
  }


  /**
    * query by one field
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param from
    * @param to
    * @param field
    * @param keyWords
    * @return
    */
  def matchQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.matchQuery(field, keyWords))
  }

  /**
    * query by more than one field
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param from
    * @param to
    * @param keyWords
    * @param fields
    * @return
    */
  def multiMatchQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, keyWords: Object, fields: String*): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.multiMatchQuery(keyWords, fields: _*))
  }

  /**
    * common term query
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param from
    * @param to
    * @param field
    * @param keyWords
    * @return
    */
  def commonTermQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.commonTermsQuery(field, keyWords))
  }

  def termQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.termQuery(field, keyWords))
  }

  /**
    * query by range
    *
    * @param client
    * @param indexName
    * @param typeName
    * @param from
    * @param to
    * @param field
    * @param lowerBounder
    * @param upperBounder
    * @return
    */
  def rangeQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, lowerBounder: Object, upperBounder: Object): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.rangeQuery(field).gt(lowerBounder).lt(upperBounder))
  }

  def prefixQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, preffix: String): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.prefixQuery(field, preffix))
  }

  def wildcardQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, wildcard: String): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.wildcardQuery(field, wildcard))
  }

  def regexpQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, regex: String): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.regexpQuery(field, regex))
  }

  def fuzzyQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, fuzzy: String): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.regexpQuery(field, fuzzy))
  }

  def typeQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.typeQuery(typeName))
  }

  def idsQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, ids: String*): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.idsQuery(indexName, typeName).addIds(ids))
  }

  def constantScoreQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object, score: Float): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.constantScoreQuery(QueryBuilders.termQuery(field, keyWords)).boost(score))
  }

}
