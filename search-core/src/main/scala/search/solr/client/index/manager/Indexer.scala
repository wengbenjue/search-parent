package search.solr.client.index.manager

import java.util.concurrent.Executors

import com.google.common.util.concurrent.MoreExecutors
import search.common.queue.BlockQueue
import search.solr.client.{SolrClientConf, SolrClient}

import scala.reflect.ClassTag

/**
  * Created by soledede on 2015/11/23.
  */
private[search] class Indexer private(conf: SolrClientConf) {

  //private val c = SolrClient(conf)
  //private var blockQueue: BlockQueue = _
  //def initBlockQueue[D: ClassTag](task: String = "mapDoc") = this.blockQueue = BlockQueue[D](conf,task)
  // def start = {}



}


private[search] object Indexer {
  var indexer: Indexer = null
  def apply[D: ClassTag](conf: SolrClientConf): Indexer = {
    if (indexer == null) indexer = new Indexer(conf)
    indexer
  }
}
