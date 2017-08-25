package search.common.listener.trace

/**
 * Created by soledede on 2016/4/7.
 */
sealed trait TraceListenerEvent

case class AddIndex(content: String) extends TraceListenerEvent

case class DelLastIndex() extends TraceListenerEvent

case class SolrCollectionTimeout() extends TraceListenerEvent

case class SolrNoHelthNode() extends TraceListenerEvent

case class SwitchSolrServer(server: String) extends TraceListenerEvent




