package search.common.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
  * Created by soledede.weng on 2016/7/27.
  */
private[search] trait EsConfiguration extends Configuration {


  val esConfig = ConfigFactory.load("es.conf")
  lazy val esClusterName = Try(esConfig.getString("cluster.name")).getOrElse("es-cluster")

  lazy val esHosts = Try(esConfig.getString("es.hosts")).getOrElse("127.0.0.1:9300")

  lazy val pinyinScoreThreshold = Try(esConfig.getDouble("match.pinyinScoreThreshold")).getOrElse(37.0)
  lazy val matchScoreThreshold = Try(esConfig.getDouble("match.matchScoreThreshold")).getOrElse(23.0)
  lazy val matchRelevantKWThreshold = Try(esConfig.getDouble("match.matchRelevantKWThreshold")).getOrElse(30.0)

  lazy val esClients = Try(esConfig.getInt("es.clients")).getOrElse(3)

  lazy val number_of_shards = Try(esConfig.getInt("es.number_of_shards")).getOrElse(3)
  lazy val number_of_replicas = Try(esConfig.getInt("es.number_of_replicas")).getOrElse(3)
  lazy val pageSize = Try(esConfig.getInt("index.pageSize")).getOrElse(1000)

  lazy val bulkCommitSize = Try(esConfig.getInt("index.bulkCommitSize")).getOrElse(-1)

  lazy val graphIndexName = Try(esConfig.getString("graph.indexName")).getOrElse("nlp")
  lazy val graphTypName = Try(esConfig.getString("graph.typName")).getOrElse("graph")

  lazy val fetchUrl = Try(esConfig.getString("api.url.crawler")).getOrElse("http://192.168.100.20:5000/?kw=")
  lazy val graphUrl = Try(esConfig.getString("api.url.graph")).getOrElse("http://192.168.250.207:9000/api/graph/mgra?c=")
}
