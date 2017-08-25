package search.es.client.util

import com.mongodb.{BasicDBObject, DBCollection}
import search.common.mongo.base.{MongoTableConstants, MongoBase}

/**
  * Created by soledede.weng on 2016/8/19.
  */
private[search] class CatNlpDataManager(conf: EsClientConf) extends DataManager(conf) {


  override  def getCollection: DBCollection = {
    getCollection(MongoTableConstants.NEWS_GRAPH_CAT_NLP)
  }

}
