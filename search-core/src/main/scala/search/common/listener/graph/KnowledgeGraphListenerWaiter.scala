package search.common.listener.graph

import java.util.concurrent.atomic.AtomicBoolean

import search.common.listener.ListenerWaiter

/**
  * Created by soledede.weng on 2016/7/28.
  */
trait KnowledgeGraphListenerWaiter extends ListenerWaiter[KnowledgeGraphListener, KnowledgeGraphListenerEvent] {

  private val logDroppedEvent = new AtomicBoolean(false)

  override def onDropEvent(event: KnowledgeGraphListenerEvent): Unit = {
    if (logDroppedEvent.compareAndSet(false, true)) {
      // Only log the following message once to avoid duplicated annoying logs.
      logError("Dropping ListenerEvent because no remaining room in event queue. " +
        "This likely means one of the Listeners is too slow and cannot keep up with " +
        "the rate at which tasks are being started by the scheduler.")
    }
  }

  override def onPostEvent(listener: KnowledgeGraphListener, event: KnowledgeGraphListenerEvent): Unit = {
    event match {
      case updateState: UpdateState =>
        listener.onUpdateState(updateState)
      case request: Request =>
        listener.onNewRequest(request)
      case _ =>
    }
  }
}
