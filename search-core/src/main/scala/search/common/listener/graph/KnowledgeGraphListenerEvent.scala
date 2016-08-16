package search.common.listener.graph

import search.common.entity.state.ProcessState

/**
  * Created by soledede.weng on 2016/7/28.
  */
sealed trait KnowledgeGraphListenerEvent

case class UpdateState(query: String,processState: ProcessState) extends KnowledgeGraphListenerEvent

case class Request(query: String,needSearch: Int,showLevel: Integer) extends KnowledgeGraphListenerEvent

case class WarmCache() extends KnowledgeGraphListenerEvent
