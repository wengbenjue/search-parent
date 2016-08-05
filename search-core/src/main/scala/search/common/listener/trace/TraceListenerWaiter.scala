package search.common.listener.trace

import java.util.concurrent.atomic.AtomicBoolean

import search.common.listener.ListenerWaiter

/**
  * Created by soledede on 2015/9/17.
  */
trait TraceListenerWaiter extends ListenerWaiter[TraceListener, TraceListenerEvent] {


  private val logDroppedEvent = new AtomicBoolean(false)

  override def onDropEvent(event: TraceListenerEvent): Unit = {
    if (logDroppedEvent.compareAndSet(false, true)) {
      // Only log the following message once to avoid duplicated annoying logs.
      logError("Dropping ListenerEvent because no remaining room in event queue. " +
        "This likely means one of the Listeners is too slow and cannot keep up with " +
        "the rate at which tasks are being started by the scheduler.")
    }
  }

  override def onPostEvent(listener: TraceListener, event: TraceListenerEvent): Unit = {

    event match {
      case addIndex: AddIndex =>
        listener.onAddIndex(addIndex)
      case delLastIndex: DelLastIndex =>
        listener.onDelLastIndex()
      case solrCollectionTimeout:SolrCollectionTimeout =>
        listener.onSolrCollectionTimeout()
      case nodeHelthy: SolrNoHelthNode =>
        listener.onNodeNoHealth()
      case switchServer: SwitchSolrServer =>
        listener.onSwitchSolrServer(switchServer.server)
    }
  }
}
