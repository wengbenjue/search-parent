package search.common.util

/**
  * Created by soledede.weng on 2016/8/1.
  */
private[search] object KnowledgeGraphStatus {

  final val SEARCH_QUERY_PROCESS = 1
  final val FETCH_PROCESS = 2
  final val NLP_PROCESS = 3

}
private[search] object FinshedStatus {
  final val UNFINISHED = 0
  final val FINISHED = 1

}