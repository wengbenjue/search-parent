package search.es.client

import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.alibaba.fastjson.{JSON, JSONObject}
import com.google.common.collect.Maps
import org.apache.lucene.queryparser.xml.FilterBuilder
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.ActionFuture
import org.elasticsearch.action.admin.indices.alias.Alias
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse
import org.elasticsearch.action.bulk.{BulkRequest, BulkResponse}
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.deletebyquery.{DeleteByQueryAction, DeleteByQueryRequestBuilder}
import org.elasticsearch.action.index.{IndexRequest, IndexResponse}
import org.elasticsearch.action.search.{SearchResponse, SearchType}
import org.elasticsearch.action.update.{UpdateRequestBuilder, UpdateResponse}
import org.elasticsearch.client.{Client, Requests}
import org.elasticsearch.client.transport.{NoNodeAvailableException, TransportClient}
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}
import org.elasticsearch.index.query._
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilder
import org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin
import org.elasticsearch.search.{SearchHit, SearchHits}
import search.common.config.EsConfiguration
import search.common.entity.bizesinterface.IndexObjEntity
import search.common.util.Logging

import scala.collection.JavaConversions._
import scala.reflect.ClassTag


/**
  * Created by soledede.weng on 2016/7/26.
  */
private[search] trait EsClient extends EsConfiguration {

  def count(indexName: String, typeName: String): Long

  def createIndex(indexName: String, indexAliases: String, typeName: String): Boolean

  def indexExists(indexName: String): Boolean

  def deleteIndex(indexName: String): Boolean

  def totalIndexRun(indexName: String, typeName: String): Unit

  def bulkIndexRun(indexName: String, typeName: String, startDate: Long, endDate: Long): Unit

  def incrementIndexOne(indexName: String, typeName: String, data: String): Boolean

  def incrementIndexNlpCat(data: java.util.Collection[IndexObjEntity]): Boolean

  def incrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean

  def incrementIndexWithRw(indexName: String, typeName: String, data: java.util.Collection[IndexObjEntity], typeChoose: String = graphTypName): Boolean

  def decrementIndex(indexName: String, typeName: String, data: java.util.Collection[String]): Boolean

  def addDocument(indexName: String, typeName: String, doc: java.util.Map[String, Object]): Boolean

  def addDocument[T: ClassTag](indexName: String, typeName: String, id: String, doc: T): Boolean

  def addDocuments(indexName: String, typeName: String, docs: java.util.List[java.util.Map[String, Object]]): Boolean


  def matchPhraseQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def matchMultiPhraseQuery(indexName: String, typeName: String, from: Int, to: Int, keyWords: Object, fields: String*): Array[java.util.Map[String, Object]]

  def matchQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def matchAllQueryWithCount(indexName: String, typeName: String, from: Int, to: Int): (Long, Array[java.util.Map[String, Object]])

  def termQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def commonTermQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def delAllData(indexName: String, typeName: String): Boolean

  def multiMatchQuery(indexName: String, typeName: String, from: Int, to: Int, keyWords: Object, fields: String*): Array[java.util.Map[String, Object]]

  def boolMustQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]]

  def indexGraphNlp(indexName: String, typeName: String, data: java.util.Collection[IndexObjEntity], typeChoose: String): Boolean

  def prefixQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, preffix: String): Array[java.util.Map[String, Object]]


  def wildcardQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, wildcard: String): Array[java.util.Map[String, Object]]

  def fuzzyQuery(indexName: String, typeName: String, from: Int, to: Int, field: String, fuzzy: String): Array[java.util.Map[String, Object]]

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

  def count(client: Client, indexName: String, typeName: String) = {
    matchAllQueryWithCount(client, indexName, typeName, 0, 0)
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

  def delByKeyword(client: Client, indexName: String, typeName: String, field: String, keyWord: String): Boolean = {
    try {
      val response = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE)
        .setIndices(indexName)
        .setTypes(typeName)
        .setQuery(QueryBuilders.termQuery(field, keyWord))
        .execute().actionGet()
      true
    } catch {
      case e: Exception =>
        logError(s"delete index ${indexName} with type ${typeName} failed", e)
        false
    }
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
    * @param client
    * @param indexName
    * @param typeName
    * @param bytes
    * @return
    */
  def postDocument(client: Client, indexName: String, typeName: String, id: String, bytes: Array[Byte]): Boolean = {
    try {
      client.prepareIndex(indexName, typeName, id).setSource(bytes).get()
      true
    } catch {
      case e: Exception =>
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
  def bulkPostDocument(client: Client, indexName: String, typeName: String, docs: java.util.List[java.util.Map[String, Object]]): Boolean = {

    import scala.collection.JavaConversions._
    try {
      var cnt = 0
      var request: BulkRequest = Requests.bulkRequest
      for (doc <- docs) {
        cnt += 1
        var id = doc.get("id")
        if (id == null) {
          id = doc.get("_id")
        }
        doc.remove("id")
        doc.remove("_id")
        if (id == null) request.add(Requests.indexRequest(indexName).`type`(typeName).source(doc))
        else request.add(Requests.indexRequest(indexName).`type`(typeName).id(id.toString.trim).source(doc))
        if (cnt > 3000 && cnt % 3000 == 0) {
          bulkPostDocumentSubmit(client, request)
          request = Requests.bulkRequest
          Thread.sleep(1000)
        }
      }
      //val response: ActionFuture[BulkResponse] = client.bulk(request)
      bulkPostDocumentSubmit(client, request)
      return true
    } catch {
      case e: Exception => logError("bulk post document failed!", e)
        return false
    }

  }

  def bulkPostDocumentSubmit(client: Client, request: BulkRequest): Boolean = {
    try {
      val response = client.bulk(request).actionGet()
      if (response.hasFailures) {
        logError(s"build index failed with bulk [${response.buildFailureMessage()}]")
        return false
      }
      logInfo(s"build index succsefully,costPeriod:[${response.getTookInMillis} ms]")
      true
    } catch {
      case e: Exception =>
        false
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
  def query(client: Client, indexName: String, typeName: String, from: Int, to: Int, qb: QueryBuilder): SearchHits = {
    try {
      val res: SearchResponse = client.
        prepareSearch(indexName)
        .setTypes(typeName)
        .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
        .setQuery(qb)
        .setFrom(from).setSize(to).execute.actionGet
      res.getHits
    } catch {
      case e: Exception =>
        logError(s"es cliet error:${e.getMessage}", e)
        null
    }
  }


  def queryAsMap(client: Client, indexName: String, typeName: String, from: Int, to: Int, qb: QueryBuilder): Array[java.util.Map[String, Object]] = {
    val allHits = query(client, indexName, typeName, from, to, qb)
    val hits = allHits.getHits
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

  def queryAsMapWithCount(client: Client, indexName: String, typeName: String, from: Int, to: Int, qb: QueryBuilder): (Long, Array[java.util.Map[String, Object]]) = {
    val allHits = query(client, indexName, typeName, from, to, qb)
    val hits = allHits.getHits
    val cnt = allHits.getTotalHits
    if (hits == null || hits.size == 0) return (cnt, null)
    val result = hits.map { hit =>
      val _id = hit.getId
      val _score = java.lang.Float.valueOf(hit.getScore)
      val doc = hit.sourceAsMap()
      doc.put("id", _id)
      doc.put("score", _score)
      doc
    }
    (cnt, result)
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

  def matchAllQueryWithCount(client: Client, indexName: String, typeName: String, from: Int, to: Int): (Long, Array[java.util.Map[String, Object]]) = {
    queryAsMapWithCount(client, indexName, typeName, from, to, QueryBuilders.matchAllQuery)
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
    queryAsMap(client, indexName, typeName, from, to,
      QueryBuilders.matchQuery(field, keyWords)
    )
  }

  def matchPhraseQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, field: String, keyWords: Object): Array[java.util.Map[String, Object]] = {
    queryAsMap(client, indexName, typeName, from, to,
      QueryBuilders.matchPhraseQuery(field, keyWords)
    )
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
    multiMatchQuery(client, indexName, typeName, from, to, keyWords, "or", 0.3f, "1", minimumShouldMatch = null, "most", fields: _*)
  }

  def multiMatchQuery(client: Client, indexName: String, typeName: String, from: Int, to: Int, keyWords: Object, op: String, tieBreaker: Float, fuzziness: String, minimumShouldMatch: String, queryType: String, fields: String*): Array[java.util.Map[String, Object]] = {
    val multiMatchQuery = QueryBuilders.multiMatchQuery(keyWords, fields: _*)

    if (fuzziness != null && !fuzziness.isEmpty) multiMatchQuery.fuzziness(fuzziness)

    if (minimumShouldMatch != null && !minimumShouldMatch.isEmpty) multiMatchQuery.minimumShouldMatch(minimumShouldMatch)

    var operator: MatchQueryBuilder.Operator = MatchQueryBuilder.Operator.OR
    if (op != null && op.trim.equalsIgnoreCase("and")) {
      operator = MatchQueryBuilder.Operator.AND
    }
    var tieBreakerF = tieBreaker
    if (tieBreaker == null) tieBreakerF = 0.0f

    var searchType: MultiMatchQueryBuilder.Type = MultiMatchQueryBuilder.Type.BEST_FIELDS
    if (queryType != null && (queryType.trim.equalsIgnoreCase("most_fields") || queryType.trim.equalsIgnoreCase("mostFields") || queryType.trim.equalsIgnoreCase("most"))) {
      searchType = MultiMatchQueryBuilder.Type.MOST_FIELDS
    } else if (queryType != null && (queryType.trim.equalsIgnoreCase("cross_fields") || queryType.trim.equalsIgnoreCase("crossFields") ||queryType.trim.equalsIgnoreCase("cross"))) {
      searchType = MultiMatchQueryBuilder.Type.CROSS_FIELDS
    } else if (queryType != null && queryType.trim.equalsIgnoreCase("phrase")) {
      searchType = MultiMatchQueryBuilder.Type.PHRASE
    } else if (queryType != null && (queryType.trim.equalsIgnoreCase("phrase_prefix") || queryType.trim.equalsIgnoreCase("phrasePrefix"))) {
      searchType = MultiMatchQueryBuilder.Type.PHRASE_PREFIX
    }

    queryAsMap(client, indexName, typeName, from, to,
      multiMatchQuery
        .operator(operator)
        .tieBreaker(tieBreakerF)
        .`type`(searchType)
    )
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

  def disMaxQuery(client: Client, indexName: String, typeName: String, query: String, from: Int, to: Int, boost: Float = 1.0f, tieBreaker: Float = 0.3f, fields: Seq[String]): Array[java.util.Map[String, Object]] = {
    val disMaxQuery = QueryBuilders.disMaxQuery() //just use best field score
    fields.foreach(f => disMaxQuery.add(QueryBuilders.termQuery(f, query)))
    queryAsMap(client, indexName, typeName, from, to, disMaxQuery.boost(boost)
      .tieBreaker(tieBreaker) //other fields(expect best field that the score is highest) multiply this weight and compound to the total score
    )
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
    queryAsMap(client, indexName, typeName, from, to, QueryBuilders.fuzzyQuery(field, fuzzy)
      .prefixLength(6))
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


  /**
    * 为一份文档建立索引
    *
    * @param indexName 索引名，相当于关系型数据库的库名
    * @param typeName  文档类型，相当于关系型数据库的表名
    * @param json      json格式的数据集，必须含有属性"id"
    * @return
    */
  @throws(classOf[Exception])
  def indexDoc(client: Client, indexName: String, typeName: String, json: String): IndexResponse = {
    val kvMap = JSON.parseObject(json)
    indexDoc(client, indexName, typeName, kvMap)
  }


  /**
    * 为一份文档建立索引
    *
    * @param indexName 索引名，相当于关系型数据库的库名
    * @param typeName  文档类型，相当于关系型数据库的表名
    * @param kvMap     键值对形式的数据集，map中必须有属性key: "id"
    * @return
    */
  @throws(classOf[Exception])
  def indexDoc(client: Client, indexName: String, typeName: String, kvMap: java.util.Map[String, Object]): IndexResponse = {
    if (!kvMap.containsKey("id")) {
      throw new Exception("创建索引时，传入的map或json串中没有属性'id'! ");
    }
    val id = kvMap.get("id").toString;
    if (id == null) {
      throw new Exception("创建索引时，传入的map或json的属性'id'的值为null! ");
    }

    val builder = client.prepareIndex(indexName, typeName, id);
    val response = builder.setSource(kvMap)
      .execute()
      .actionGet()
    return response
  }

  /**
    * 为多份文档建立索引
    *
    * @param indexName 索引名，相当于关系型数据库的库名
    * @param typeName  文档类型，相当于关系型数据库的表名
    * @param jsons     json格式的数据集，其下json串必须有属性"id"
    * @return
    */
  @throws(classOf[Exception])
  def batchIndexDocsForJson(client: Client, indexName: String, typeName: String, jsons: java.util.List[String]): BulkResponse = {
    if (jsons.isEmpty()) {
      throw new Exception("批量创建索引时，传入的参数'jsons'为空！");
    }

    val kvList = new java.util.ArrayList[java.util.Map[String, Object]](jsons.size());
    for (json <- jsons) {
      val kvMap = JSON.parseObject(json)
      kvList.add(kvMap)
    }

    val response = batchIndexDocsForMap(client, indexName, typeName, kvList)
    kvList.clear()
    return response
  }


  /**
    * 为多份文档建立索引
    *
    * @param indexName 索引名，相当于关系型数据库的库名
    * @param typeName  文档类型，相当于关系型数据库的表名
    * @param kvList    键值对形式的数据集，其下map中必须有属性key: "id"
    * @return
    */
  @throws(classOf[Exception])
  def batchIndexDocsForMap(client: Client, indexName: String, typeName: String, kvList: java.util.List[java.util.Map[String, Object]]): BulkResponse = {
    if (kvList.isEmpty()) {
      throw new Exception("批量创建索引时，传入的参数'kvList'为空！")
    }

    val requestList = new java.util.ArrayList[IndexRequest](kvList.size())

    for (kvMap <- kvList) {
      if (!kvMap.containsKey("id")) {
        throw new Exception("批量创建索引时，传入的map或json串中没有属性'id'! ")
      }
      val id = kvMap.get("id").toString
      if (id == null) {
        throw new Exception("批量创建索引时，传入的map或json的属性'id'的值为null! ")
      }

      val request = client
        .prepareIndex(indexName, typeName, id).setSource(kvMap)
        .request()
      requestList.add(request)
    }

    val bulkRequest = client.prepareBulk()
    for (request <- requestList) {
      bulkRequest.add(request)
    }

    val response = bulkRequest
      .execute()
      .actionGet()

    return response
  }


  /**
    * 删除一个文档
    *
    * @param indexName 索引名，相当于关系型数据库的库名
    * @param typeName  文档类型，相当于关系型数据库的表名
    * @param id        键值对形式的数据集
    * @return
    */
  @throws(classOf[Exception])
  def deleteDoc(client: Client, indexName: String, typeName: String, id: String): DeleteResponse = {
    val builder = client.prepareDelete(indexName, typeName, id)
    val response = builder
      .execute()
      .actionGet()
    return response
  }

  /**
    * 根据条件删除多个文档
    *
    * @param indexName    索引名，相当于关系型数据库的库名
    * @param typeName     文档类型，相当于关系型数据库的表名
    * @param queryBuilder 查询器
    * @return
    */
  @throws(classOf[Exception])
  def deleteDocsByQuery(client: Client, indexName: String, typeName: String, queryBuilder: QueryBuilder) = {
    val response = new DeleteByQueryRequestBuilder(client, DeleteByQueryAction.INSTANCE)
      .setIndices(indexName)
      .setTypes(typeName)
      .setQuery(queryBuilder)
      .execute().actionGet()
  }


  /**
    * 指定id获取文档
    *
    * @param indexName 索引名，相当于关系型数据库的库名
    * @param typeName  文档类型，相当于关系型数据库的表名
    * @param id        文档id
    * @return
    */
  def getDoc(client: Client, indexName: String, typeName: String, id: String): java.util.Map[String, Object] = {
    val response = client.prepareGet(indexName, typeName, id)
      .execute()
      .actionGet()

    val retMap = response.getSourceAsMap()
    return retMap;
  }


  def search(client: Client, indexName: String, typeName: String, queryBuilder: QueryBuilder, filterMap: java.util.Map[String, Object]): java.util.List[java.util.Map[String, Object]] = {
    var builder = client.prepareSearch(indexName).setTypes(typeName)
    if (queryBuilder != null) {
      builder = builder.setQuery(queryBuilder)
    }
    if (filterMap != null) {
      builder = builder.setPostFilter(filterMap)
    }

    val searchResponse = builder.execute().actionGet()

    val hits = searchResponse.getHits()
    log.info("Es Hits count: " + hits.getTotalHits())

    val kvList = new java.util.ArrayList[java.util.Map[String, Object]]()

    val hitArray = hits.getHits()
    if (hitArray.length > 0) {
      for (hit <- hitArray) {
        val kvMap = hit.getSource()
        kvMap.put("id", hit.getId())
        kvList.add(kvMap)
      }
    }
    return kvList
  }


}
