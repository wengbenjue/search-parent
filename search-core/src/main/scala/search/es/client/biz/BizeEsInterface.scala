package search.es.client.biz

import java.io.IOException
import java.net.{URLEncoder, URI}
import java.util

import com.alibaba.fastjson.{JSONObject, JSON}
import org.ansj.splitWord.analysis.ToAnalysis
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.{URIUtils, URLEncodedUtils, HttpClientUtils}
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import search.common.cache.pagecache.ESSearchPageCache
import search.common.config.EsConfiguration
import search.common.entity.bizesinterface.{IndexObjEntity, QueryData, Result}
import search.common.entity.state.ProcessState
import search.common.http.HttpClientUtil
import search.common.listener.graph.{Request, UpdateState}
import search.common.util.{FinshedStatus, Util, KnowledgeGraphStatus, Logging}
import search.es.client.util.EsClientConf
import search.common.entity.searchinterface.NiNi
import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2016/7/28.
  */
private[search] object BizeEsInterface extends Logging with EsConfiguration {

  val conf = new EsClientConf()
  conf.init()
  val clinet = conf.esClient

  val keywordField = "keyword"
  val keywordStringField = "keyword_string"
  val pinyinField = "pinyin"
  val scoreField = "score"
  val relevantKwsField = "relevant_kws"


  def totalIndexRun(indexName: String, typeName: String) = {
    clinet.totalIndexRun(indexName, typeName)
    Thread.currentThread().suspend()
  }

  def searchTopKeyWord(sessionId: String, keyword: String): NiNi = {
    Util.caculateCostTime {
      cacheQueryBestKeyWord(keyword, 1)
    }
  }


  def wrapIndexByKeywords(keywords: java.util.Collection[IndexObjEntity]): NiNi = {
    Util.caculateCostTime {
      indexDataWithRw(keywords)
    }
  }

  def indexByKeywords(sessionId: String, originQuery: String, keywords: java.util.Collection[String]): NiNi = {
    Util.caculateCostTime {
      indexData(sessionId, originQuery, keywords)
    }
  }


  def wrapShowStateAndGetByQuery(query: String, needSearch: Int): NiNi = {
    Util.caculateCostTime {
      showStateAndGetByQuery(query, needSearch)
    }
  }


  def wrapCleanRedisByNamespace(namespace: String): NiNi = {
    Util.caculateCostTime {
      cleanRedisByNamespace(namespace)
    }
  }

  /**
    *
    * @param query
    * @return
    */
  def showStateAndGetByQuery(query: String, needSearch: Int): Result = {
    val state = conf.stateCache.getObj[ProcessState](query)
    if (state != null) {
      val imutableState = state.clone()
      val currentState = imutableState.getCurrentState
      if (currentState == 0) triggerQuery(query, needSearch)
      else {
        val isFinished = imutableState.getFinished
        if (isFinished == FinshedStatus.UNFINISHED) {
          new Result(imutableState)
        } else {
          val result = new Result(imutableState, cacheQueryBestKeyWord(query, needSearch))
          result
        }
      }

    } else {
      triggerQuery(query, needSearch)
    }

  }

  def triggerQuery(query: String, needSearch: Int): Result = {
    val state = new ProcessState(0, 0)
    conf.waiter.post(Request(query, needSearch))
    new Result(state)
  }

  /**
    *
    * @param keywords
    * @return
    */
  def indexData(sessionId: String, originQuery: String, keywords: java.util.Collection[String]): Boolean = {
    val resutlt = clinet.incrementIndex(graphIndexName, graphTypName, keywords)
    cleanRedisByNamespace(cleanNameSpace)
    resutlt
  }

  def indexDataWithRw(keywords: java.util.Collection[IndexObjEntity]): Boolean = {
    val result = clinet.incrementIndexWithRw(graphIndexName, graphTypName, keywords)
    cleanRedisByNamespace(cleanNameSpace)
    result
  }

  def cacheQueryBestKeyWord(query: String, needSearch: Int): AnyRef = {
    ESSearchPageCache.cacheShowStateAndGetByQuery(query, {
      queryBestKeyWord(null, query, false, needSearch)
    })
  }


  def wrapDelIndexByKeywords(keywords: java.util.Collection[String]): NiNi = {
    Util.caculateCostTime {
      delIndexByKeywords(keywords)
    }
  }

  def delIndexByKeywords(keywords: java.util.Collection[String]): Boolean = {
    clinet.decrementIndex(graphIndexName, graphTypName, keywords)
  }

  /**
    *
    * @param keyword
    * @param reSearch
    * @return
    */
  def queryBestKeyWord(sessionId: String, keyword: String, reSearch: Boolean = false, needSearch: Int): AnyRef = {


    if (keyword == null || keyword.trim.equalsIgnoreCase("")) {
      logError("keyword can't be null")
      return returnNoData
    }



    updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.UNFINISHED)

    if (needSearch != 1) {

      val result = wrapRequestNlp(sessionId, keyword, keyword)
      updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
      return result
    }

    var targetKeyword: String = null


    def relevantTargetKeyWord(score: java.lang.Float, keyWord: String): Unit = {
      val matchQueryResult1 = clinet.matchQuery(graphIndexName, graphTypName, 0, 1, keywordField, keyword)
      if (matchQueryResult1 != null && matchQueryResult1.length > 0) {
        val doc = matchQueryResult1.head
        val matchScore = doc.get(scoreField).toString.toFloat
        if (matchScore > matchScoreThreshold) {
          if (score != null && matchScore < score) {
            targetKeyword = keyWord
            logInfo(s"${keyword} have been matched according pinyin and relevant,relevant score:${matchScore} pinyin score:${score}  targetKeyword: $targetKeyword")
          } else {
            val matchKeyWord = doc.get(keywordField).toString
            targetKeyword = matchKeyWord
            logInfo(s"${keyword} have been matched according relevant,relevant score:${matchScore}  targetKeyword: $targetKeyword")
          }
        } else if (score != null && score >= pinyinScoreThreshold) {
          targetKeyword = keyWord
          logInfo(s"${keyword} have been matched according relevant,relevant score:${matchScore} pinyin score:${score}  targetKeyword: $targetKeyword")
        } else {
          //word2Vec expand query
          word2VectorProcess(keyword)
        }
      } else {
        word2VectorProcess(keyword)
      }

    }


    //like kfc->肯德基
    def relevantTargetKeyWordByRelevantKw = {
      val matchQueryResult = clinet.matchQuery(graphIndexName, graphTypName, 0, 1, relevantKwsField, keyword)
      if (matchQueryResult != null && matchQueryResult.length > 0) {
        val doc = matchQueryResult.head
        val rlvKWScore = doc.get(scoreField).toString.toFloat
        val matchKeyWord = doc.get(keywordField).toString
        if (rlvKWScore >= matchRelevantKWThreshold) {
          targetKeyword = matchKeyWord
          logInfo(s"${keyword} have been matched by relevantKw,relevant score:${rlvKWScore}} targetKeyword: $targetKeyword")
        } else {
          //pinyin match
          pinyinMatch
        }
      } else {
        pinyinMatch
      }

    }



    def word2VectorProcess(keyWord: String) = {
      val relevantWords = splitWords(keyWord).flatMap(conf.similarityCaculate.word2Vec(_))
      if (relevantWords != null && !relevantWords.isEmpty && relevantWords.size > 0) {
        val matchQueryResult1 = clinet.matchQuery(graphIndexName, graphTypName, 0, 1, keywordField, relevantWords.mkString(" "))
        if (matchQueryResult1 != null && matchQueryResult1.length > 0) {
          val doc = matchQueryResult1.head
          val word2VecScore = doc.get(scoreField).toString.toFloat
          val matchKeyWord = doc.get(keywordField).toString
          if (word2VecScore >= matchScoreThreshold) {
            targetKeyword = matchKeyWord
            logInfo(s"${keyword} have been matched by word2vec,relevant score:${word2VecScore},similarity keywords: ${relevantWords.mkString(" ")} targetKeyword: $targetKeyword")
          }
        }
      }
    }


    def pinyinMatch: Unit = {
      //pinyin query
      val pinyinResult = clinet.matchQuery(graphIndexName, graphTypName, 0, 1, pinyinField, keyword)
      if (pinyinResult != null && pinyinResult.length > 0) {
        val doc = pinyinResult.head
        val docScore = doc.get(scoreField).toString.toFloat
        val docKeyword = doc.get(keywordField).toString
        if (docScore > pinyinScoreThreshold && keyword.length == docKeyword.trim.length) {
          targetKeyword = docKeyword
          logInfo(s"${keyword} have been matched according pinyin,pinyin score:${docScore}  targetKeyword: $targetKeyword")
        } else {
          //relevant query
          relevantTargetKeyWord(score = docScore, keyWord = docKeyword)
        }
      } else {
        //relevant query
        relevantTargetKeyWord(score = null, keyWord = null)
      }
    }

    // term query
    val mustResult = clinet.boolMustQuery(graphIndexName, graphTypName, 0, 1, keywordStringField, keyword)
    if (mustResult != null && mustResult.length > 0) {
      targetKeyword = mustResult.head.get(keywordField).toString
      logInfo(s"${keyword} have been matched accurately  targetKeyword: $targetKeyword")
    } else {
      relevantTargetKeyWordByRelevantKw
    }



    if (targetKeyword == null && !reSearch) {
      //no data, save status to redis and request to trigger crawler with fetch data asynchronous

      //updateState(sessionId, keyword, KnowledgeGraphStatus.FETCH_PROCESS, FinshedStatus.UNFINISHED)
      var dataString: String = null
      //dataString = realTimeCrawler(keyword)
      //TODO

      if (dataString == null) {
        //return no data
        //TODO
        //updateState(sessionId, keyword, KnowledgeGraphStatus.FETCH_PROCESS, FinshedStatus.FINISHED)
        updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
        return returnNoData
      } else {
        val dataJson = JSON.parseObject(dataString)
        val data = dataJson.getJSONArray("data")
        updateState(sessionId, keyword, KnowledgeGraphStatus.NLP_PROCESS, FinshedStatus.UNFINISHED)
        if (data == null || data.size() == 0) {
          updateState(sessionId, keyword, KnowledgeGraphStatus.FETCH_PROCESS, FinshedStatus.FINISHED)
          //return no data
          return returnNoData
        } else {
          //fetch request nlp
          //TODO
          //requery
          return queryBestKeyWord(sessionId, keyword, true, 1)
        }
      }
    } else if (targetKeyword == null && reSearch) {
      updateState(sessionId, keyword, KnowledgeGraphStatus.NLP_PROCESS, FinshedStatus.FINISHED)
      //return nodata
      return returnNoData
    } else {
      if (targetKeyword != null) {
        //request nlp
        //found keyword
        var result = wrapRequestNlp(sessionId, keyword, targetKeyword)
        if (!reSearch)
          updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
        else {
          updateState(sessionId, keyword, KnowledgeGraphStatus.NLP_PROCESS, FinshedStatus.FINISHED)
        }
        try {
          val nlpObj = result.getData
          if (nlpObj != null && nlpObj.isInstanceOf[JSONObject]) {
            val nlpJsonObj = nlpObj.asInstanceOf[JSONObject]
            val dataType = nlpJsonObj.getString("type")
            if (dataType.trim.equalsIgnoreCase("ERROR")) {
              result = null
            }
          }
        } catch {
          case e: Exception =>
        }
        return result
      } else {
        updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
        return returnNoData
      }
    }


  }

  def cleanRedisByNamespace(namespace: String): String = {
    try {
      conf.stateCache.cleanAll()
      val sets = conf.storage.keys(s"$namespace:*")
      sets.foreach(conf.storage.del(_))
      logInfo(s"clean ${namespace} from redis successfully!")
      s"clean ${namespace} from redis successfully"
    } catch {
      case e: Exception =>
        val result = s"need clean ${namespace} from redis again"
        logInfo(result)
        result
    }

  }

  /**
    * request nlp graph with targetKeyword
    *
    * @param originQuery just for update state
    * @param query
    * @return
    */
  private def requestNlp(sessionId: String, originQuery: String, query: String): AnyRef = {
    val url: String = s"${graphUrl}${URLEncoder.encode(query, "UTF-8")}"
    var httpResp: CloseableHttpResponse = null
    try {
      httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
      val entity: HttpEntity = httpResp.getEntity
      val sResponse: String = EntityUtils.toString(entity)
      JSON.parseObject(sResponse)
    }
    catch {
      case e: IOException => {
        logError("request graph nlp failed!", e)
        null
      }
    } finally {
      if (httpResp != null)
        HttpClientUtils.closeQuietly(httpResp)
    }
  }


  private def wrapRequestNlp(sessionId: String, originQuery: String, query: String): QueryData = {
    new QueryData(originQuery, query, requestNlp(sessionId, originQuery, query))
  }


  private def returnNoData(): AnyRef = {
    null
  }

  private def updateState(sessionId: String, query: String, currentDate: Int, finishState: Int) = {
    if (sessionId != null)
      conf.waiter.post(UpdateState(sessionId + query, new ProcessState(currentDate, finishState)))
    else
      conf.waiter.post(UpdateState(query, new ProcessState(currentDate, finishState)))
  }

  private def splitWords(keyWord: String): Set[String] = {
    val terms = ToAnalysis.parse(keyWord)
    if (terms == null) return Set.empty[String]
    else {
      terms.map(_.getName).filter(_.length > 1).toSet
    }

  }


  private def realTimeCrawler(kw: String): String = {
    val url: String = s"${fetchUrl}$kw"
    val httpResp: CloseableHttpResponse = HttpClientUtil.requestHttpSyn(url, "get", null, null)
    try {
      if (httpResp != null) {
        val entity: HttpEntity = httpResp.getEntity
        val sResponse: String = EntityUtils.toString(entity)
        sResponse
      } else null
    }
    catch {
      case e: IOException => {
        logError("real fetch data failed!", e)
        null
      }
    } finally {
      HttpClientUtils.closeQuietly(httpResp)
    }
  }


  def wrapDeleteAllMongoData(): NiNi = {
    Util.caculateCostTime {
      deleteAllMongoData
    }
  }

  def deleteAllMongoData(): Boolean = {
    conf.mongoDataManager.deleteAllData()
  }


  def delAllData(): Boolean = {
    clinet.delAllData(graphIndexName, graphTypName)
  }


  def wrapCleanAllFromMongoAndIndex = {
    Util.caculateCostTime {
      cleanAllFromMongoAndIndex()
    }
  }

  def cleanAllFromMongoAndIndex(): Boolean = {
    deleteAllMongoData()
    delAllData()
  }


  def main(args: Array[String]) {
    //testTotalIndexRun
    //testQueryBestKeyWord
    //testRealTimeCrawler
    //testShowStateByQuery
    //testDecrementIndex
    //testCleanRedisByNamespace
    //testDeleteAllMongoData()
    //testDelAllData
  }


  private def testDelAllData() = {
    println(BizeEsInterface.delAllData())
  }

  private def testDeleteAllMongoData() = {
    println(BizeEsInterface.deleteAllMongoData())
  }

  private def testCleanRedisByNamespace() = {
    BizeEsInterface.cleanRedisByNamespace("graph_state")
  }

  private def testRealTimeCrawler() = {
    BizeEsInterface.realTimeCrawler("王老吉")
  }

  private def testQueryBestKeyWord() = {
    val result = BizeEsInterface.queryBestKeyWord(null, "百度", false, 1)
    println(result)
  }

  private def testTotalIndexRun() = {
    val indexName = "nlp"
    val typName = "graph"
    BizeEsInterface.totalIndexRun(indexName, typName)
  }

  private def testShowStateByQuery() = {
    val query = "百度凤凰"
    val result = BizeEsInterface.showStateAndGetByQuery(query, 1)
    println(result)
  }

  private def testDecrementIndex() = {
    val query = "建筑材料"
    val keywords = new util.ArrayList[String]()
    keywords.add(query)
    val result = BizeEsInterface.delIndexByKeywords(keywords)
    println(result)
  }
}
