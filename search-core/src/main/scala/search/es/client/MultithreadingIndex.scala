package search.es.client

import com.mongodb.BasicDBObject
import org.elasticsearch.action.ActionFuture
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.client.{Client, Requests}
import search.common.cache.impl.LocalCache
import search.common.entity.bizesinterface.IndexObjEntity
import search.common.util.Logging
import search.es.client.util.EsClientConf

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] class MultithreadingIndex() extends Logging {
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
  def bulkPostDocument(client: Client, indexName: String, typeName: String, docs: java.util.Collection[java.util.Map[String, Object]]): Boolean = {

    import scala.collection.JavaConversions._
    try {
      var cnt = 0
      var request: BulkRequest = Requests.bulkRequest
      request.refresh(true)
      for (doc <- docs) {
        cnt += 1
        var id = doc.get("id")
        if (id == null) {
          id = doc.get("_id")
        }
        doc.remove("id")
        doc.remove("_id")
        logInfo(s"id ${id} indexed.")
        if (id == null) request.add(Requests.indexRequest(indexName).`type`(typeName).source(doc))
        else request.add(Requests.indexRequest(indexName).`type`(typeName).id(id.toString.trim).source(doc))
        if (cnt > 1000 && cnt % 1000 == 0) {
          bulkPostDocumentSubmit(client, request)
          request = Requests.bulkRequest
          request.refresh(true)
          //Thread.sleep(1000)
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


}
