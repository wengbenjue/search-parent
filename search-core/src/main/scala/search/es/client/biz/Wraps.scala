package search.es.client.biz

import search.common.entity.searchinterface.NiNi
import search.common.util.Util

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] object Wraps {


  def wrapDumpIndexToDisk(): NiNi = {
    Util.caculateCostTime {
      BizeEsInterfaceUtils.dumpIndexToDisk()
    }
  }

}
