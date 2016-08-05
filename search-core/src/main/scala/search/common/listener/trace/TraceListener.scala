package search.common.listener.trace


/**
 * Created by soledede on 2016/4/8.
 */
trait TraceListener{

  def onAddIndex(content: AddIndex)

  def onDelLastIndex()

  def onSolrCollectionTimeout()

  def onNodeNoHealth()

  def onSwitchSolrServer(server: String)
}
