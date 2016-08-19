package search.common.listener.graph


/**
  * Created by soledede.weng on 2016/7/28.
  */
trait KnowledgeGraphListener {
  def onUpdateState(state: UpdateState)

  def onNewRequest(request: Request)

  def onWarmCache()

  def onIndexGraphNlp(indexGraphNlp: IndexGraphNlp)
}
