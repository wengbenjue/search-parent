package search.common.listener.graph

import com.alibaba.fastjson.JSON
import search.common.config.RedisConfiguration
import search.es.client.biz.BizeEsInterface
import search.es.client.util.EsClientConf

/**
  * Created by soledede.weng on 2016/7/28.
  */
class KnowledgeGraphListenerImpl(conf: EsClientConf) extends KnowledgeGraphListener with RedisConfiguration {

  override def onUpdateState(updateState: UpdateState): Unit = {
    val processState = updateState.processState
    //val processStateString = JSON.toJSONString(processState, false)
    conf.stateCache.put(updateState.query, processState, expireTime)
  }

  override def onNewRequest(request: Request): Unit = {
    BizeEsInterface.cacheQueryBestKeyWord(request.query,request.needSearch)
  }
}
