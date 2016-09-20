package search.es.client.biz

import java.io.FileOutputStream

import search.common.config.EsConfiguration
import search.common.serializer.JavaSerializer
import search.common.util.Logging
import search.solr.client.SolrClientConf

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] object BizeEsInterfaceUtils extends Logging with EsConfiguration{


  /**
    * dump索引到磁盘
    * @return
    */
  def dumpIndexToDisk(): String = {
    val cnt = BizeEsInterface.count().toInt
    val result = BizeEsInterface.matchAllQueryWithCount(0, cnt)
    val fOut = new FileOutputStream(dumpIndexPath)
    val ser = JavaSerializer(new SolrClientConf()).newInstance()
    val outputStrem = ser.serializeStream(fOut)
    outputStrem.writeObject(result)
    outputStrem.flush()
    outputStrem.close()
    val resultString = s"dump index to dis successful,size:${cnt},local path:${dumpIndexPath}"
    println(resultString)
    resultString
  }



}
