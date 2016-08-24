package search.common.clock

import java.util.Date
import java.util.concurrent.TimeUnit

import com.google.common.cache._
import search.common.util.{Logging, Util}

/**
  *
  * Created by soledede.weng on 2016/6/12.
  *
  * @param name
  * @param callback
  * @param interval
  * @param isRestartIfTimeout
  * @param workerType
  */
private[search] class CloudTimerWorker(name: String,
                                      var callback: () => Long,
                                      var interval: Long = 1 * 1000,
                                      isRestartIfTimeout: () => Boolean = () => true,
                                      var workerType: CloudTimerWorkerType.Type = CloudTimerWorkerType.REPEAT) extends Logging {
  // initialize TimerWork
  val workerName: String = "TimerWorker-" + name
  val workerDaemonName: String = "TimerWorker-daemon-" + name
  private val checkInterval: Long = interval * 5
  private val expiredTime: Long = interval * Math.log(interval).toInt * 40
  private val clock = new SystemTimerClock()
  var workerThread: Timing = new Timing(clock, interval, doWork, workerName)
  var workerDaemonThread: Timing = new Timing(clock, checkInterval, monitorWorker, workerDaemonName)
  private var runState: Boolean = false

  var cacheManager = CloudTimerWorker.initCache(workerName, expiredTime)
  logInfo(workerName + " init finished")

  def updateCache(key: String, value: Long) {
    cacheManager.put(key, value)
    val nowStr = Util.convertDateFormat(value)
    logDebug(Util.convertDateFormat(new Date()) + ":\t update cache " + key + "->" + nowStr)
  }

  def updateWorderThreadCache() {
    updateCache(workerName, System.currentTimeMillis())
  }

  def startUp() {
    cacheManager.apply(workerDaemonName)
    workerDaemonThread.start()
    workerThread.start()
    cacheManager.apply(workerName)
    //Thread.sleep(Integer.MAX_VALUE)
  }

  //get workerThread default interval time
  protected def getDefaultIntervaltime = interval

  protected def doWork() {
    monitorWorkerDaemon()
    if (workerType == CloudTimerWorkerType.NO_REPEAT) {
      if (runState == false) {
        callback()
        runState = true
      }
    } else {
      var newPeriod = callback()
      if (newPeriod <= 0) {
        newPeriod = getDefaultIntervaltime
      }
      if (newPeriod > 0) {
        newPeriod = List(newPeriod, expiredTime - 10).min
        workerThread.period = newPeriod
        logDebug("reset " + workerName + " newPeriod " + newPeriod)
      }
    }
  }

  def monitorWorkerDaemon() {
    val now = System.currentTimeMillis()
    updateCache(workerName, now)
    logDebug("check " + workerDaemonName + " running state")
    val lastTimeCache = Option(cacheManager.getIfPresent(workerDaemonName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerDaemonName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        logInfo(workerDaemonName + " may be dead")
        workerDaemonThread.stop(true)
        workerDaemonThread.restart()
        updateCache(workerDaemonName, now)
      }
    }
  }

  protected def monitorWorker() {
    val now = System.currentTimeMillis()
    updateCache(workerDaemonName, now)
    logDebug("check " + workerName + " running state")
    val lastTimeCache = Option(cacheManager.getIfPresent(workerName))
    lastTimeCache match {
      case Some(lasttime) => logDebug(workerName + " run at " + Util.convertDateFormat(lasttime))
      case None => {
        if (isRestartIfTimeout() == false) {
          return
        } else {
          logInfo(workerName + " may be dead")
          workerThread.stop(true)
          runState = false
          workerThread.restart()
          updateCache(workerName, now)
        }
      }
    }
  }

}

private[search] object CloudTimerWorker {
  def initCache(workerName: String, expiredTime: Long): LoadingCache[java.lang.String, java.lang.Long] = {
    val cacheLoader: CacheLoader[java.lang.String, java.lang.Long] =
      new CacheLoader[java.lang.String, java.lang.Long]() {
        def load(key: java.lang.String): java.lang.Long = {
          long2Long(System.currentTimeMillis())
        }
      }
    var cacheManager = CacheBuilder.newBuilder()
      .expireAfterWrite(expiredTime, TimeUnit.MILLISECONDS).build(cacheLoader)
    cacheManager
  }
}

private[search]
object CloudTimerWorkerType extends Enumeration {
  type Type = Value
  val REPEAT, NO_REPEAT = Value
}
