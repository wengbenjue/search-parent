package search.common.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
  * Created by soledede.weng on 2016/7/27.
  */
private[search] trait EsConfiguration extends Configuration {


  val esConfig = ConfigFactory.load("es.conf")
  lazy val esClusterName = Try(esConfig.getString("cluster.name")).getOrElse("es-cloud")

  lazy val esHosts = Try(esConfig.getString("es.hosts")).getOrElse("127.0.0.1:9300")

  lazy val pinyinScoreThreshold = Try(esConfig.getDouble("match.pinyinScoreThreshold")).getOrElse(37.0)
  lazy val matchScoreThreshold = Try(esConfig.getDouble("match.matchScoreThreshold")).getOrElse(23.0)
  lazy val matchRelevantKWThreshold = Try(esConfig.getDouble("match.matchRelevantKWThreshold")).getOrElse(30.0)
  lazy val mulitiMatchRelevantKWThreshold = Try(esConfig.getDouble("match.mulitiMatchRelevantKWThreshold")).getOrElse(10.0)
  lazy val word2vecMatchRelevantKWThreshold = Try(esConfig.getDouble("match.word2vecMatchRelevantKWThreshold")).getOrElse(1.0)

//switch
lazy val switchCrawler = Try(esConfig.getString("switch.crawler")).getOrElse("off")

  lazy val esClients = Try(esConfig.getInt("es.clients")).getOrElse(3)

  lazy val number_of_shards = Try(esConfig.getInt("es.number_of_shards")).getOrElse(3)
  lazy val number_of_replicas = Try(esConfig.getInt("es.number_of_replicas")).getOrElse(3)
  lazy val pageSize = Try(esConfig.getInt("index.pageSize")).getOrElse(1000)

  lazy val bulkCommitSize = Try(esConfig.getInt("index.bulkCommitSize")).getOrElse(-1)

  lazy val graphIndexName = Try(esConfig.getString("graph.indexName")).getOrElse("nlp")
  lazy val graphTypName = Try(esConfig.getString("graph.typName")).getOrElse("graph")

  lazy val newsIndexName = Try(esConfig.getString("news.indexName")).getOrElse("news")
  lazy val newsTypName = Try(esConfig.getString("news.typName")).getOrElse("news")

  lazy val announceIndexName = Try(esConfig.getString("announce.indexName")).getOrElse("announ")
  lazy val announceTypeName = Try(esConfig.getString("announce.typeName")).getOrElse("announ")

  lazy val catTypName = Try(esConfig.getString("graph.catTypeName")).getOrElse("cat")

  lazy val cleanNameSpace = Try(esConfig.getString("clean.namespace")).getOrElse("graph_state")

  lazy val STATE_PREFFIX = Try(esConfig.getString("state.preffix")).getOrElse("state_preffix_")


  lazy val dumpIndexPath = Try(esConfig.getString("index.dumpPath")).getOrElse("D:/es_graph_index")

  lazy val dumpDictionaryPath = Try(esConfig.getString("dump.dictionaryPath")).getOrElse("/home/soledede/searchclient/es_trie_dictionary")
  lazy val dumpGraphDictionaryPath = Try(esConfig.getString("dump.graphDictionaryPath")).getOrElse("/home/soledede/searchclient/es_trie_graph_dictionary")
  lazy val newExcelPath = Try(esConfig.getString("dump.newExcelPath")).getOrElse("/home/soledede/searchclient/kv_news.xlsx")

  //高亮显示字段
  lazy val hlFields = Try(esConfig.getString("hl.fields")).getOrElse("")

  lazy val fetchUrl = Try(esConfig.getString("api.url.crawler")).getOrElse("http://192.168.250.207:8010/api/graph?")
  lazy val graphUrl = Try(esConfig.getString("api.url.graph")).getOrElse("http://192.168.250.207:9000/api/graph/mgra?c=")
  lazy val warmUrl = Try(esConfig.getString("api.url.warmUrl")).getOrElse("http://54.222.222.172:8999/es/search/state/?keyword=")
  lazy val synonymUrl = Try(esConfig.getString("api.url.synonymUrl")).getOrElse("http://54.222.222.172:9001/api/synonym/k/")
  lazy val synonymAddUrl = Try(esConfig.getString("api.url.synonymAddUrl")).getOrElse("http://54.222.222.172:9001/api/synonym/add/")
  lazy val graphNodeDataUrl = Try(esConfig.getString("api.url.graphNodeDataUrl")).getOrElse("http://54.222.222.172:9000/api/news/allnode")
  lazy val companyWeightUrl = Try(esConfig.getString("api.url.companyWeightUrl")).getOrElse("http://54.222.222.172:9000/api/news/comw")
  lazy val industryWeightUrl = Try(esConfig.getString("api.url.industryWeightUrl")).getOrElse("http://54.222.222.172:9000/api/news/indw")
}
