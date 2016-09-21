package search.es.client.biz

import java.io.{FileInputStream, FileOutputStream}

import search.common.algorithm.impl.TrieDictionaryExpand
import search.common.config.EsConfiguration
import search.common.entity.bizesinterface.{IndexObjEntity, QueryEntityWithCnt}
import search.common.serializer.JavaSerializer
import search.common.util.Logging
import search.es.client.util.EsClientConf
import search.solr.client.SolrClientConf

/**
  * Created by soledede.weng on 2016/9/20.
  */
private[search] object BizeEsInterfaceUtils extends Logging with EsConfiguration {


  /**
    * dump索引到磁盘
    *
    * @return
    */
  def dumpIndexToDisk(): String = {
    try {
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
    } catch {
      case e: Exception => null
    }
  }

  /**
    * 从磁盘读入Trie树
    *
    * @param conf
    */
  def readDumpTrieFromDisk(conf: EsClientConf) = {
    readDumpTrieDictionaryFromDisk
    readDumpTrieGraphDictionaryFromDisk

    def readDumpTrieDictionaryFromDisk() = {
      try {
        val ser = JavaSerializer(new SolrClientConf()).newInstance()
        val fIput = new FileInputStream(dumpDictionaryPath)
        val inputStream = ser.deserializeStream(fIput)
        val obj = inputStream.readObject[TrieDictionaryExpand]()
        if (obj != null) {
          conf.dictionary = obj
        }
        inputStream.close()
      } catch {
        case fE: java.io.FileNotFoundException=>logInfo("I can't read,no dump to disk")
        case e: Exception =>
          logError("read dump trie failed! dumpDictionaryPath",e)
      }
    }

    def readDumpTrieGraphDictionaryFromDisk() = {
      try {
        val ser = JavaSerializer(new SolrClientConf()).newInstance()
        val fIput = new FileInputStream(dumpGraphDictionaryPath)
        val inputStream = ser.deserializeStream(fIput)
        val obj = inputStream.readObject[TrieDictionaryExpand]()
        if (obj != null) {
          conf.graphDictionary = obj
        }
        inputStream.close()
      } catch {
        case fE: java.io.FileNotFoundException=>logInfo("I can't read,no dump to disk")
        case e: Exception =>
          logError("read dump trie failed! dumpGraphDictionaryPath",e)
      }
    }

  }


  /**
    * dump Trie树到磁盘
    *
    * @param conf
    */
  def dumpTrieToDisk(conf: EsClientConf): Long = {

    dumpDictionaryToDisk
    dumpGraphDictionaryToDisk


    def dumpDictionaryToDisk() = {
      if (conf.dictionary != null) {
        try {
          val fOut = new FileOutputStream(dumpDictionaryPath)
          val ser = JavaSerializer(new SolrClientConf()).newInstance()
          val outputStrem = ser.serializeStream(fOut)
          outputStrem.writeObject(conf.dictionary)
          outputStrem.flush()
          outputStrem.close()
          val resultString = s"dump trie dictionary  to dis successful,local path:${dumpDictionaryPath}"
          println(resultString)
        } catch {
          case e: Exception =>
            logError("write dump trie failed! dumpDictionaryPath",e)
        }
      }
    }

    def dumpGraphDictionaryToDisk() = {
      if (conf.graphDictionary != null) {
        try {
          val fOut = new FileOutputStream(dumpGraphDictionaryPath)
          val ser = JavaSerializer(new SolrClientConf()).newInstance()
          val outputStrem = ser.serializeStream(fOut)
          outputStrem.writeObject(conf.graphDictionary)
          outputStrem.flush()
          outputStrem.close()
          val resultString = s"dump trie dictionary  to dis successful,local path:${dumpGraphDictionaryPath}"
          println(resultString)
        } catch {
          case e: Exception =>
            logError("write dump trie failed! dumpGraphDictionaryPath",e)
        }
      }
    }
    -1
  }


}
