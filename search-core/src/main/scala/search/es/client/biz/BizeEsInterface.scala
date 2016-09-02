package search.es.client.biz

import java.io.{FileInputStream, FileOutputStream, IOException}
import java.net.{URLEncoder, URI}
import java.util
import javax.servlet.http.HttpServletRequest

import com.alibaba.fastjson.{JSONArray, JSONObject, JSON}
import com.mongodb.{BasicDBList, BasicDBObject}
import edu.stanford.nlp.ling.{CoreLabel, CoreAnnotations}
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.util.CoreMap
import org.ansj.splitWord.analysis.ToAnalysis
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.{URIUtils, URLEncodedUtils, HttpClientUtils}
import org.apache.http.util.EntityUtils
import search.common.bloomfilter.mutable.BloomFilter
import search.common.cache.impl.LocalCache
import search.common.cache.pagecache.ESSearchPageCache
import search.common.clock.CloudTimerWorker
import search.common.config.EsConfiguration
import search.common.entity.bizesinterface._
import search.common.entity.state.ProcessState
import search.common.http.HttpClientUtil
import search.common.listener.graph.{WarmCache, Request, UpdateState}
import search.common.redis.RedisClient
import search.common.serializer.JavaSerializer
import search.common.tool.corenlp.CoreNLPSegmentType
import search.common.util._
import search.es.client.EsClient
import search.es.client.util.{QueryCatType, EsClientConf}
import search.common.entity.searchinterface.NiNi
import search.solr.client.{SolrClientConf, SolrClient}
import scala.collection.JavaConversions._
import scala.collection.{JavaConversions, JavaConverters}
import scala.util.control.Breaks._

/**
  * Created by soledede.weng on 2016/7/28.
  */
private[search] object BizeEsInterface extends Logging with EsConfiguration {
  var conf: EsClientConf = _
  var client: EsClient = _


  val keywordFieldWithBoost = "keyword"
  val relevantKwsField_kwWithBoost = "relevant_kws.relevant_kws_kw^4"
  //"relevant match"
  val keywordStringFieldWithBoost = "keyword_string^20"
  //"accurate match"
  val pinyinFieldWithBoost = "pinyin^0.0009"
  val scoreFieldWithBoost = "score"
  val relevantKwsFieldWithBoost = "relevant_kws^19" //"vr->虚拟现实"

  val companyFieldWithBoost = "s_com"
  val comSimFieldWithBoost = "s_zh"
  val comStockCodeFieldWithBoost = "stock_code^500"
  val companyEnFieldWithBoost = "s_en"

  val word2vecWithBoost = "word2vec^3"
  val word2vecRwWithBoost = "word2vec.word2vec_rw^6"


  val keywordField = "keyword"
  val relevantKwsField_kw = "relevant_kws.relevant_kws_kw"
  //"relevant match"
  val keywordStringField = "keyword_string"
  //"accurate match"
  val pinyinField = "pinyin"
  val scoreField = "score"
  val relevantKwsField = "relevant_kws" //"vr->虚拟现实"

  val companyField = "s_com"
  val comSimField = "s_zh"
  val comStockCodeField = "stock_code"
  val comStockCodeStringField = "stock_code_string"
  val companyEnField = "s_en"

  val timerPeriodSchedule = new CloudTimerWorker(name = "timerPeriodSchedule", interval = 1000 * 60 * 60 * 24, callback = () => loadEventRegexRule())


  var eventRegexRuleSets = new java.util.HashSet[String]()

  def loadEventRegexRule(): Long = {
    loadEventRegexToCache
    -1
  }

  def loadThread = new Thread {
    override def run(): Unit = {
      load
    }
  }

  loadThread.setDaemon(true)

  def loadStart() = {
    loadThread.start()
  }

  init()

  def init() = {
    this.conf = new EsClientConf()
    this.conf.init()
    this.client = conf.esClient
    loadStart
  }


  def load() = {
    //loadCache
    loadCacheFromCom
    addBloomFilter
    cleanRedisByNamespace(cleanNameSpace)
    //indexCatOfKeywords
    loadEventRegexToCache()
    timerPeriodSchedule.startUp()
  }

  def warpLoadEventRegexToCache(): NiNi = {
    Util.caculateCostTime {
      loadEventRegexToCache()
    }
  }


  def isEvent(query: String): Boolean = {
    var flag = false
    breakable {
      val it = eventRegexRuleSets.iterator()
      while (it.hasNext) {
        val rule = it.next()
        if (Util.regex(query, rule)) {
          flag = true
          break
        }
      }
    }
    flag
  }

  def loadEventRegexToCache(): String = {
    val dm = conf.mongoDataManager

    val events = dm.findEvent()
    if (events != null && events.size() > 0) {
      events.map { m =>
        val k: String = if (m.get("rule") != null) m.get("rule").toString else null
        if (k != null) {
          eventRegexRuleSets.add(k)
          logInfo(s"loaded event regex rule:${k}")
        }
      }
    }
    val returnS = s"event regex rule load to cache successfully,total number:${eventRegexRuleSets.size()}"
    logInfo(returnS)
    returnS
  }

  def wrapViewCache(key: String): NiNi = {
    Util.caculateCostTime {
      veiwCache(key)
    }
  }

  def veiwCache(key: String): Object = {
    if (key == null || key.equalsIgnoreCase("-1")) {
      val set = LocalCache.baseStockCache.keySet
      (set.size, JavaConversions.setAsJavaSet(set))
    } else {
      if (LocalCache.baseStockCache.contains(key.trim)) {
        val stock = LocalCache.baseStockCache(key.trim)
        stock
      } else null
    }
  }

  def warpLoadCache(): NiNi = {
    Util.caculateCostTime {
      //loadCache()
      loadCacheFromCom
    }
  }

  def loadCacheFromCom(): String = {
    val stocks = conf.mongoDataManager.findCompanyCode()
    if (stocks != null && stocks.size() > 0) {
      stocks.foreach { dbobj =>
        var comSim: String = null

        comSim = if (dbobj.get("w") != null) dbobj.get("w").toString else null

        val comCode: String = if (dbobj.get("code") != null) dbobj.get("code").toString else null
        if (comSim != null && comCode != null) {
          LocalCache.baseStockCache(comSim) = new BaseStock(comSim, comCode)
        }

      }
      logInfo("comoany stock cached in local cache")
    }
    "comoany stock cached in local cache"
  }

  def loadCache(): String = {
    val stocks = conf.mongoDataManager.findBaseStock()
    if (stocks != null && stocks.size() > 0) {
      stocks.foreach { dbobj =>
        var comSim: String = null
        var company: String = null
        var comEn: String = null

        val nameObj = dbobj.get("name").asInstanceOf[BasicDBObject]
        if (nameObj != null) {
          comSim = if (nameObj.get("szh") != null) nameObj.get("szh").toString else null
        }

        val orgObj = dbobj.get("org").asInstanceOf[BasicDBObject]
        if (orgObj != null) {
          company = if (orgObj.get("szh") != null) orgObj.get("szh").toString else null
          comEn = if (orgObj.get("en") != null) orgObj.get("en").toString else null
        }
        val comCode: String = if (dbobj.get("code") != null) dbobj.get("code").toString else null
        if (comSim != null && comCode != null) {
          LocalCache.baseStockCache(comSim) = new BaseStock(comSim, company, comEn, comCode)
        }

      }
      logInfo("base stock cached in local cache")
    }
    "base stock cached in local cache"
  }

  def addBloomFilter(): String = {
    conf.bloomFilter = BloomFilter[String](Constants.GRAPH_KEYWORDS_BLOOMFILTER_KEY, expectedElements, falsePositiveRate)
    val cnt = BizeEsInterface.count().toInt
    val obj = BizeEsInterface.matchAllQueryWithCount(0, cnt)
    if (obj != null && obj.getResult != null) {
      val result = JSON.toJSON(obj.getResult).asInstanceOf[JSONArray]
      result.foreach { obj =>
        val obj1 = obj.asInstanceOf[JSONObject]
        val keyWord = obj1.getString("keyword")
        if (keyWord != null && !keyWord.equalsIgnoreCase("")) {
          conf.bloomFilter.add(keyWord)
        }
      }
    }
    s"add bloomFilter successful,size: ${cnt}"
  }


  def wrapAddBloomFilter(): NiNi = {
    Util.caculateCostTime {
      addBloomFilter()
    }
  }

  def warpIndexCatOfKeywords(): NiNi = {
    Util.caculateCostTime {
      indexCatOfKeywords()
    }
  }

  def indexCatOfKeywords(): String = {
    val dm = conf.mongoDataManager

    indexTopicConp

    indexEvent

    indexIndustry

    indexCompany

    //add stock block/concept to index  graph.topic_conp
    def indexTopicConp() = {
      val datas = new java.util.ArrayList[IndexObjEntity]()
      val topicConps = dm.findGrapnTopicConp()
      if (topicConps != null && topicConps.size() > 0) {
        topicConps.map { m =>
          val k: String = if (m.get("w") != null) m.get("w").toString else null
          var codes: BasicDBList = null
          if (m.get("code") != null) codes = m.get("code").asInstanceOf[BasicDBList]

          if (k != null) {
            datas.add(new IndexObjEntity(k, codes.map(_.toString)))
          }
        }
        if (datas.size() > 0)
          client.incrementIndexNlpCat(datas)
      }
      logInfo(s"topic company index successfully,total number:${datas.size()}")
    }

    //add stock event to index
    def indexEvent() = {
      var sets = new java.util.HashSet[String]()
      val datas = new java.util.ArrayList[IndexObjEntity]()
      val events = dm.findEvent()
      if (events != null && events.size() > 0) {
        events.map { m =>
          val k: String = if (m.get("szh") != null) m.get("szh").toString else null
          if (k != null) {
            sets.add(k)
          }
        }
        if (sets.size() > 0) {
          sets.foreach(s => datas.add(new IndexObjEntity(s)))
          sets = null
        }
        if (datas.size() > 0)
          client.incrementIndexNlpCat(datas)
      }
      logInfo(s"event index successfully,total number:${datas.size()}")
    }


    //add stock industry  to index
    def indexIndustry() = {
      val industry2Product = new java.util.HashMap[String, java.util.Collection[String]]()
      val datas = new java.util.ArrayList[IndexObjEntity]()
      val products = dm.findIndustry()
      if (products != null && products.size() > 0) {
        products.map { m =>
          val c: String = if (m.get("c") != null) m.get("c").toString else null
          val w: String = if (m.get("w") != null) m.get("w").toString else null
          if (c != null) {
            if (!industry2Product.contains(c)) {
              val products = new java.util.ArrayList[String]()
              if (w != null)
                products.add(w)
              industry2Product.put(c, products)
            } else {
              if (w != null) {
                industry2Product.get(c).add(w)
              }
            }
          }
        }
        industry2Product.foreach { cp =>
          datas.add(new IndexObjEntity(cp._1, cp._2))
        }
        if (datas.size() > 0)
          client.incrementIndexNlpCat(datas)
      }
      logInfo(s"industry index successfully,total number:${datas.size()}")
    }



    //add company to index
    def indexCompany() = {
      val datas = new java.util.ArrayList[IndexObjEntity]()
      val stocks = dm.findCompanyCode()
      if (stocks != null && stocks.size() > 0) {
        stocks.foreach { dbobj =>
          var comSim: String = null
          comSim = if (dbobj.get("w") != null) dbobj.get("w").toString else null
          val comCode: String = if (dbobj.get("code") != null) dbobj.get("code").toString else null
          if (comSim != null) {
            datas.add(new IndexObjEntity(comSim, comCode))
          }
        }
      }
      if (datas.size() > 0)
        client.incrementIndexNlpCat(datas)
      logInfo(s"company index successfully,total number:${datas.size()}")
    }

    "index cat succesfully!"
  }

  def totalIndexRun(indexName: String, typeName: String) = {
    client.totalIndexRun(indexName, typeName)
    Thread.currentThread().suspend()
  }

  def searchTopKeyWord(req: HttpServletRequest, sessionId: String, keyword: String): NiNi = {
    Util.caculateCostTime {
      Util.fluidControl({
        cacheQueryBestKeyWord(keyword, null, 1)
      }, req.getRemoteHost)

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


  def wrapShowStateAndGetByQuery(req: HttpServletRequest, query: String, showLevel: Integer, needSearch: Int): NiNi = {
    Util.caculateCostTime {
      Util.fluidControl({
        showStateAndGetByQuery(query, showLevel, needSearch)
      }, req.getRemoteHost)

    }
  }


  def wrapCleanRedisByNamespace(namespace: String): NiNi = {
    Util.caculateCostTime {
      cleanRedisByNamespace(namespace)
    }
  }

  def wrapCleanQueryByGraphKeys(nodes: java.util.Set[String]): NiNi = {
    Util.caculateCostTime {
      cleanQueryByGraphKeys(nodes)
    }
  }

  def cleanQueryByGraphKeys(nodes: java.util.Set[String]): String = {
    BizeEsInterface.cleanRedisByNamespace(cleanNameSpace)
  }

  /**
    *
    * @param query
    * @return
    */
  def showStateAndGetByQuery(query: String, showLevel: Integer, needSearch: Int): Result = {
    val state = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
    if (state != null) {
      val imutableState = state.clone()
      val currentState = imutableState.getCurrentState
      if (currentState == 0) {
        // triggerQuery(query, showLevel, needSearch)
        synTriggerQuery(query, showLevel, needSearch)
      }
      else {
        val isFinished = imutableState.getFinished
        if (isFinished == FinshedStatus.UNFINISHED) {
          imutableState.setFinished(FinshedStatus.FINISHED)
          new Result(imutableState)
        } else {
          val result = new Result(imutableState, cacheQueryBestKeyWord(query, showLevel, needSearch))
          result
        }
      }

    } else {
      //triggerQuery(query, needSearch)
      synTriggerQuery(query, showLevel, needSearch)
    }

  }

  def synTriggerQuery(query: String, showLevel: Integer, needSearch: Int): Result = {
    var resultObj = new Result(new ProcessState(0, 1))
    var result = cacheQueryBestKeyWord(query, showLevel, needSearch)
    val state = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
    if (state != null) {
      val state = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
      if (state.getFinished == FinshedStatus.UNFINISHED) state.setFinished(FinshedStatus.FINISHED)
      resultObj = new Result(state, result)
    } else if (state == null && result != null) {
      result = queryBestKeyWord(null, query, showLevel, false, needSearch)
      var newState = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
      if (newState == null) newState = new ProcessState(0, 1)
      if (newState.getFinished == FinshedStatus.UNFINISHED) newState.setFinished(FinshedStatus.FINISHED)
      resultObj = new Result(newState, result)
    }
    resultObj
  }

  def triggerQuery(query: String, showLevel: Integer, needSearch: Int): Result = {
    val state = new ProcessState(0, 0)
    conf.waiter.post(Request(query, showLevel, needSearch))
    new Result(state)
  }

  /**
    *
    * @param keywords
    * @return
    */
  def indexData(sessionId: String, originQuery: String, keywords: java.util.Collection[String]): Boolean = {
    val resutlt = client.incrementIndex(graphIndexName, graphTypName, keywords)
    addBloomFilter()
    cleanRedisByNamespace(cleanNameSpace)
    resutlt
  }

  def indexDataWithRw(keywords: java.util.Collection[IndexObjEntity]): Boolean = {
    val result = client.incrementIndexWithRw(graphIndexName, graphTypName, keywords)
    addBloomFilter()
    cleanRedisByNamespace(cleanNameSpace)
    result
  }

  def cacheQueryBestKeyWord(query: String, showLevel: Integer, needSearch: Int): AnyRef = {
    ESSearchPageCache.cacheShowStateAndGetByQuery(query, showLevel, {
      queryBestKeyWord(null, query, showLevel, false, needSearch)
    })
  }


  def wrapDelIndexByKeywords(keywords: java.util.Collection[String]): NiNi = {
    Util.caculateCostTime {
      delIndexByKeywords(keywords)
    }
  }

  def wrapMatchAllQueryWithCount(from: Int, to: Int): NiNi = {
    Util.caculateCostTime {
      matchAllQueryWithCount(from, to)
    }
  }

  def matchAllQueryWithCount(from: Int, to: Int): QueryEntityWithCnt = {
    val resultWithCnt = client.matchAllQueryWithCount(graphIndexName, graphTypName, from, to)
    var size = 0
    val obj = resultWithCnt._2
    if (obj != null) size = obj.size
    new QueryEntityWithCnt(from, to, resultWithCnt._1, size, resultWithCnt._2)
  }

  def wrapCount(): NiNi = {
    Util.caculateCostTime {
      count()
    }
  }

  def count(): Long = {
    client.count(graphIndexName, graphTypName)
  }

  def delIndexByKeywords(keywords: java.util.Collection[String]): Boolean = {
    client.decrementIndex(graphIndexName, graphTypName, keywords)
  }

  def requestHttpForSynonym(query: String, reqType: String = "get"): AnyRef = {
    reqType match {
      case "get" =>
        var url: String = s"${synonymUrl}${URLEncoder.encode(query, "UTF-8")}"
        var httpResp: CloseableHttpResponse = null
        try {
          httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
          if (httpResp != null) {
            val entity: HttpEntity = httpResp.getEntity
            if (entity != null) {
              val sResponse: String = EntityUtils.toString(entity)
              val jsonArray = JSON.parseArray(sResponse)
              jsonArray
            } else null
          } else null
        }
        catch {
          case e: IOException => {
            logError("request synonym failed!", e)
            null
          }
        } finally {
          if (httpResp != null)
            HttpClientUtils.closeQuietly(httpResp)
        }
      case _ =>
        null
    }
  }

  /**
    *
    * @param query
    * @param reSearch
    * @return
    */
  def queryBestKeyWord(sessionId: String, query: String, showLevel: Integer, reSearch: Boolean = false, needSearch: Int): AnyRef = {
    var keyword = query
    if (keyword == null || keyword.trim.equalsIgnoreCase("")) {
      logError("keyword can't be null")
      return returnNoData
    }
    addSynonym
    //query rewrite
    //add synonym
    def addSynonym() = {
      try {
        val synonyms = requestHttpForSynonym(keyword)
        if (synonyms != null) {
          val synonymArray = synonyms.asInstanceOf[JSONArray]
          if (synonymArray.size() == 0) {
            val setSyns = coreNlp(keyword)
            if (setSyns != null && setSyns.size() > 0) {
              setSyns.foreach { kv =>
                val oj = requestHttpForSynonym(kv)
                if (oj != null) {
                  val synonymArray = oj.asInstanceOf[JSONArray]
                  keyword += queryExpandBySynonym(synonymArray)
                }
              }
            }
          } else {
            //add synonym
            keyword += queryExpandBySynonym(synonymArray)
          }
        }
      } catch {
        case e: Exception =>
          log.error("synonym failed", e)
      }
    }


    updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.UNFINISHED)

    if (needSearch != 1) {

      val result = wrapRequestNlp(sessionId, showLevel, keyword, keyword)
      updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
      return result
    }


    if (conf.bloomFilter.mightContain(keyword)) {
      var result = wrapRequestNlp(sessionId, showLevel, keyword, keyword)
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
          result = null
      }

      if (result != null) {
        updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
        return result
      }
    }


    var targetKeyword: String = null

    def totalRelevantTargetKeyWord(): Unit = {
      //pinyinFieldWithBoost,  companyFieldWithBoost, companyEnFieldWithBoost, word2vecWithBoost, word2vecRwWithBoost, comStockCodeFieldWithBoost, pinyinFieldWithBoost,relevantKwsField_kwWithBoost,keywordStringFieldWithBoost,
      val matchQueryResult1 = client.multiMatchQuery(graphIndexName, graphTypName, 0, 1, keyword, keywordFieldWithBoost)
      //val matchQueryResult1 = client.matchQuery(graphIndexName, graphTypName, 0, 1, keywordField, keyword )
      if (matchQueryResult1 != null && matchQueryResult1.length > 0) {
        val doc = matchQueryResult1.head
        val matchScore = doc.get(scoreField).toString.toFloat
        if (matchScore >= mulitiMatchRelevantKWThreshold) {
          val matchKeyWord = doc.get(keywordField).toString
          setTargetkeyword(matchKeyWord)
          logInfo(s"${keyword} have been matched according relevant,relevant score:${matchScore}  targetKeyword: $targetKeyword ,mulitiMatchRelevantKWThreshold:${mulitiMatchRelevantKWThreshold}")
        } else {
          logInfo(s"${keyword} have not been matched according relevant,we will try to word2vec relevant score:${matchScore}  targetKeyword: $targetKeyword,mulitiMatchRelevantKWThreshold:${mulitiMatchRelevantKWThreshold}")
          word2VecFromIndex
        }
      } else {
        logInfo(s"${keyword} have not been matched according relevant,we will try to word2vec relevant score:${0}  targetKeyword: $targetKeyword,mulitiMatchRelevantKWThreshold:${mulitiMatchRelevantKWThreshold}")
        word2VecFromIndex
      }
    }

    def word2VecFromIndex(): Unit = {
      val matchQueryResult1 = client.multiMatchQuery(graphIndexName, graphTypName, 0, 1, keyword.toLowerCase(), word2vecWithBoost, word2vecRwWithBoost)
      if (matchQueryResult1 != null && matchQueryResult1.length > 0) {
        val doc = matchQueryResult1.head
        val matchScore = doc.get(scoreField).toString.toFloat
        if (matchScore >= word2vecMatchRelevantKWThreshold) {
          val matchKeyWord = doc.get(keywordField).toString
          setTargetkeyword(matchKeyWord)
          logInfo(s"${keyword} have been matched according word2vec,relevant score:${matchScore}  targetKeyword: $targetKeyword ,word2vecMatchRelevantKWThreshold:${word2vecMatchRelevantKWThreshold}")
        } else {
          logInfo(s"${keyword} have not been matched according word2vec,we will try to Pinyin, relevant score:${matchScore}  targetKeyword: $targetKeyword ,word2vecMatchRelevantKWThreshold:${word2vecMatchRelevantKWThreshold}")
          justPinyinMatch
          //word2VectorProcess(keyword)
        }
      } else {
        logInfo(s"${keyword} have not been matched according word2vec,we will try to Pinyin, relevant score:${0}  targetKeyword: $targetKeyword ,word2vecMatchRelevantKWThreshold:${word2vecMatchRelevantKWThreshold}")
        justPinyinMatch
        //word2VectorProcess(keyword)
      }
    }


    def relevantTargetKeyWord(score: java.lang.Float, keyWord: String): Unit = {
      //val matchQueryResult1 = client.matchQuery(graphIndexName, graphTypName, 0, 1, keywordField, keyword)
      val matchQueryResult1 = client.multiMatchQuery(graphIndexName, graphTypName, 0, 1, keyword, keywordField, companyField, companyEnField, comStockCodeField, keywordField)
      if (matchQueryResult1 != null && matchQueryResult1.length > 0) {
        val doc = matchQueryResult1.head
        val matchScore = doc.get(scoreField).toString.toFloat
        if (matchScore > matchScoreThreshold) {
          if (score != null && matchScore < score / 1000) {
            setTargetkeyword(keyWord)
            logInfo(s"${keyword} have been matched according pinyin and relevant,relevant score:${matchScore} pinyin score:${score}  targetKeyword: $targetKeyword")
          } else {
            val matchKeyWord = doc.get(keywordField).toString
            setTargetkeyword(matchKeyWord)
            logInfo(s"${keyword} have been matched according relevant,relevant score:${matchScore}  targetKeyword: $targetKeyword")
          }
        } else if (score != null && score >= pinyinScoreThreshold) {
          setTargetkeyword(keyWord)
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
      val matchQueryResult = client.matchQuery(graphIndexName, graphTypName, 0, 1, relevantKwsField, keyword)
      if (matchQueryResult != null && matchQueryResult.length > 0) {
        val doc = matchQueryResult.head
        val rlvKWScore = doc.get(scoreField).toString.toFloat
        val matchKeyWord = doc.get(keywordField).toString
        if (rlvKWScore >= matchRelevantKWThreshold) {
          setTargetkeyword(matchKeyWord)
          logInfo(s"${keyword} have been matched by relevantKw fuzzyly,relevant score:${rlvKWScore}} targetKeyword: $targetKeyword")
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
        val matchQueryResult1 = client.matchQuery(graphIndexName, graphTypName, 0, 1, keywordField, relevantWords.mkString(" "))
        if (matchQueryResult1 != null && matchQueryResult1.length > 0) {
          val doc = matchQueryResult1.head
          val word2VecScore = doc.get(scoreField).toString.toFloat
          val matchKeyWord = doc.get(keywordField).toString
          if (word2VecScore >= matchScoreThreshold) {
            setTargetkeyword(matchKeyWord)
            logInfo(s"${keyword} have been matched by word2vec,relevant score:${word2VecScore},similarity keywords: ${relevantWords.mkString(" ")} targetKeyword: $targetKeyword")
          }
        }
      }
    }


    def pinyinMatch: Unit = {
      //pinyin query
      val pinyinResult = client.matchQuery(graphIndexName, graphTypName, 0, 1, pinyinField, keyword)
      if (pinyinResult != null && pinyinResult.length > 0) {
        val doc = pinyinResult.head
        val docScore = doc.get(scoreField).toString.toFloat
        val docKeyword = doc.get(keywordField).toString
        if (docScore > pinyinScoreThreshold && keyword.length == docKeyword.trim.length) {
          setTargetkeyword(docKeyword)
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

    def justPinyinMatch: Unit = {
      if (Util.isEnglish(keyword.trim.toLowerCase())) {
        //pinyin query
        val pinyinResult = client.matchQuery(graphIndexName, graphTypName, 0, 1, pinyinField, keyword.trim.toLowerCase())
        if (pinyinResult != null && pinyinResult.length > 0) {
          val doc = pinyinResult.head
          val docScore = doc.get(scoreField).toString.toFloat
          val docKeyword = doc.get(keywordField).toString
          if (docScore > pinyinScoreThreshold) {
            setTargetkeyword(docKeyword)
            logInfo(s"${keyword} have been matched according pinyin,pinyin score:${docScore}  targetKeyword: $targetKeyword,pinyinScoreThreshold:${pinyinScoreThreshold}")
          } else {
            logInfo(s"${keyword} have not been matched according pinyin, we will try to fetch ,pinyin score:${docScore}  targetKeyword: $targetKeyword,pinyinScoreThreshold:${pinyinScoreThreshold}")
            //totalRelevantTargetKeyWord
          }
        } else {
          logInfo(s"${keyword} have not been matched according pinyin, we will try to fetch ,pinyin score:${0}  targetKeyword: $targetKeyword,pinyinScoreThreshold:${pinyinScoreThreshold}")
          //totalRelevantTargetKeyWord
        }
      } else {
        logInfo(s"${keyword} have not been matched according pinyin, we will try to fetch ,pinyin score:${0}  targetKeyword: $targetKeyword,pinyinScoreThreshold:${pinyinScoreThreshold}")
        //totalRelevantTargetKeyWord
      }
    }

    def termFuzzyQueryByRelevantKw() = {
      //val matchQueryResult = client.matchQuery(graphIndexName, graphTypName, 0, 1, relevantKwsField_kw, keyword.toLowerCase())
      val matchQueryResult = client.matchPhraseQuery(graphIndexName, graphTypName, 0, 1, relevantKwsField_kw, keyword.toLowerCase())
      if (matchQueryResult != null && matchQueryResult.length > 0) {
        val doc = matchQueryResult.head
        val rlvKWScore = doc.get(scoreField).toString.toFloat
        val matchKeyWord = doc.get(keywordField).toString
        setTargetkeyword(matchKeyWord)
        logInfo(s"${keyword} have been matched  accurately by relevantKw_kw,relevant score:${rlvKWScore}} targetKeyword: $targetKeyword")
      } else {
        //relevantTargetKeyWordByRelevantKw
        totalRelevantTargetKeyWord
      }
    }


    def setTargetkeyword(matchKeyWord: String): Unit = {

      //save relation to redis by another thread

      targetKeyword = matchKeyWord
    }
    def stockCodeSearch() = {
      if (Util.isNumeric(keyword)) {
        val matchQueryResult = client.termQuery(graphIndexName, graphTypName, 0, 1, comStockCodeStringField, keyword.toLowerCase())
        if (matchQueryResult != null && matchQueryResult.length > 0) {
          val doc = matchQueryResult.head
          val rlvKWScore = doc.get(scoreField).toString.toFloat
          val matchKeyWord = doc.get(keywordField).toString
          setTargetkeyword(matchKeyWord)
          logInfo(s"${keyword} have been matched  accurately by stock code string,relevant score:${rlvKWScore}} targetKeyword: $targetKeyword")
        } else {
          termFuzzyQueryByRelevantKw
          //justPinyinMatch
        }
      } else {
        termFuzzyQueryByRelevantKw
        // justPinyinMatch
      }
    }

    def stockCodeFuzzySearch() = {
      if (Util.isNumeric(keyword)) {
        val matchQueryResult = client.matchQuery(graphIndexName, graphTypName, 0, 1, comStockCodeField, keyword.toLowerCase())
        if (matchQueryResult != null && matchQueryResult.length > 0) {
          val doc = matchQueryResult.head
          val rlvKWScore = doc.get(scoreField).toString.toFloat
          val matchKeyWord = doc.get(keywordField).toString
          setTargetkeyword(matchKeyWord)
          logInfo(s"${keyword} have been matched  accurately by stock code,relevant score:${rlvKWScore}} targetKeyword: $targetKeyword")
        } else {
          justPinyinMatch
        }
      } else {
        justPinyinMatch
      }
    }

    def termAccurateQuery() = {
      val mustResult = client.boolMustQuery(graphIndexName, graphTypName, 0, 1, keywordStringField, keyword)
      if (mustResult != null && mustResult.length > 0) {
        setTargetkeyword(mustResult.head.get(keywordField).toString)
        logInfo(s"${keyword} have been matched  accurately  targetKeyword: $targetKeyword")
      } else {
        termFuzzyQueryByRelevantKw
      }
    }


    // term query
    // termAccurateQuery()
    //totalRelevantTargetKeyWord()
    //termFuzzyQueryByRelevantKw()
    stockCodeSearch




    if (targetKeyword == null && !reSearch) {
      //no data, save status to redis and request to trigger crawler with fetch data asynchronous
      if (!switchCrawler.equalsIgnoreCase("off")) {
        updateState(sessionId, keyword, KnowledgeGraphStatus.NLP_PROCESS, FinshedStatus.UNFINISHED)
        val result = realTimeCrawler(keyword)
        if (result == null) {
          updateState(sessionId, keyword, KnowledgeGraphStatus.FETCH_PROCESS, FinshedStatus.FINISHED)
          return null
        } else
          return queryBestKeyWord(sessionId, keyword, showLevel, true, needSearch = 1)
      } else {
        updateState(sessionId, keyword, KnowledgeGraphStatus.FETCH_PROCESS, FinshedStatus.FINISHED)
        null
      }
    } else if (targetKeyword == null && reSearch) {
      updateState(sessionId, keyword, KnowledgeGraphStatus.NLP_PROCESS, FinshedStatus.FINISHED)
      //return nodata
      return returnNoData
    } else {
      if (targetKeyword != null) {
        //request nlp
        //found keyword
        var result = wrapRequestNlp(sessionId, showLevel, keyword, targetKeyword)
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
            result = null
        }
        return result
      } else {
        updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
        return returnNoData
      }
    }


  }

  def cleanRedisByNamespace(namespace: String): String = {
    if (namespace != null && namespace.trim.equalsIgnoreCase(cleanNameSpace)) {
      val client = RedisClient("default")
      try {
        val sets = conf.storage.keys(s"$namespace:*")
        sets.foreach { k =>
          try {
            client.del(k)
          } catch {
            case e: Exception =>
              client.del(k)
          }
        }
        conf.stateCache.cleanAll()
        logInfo(s"clean ${namespace} from redis successfully!")
        s"clean ${namespace} from redis successfully"
      } catch {
        case e: Exception =>
          var result = s"need clean ${namespace} from redis again"
          logInfo(result, e)
          result = cleanRedisByNamespace(cleanNameSpace)
          result
      } finally {
        client.close()
      }
    } else {
      conf.stateCache.cleanAll()
      BizeEsInterface.warmCache()
      logInfo(s"clean ${namespace}, just local  cache successfully!")
      s"clean ${namespace} , just local  cache successfully"
    }
  }


  def cacheReuqestNlp(query: String, showLevel: Integer): AnyRef = {
    ESSearchPageCache.cacheRequestNlpByQuery(query, showLevel, {
      requestNlp(query, showLevel)
    })
  }


  /**
    * request nlp graph with targetKeyword
    *
    * @param query
    * @return
    */
  private def requestNlp(query: String, showLevel: Integer): AnyRef = {
    requestHttp(query, graphUrl, showLevel)
  }

  def requestHttp(query: String, httpUrl: String, showLevel: Integer, reqType: String = "get"): AnyRef = {
    reqType match {
      case "get" =>
        var url: String = s"${httpUrl}${URLEncoder.encode(query, "UTF-8")}"
        if (showLevel != null) url = url + s"&l=$showLevel"
        var httpResp: CloseableHttpResponse = null
        try {
          httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
          if (httpResp != null) {
            val entity: HttpEntity = httpResp.getEntity
            if (entity != null) {
              val sResponse: String = EntityUtils.toString(entity)
              JSON.parseObject(sResponse)
            } else null
          } else null
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
      case _ =>
        null
    }
  }


  private def wrapRequestNlp(sessionId: String, showLevel: Integer, originQuery: String, query: String): QueryData = {
    new QueryData(originQuery, query, cacheReuqestNlp(query, showLevel))
  }


  private def returnNoData(): AnyRef = {
    null
  }

  def updateState(sessionId: String, query: String, currentDate: Int, finishState: Int) = {
    val newQuery = STATE_PREFFIX + query

    if (sessionId != null)
      conf.waiter.post(UpdateState(sessionId + newQuery, new ProcessState(currentDate, finishState)))
    else
      conf.waiter.post(UpdateState(newQuery, new ProcessState(currentDate, finishState)))
  }

  private def splitWords(keyWord: String): Set[String] = {
    val terms = ToAnalysis.parse(keyWord)
    if (terms == null) return Set.empty[String]
    else {
      terms.map(_.getName).filter(_.length > 1).toSet
    }

  }


  private def realTimeCrawler(kw: String): String = {
    val cat: String = if (isEvent(kw)) QueryCatType.EVENT
    else {
      null
      QueryCatType.CSF
    }
    if (cat == null) return null
    val url: String = s"${fetchUrl}kw=${URLEncoder.encode(kw, "UTF-8")}&cat=$cat"
    logInfo(s"fetching query:${kw}")
    var httpResp: CloseableHttpResponse = null
    try {
      httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
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
      if (httpResp != null)
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
    client.delAllData(graphIndexName, graphTypName)
  }


  def wrapCleanAllFromMongoAndIndex = {
    Util.caculateCostTime {
      cleanAllFromMongoAndIndex()
    }
  }

  def cleanAllFromMongoAndIndex(): Boolean = {
    deleteAllMongoData()
    for (i <- 0 until 4)
      delAllData()
    delAllData()
  }

  def wrapDumpIndexToDisk(): NiNi = {
    Util.caculateCostTime {
      dumpIndexToDisk()
    }
  }

  def coreNlp(text: String, resultType: Int = CoreNLPSegmentType.WORDCUT): util.Set[String] = {
    val wordSet = new util.HashSet[String]()
    val partOfSpeechSet = new util.HashSet[String]()
    val nameEntity = new util.HashSet[String]()
    val set = new util.HashSet[(String, String, String)]()
    var annotation: Annotation = new Annotation(text)
    conf.pipeline.annotate(annotation)
    val sentences: util.List[CoreMap] = annotation.get(classOf[CoreAnnotations.SentencesAnnotation])
    val sentence: CoreMap = sentences.get(0)
    val tokens: util.List[CoreLabel] = sentence.get(classOf[CoreAnnotations.TokensAnnotation])
    import scala.collection.JavaConversions._
    for (token <- tokens) {
      val word: String = token.getString(classOf[CoreAnnotations.TextAnnotation])
      val pos: String = token.getString(classOf[CoreAnnotations.PartOfSpeechAnnotation])
      val ner: String = token.getString(classOf[CoreAnnotations.NamedEntityTagAnnotation])
      wordSet.add(word)
      partOfSpeechSet.add(pos)
      nameEntity.add(ner)
      logInfo(word + "\t " + pos + "\t " + ner)
    }
    resultType match {
      case CoreNLPSegmentType.WORDCUT =>
        wordSet
      case CoreNLPSegmentType.PART_OF_SPEECH_TAGGING =>
        partOfSpeechSet
      case CoreNLPSegmentType.NAMED_ENTITY_RECOGNITION =>
        nameEntity
    }
  }

  def queryExpandBySynonym(synonymArray: JSONArray): String = {
    var keyword = ""
    synonymArray.foreach { obj =>
      val jobj = obj.asInstanceOf[JSONObject]
      keyword += "," + jobj.getString("name")
    }
    return keyword
  }


  def wrapWarmCache(): NiNi = {
    Util.caculateCostTime {
      warmCache()
    }
  }


  def warm(): Unit = {
    var cnt = 0
    val obj = BizeEsInterface.matchAllQueryWithCount(0, BizeEsInterface.count().toInt)
    if (obj != null) {
      val result = JSON.toJSON(obj.getResult).asInstanceOf[JSONArray]
      var keyWord: String = null
      if (result != null) {
        result.foreach { obj =>
          var rvkw: java.util.Collection[String] = null
          val obj1 = obj.asInstanceOf[JSONObject]
          keyWord = obj1.getString("keyword")
          if (keyWord != null && !keyWord.equalsIgnoreCase("")) {
            reqestForCache(keyWord)
            cnt += 1
          }
          if (obj1.containsKey("relevant_kws")) {
            val list = obj1.getJSONArray("relevant_kws").toList
            val sttList = list.map(_.toString)
            rvkw = sttList
          }
          if (rvkw != null && keyWord.size > 0) {
            rvkw.filter(x => x != null && !x.equalsIgnoreCase("")).foreach { x =>
              reqestForCache(x)
              cnt += 1
            }
          }
          if (obj1.containsKey("stock_code")) {
            val stockCode = obj1.getString("stock_code")
            if (stockCode != null) {
              val companyCode = stockCode.split("_")
              reqestForCache(companyCode(0))
              reqestForCache(stockCode)
            }

          }
          //Thread.sleep(1000)
        }
      }
    }
    logInfo(s"warm successfully,total keywords:${cnt}")
    thread = null
  }

  var thread: Thread = _

  val lock = new Object

  def warmCache(): String = {

    if (thread == null) thread = new Thread() {
      override def run(): Unit = {
        warm
      }
    }
    lock.synchronized {
      if (thread != null && !thread.isAlive) {
        thread.start()
        //conf.waiter.post(WarmCache())
        "we will warm cache in background"
      } else {
        "warming..."
      }
    }

  }

  def reqestForCache(keyword: String) = {
    //BizeEsInterface.queryBestKeyWord(null, keyword, null, false, 1)
    BizeEsInterface.cacheQueryBestKeyWord(keyword, null, 1)
    //val url = "http://54.222.222.172:8999/es/search/keywords/?keyword="
    //requestHttp(keyword, warmUrl)
    logInfo(s"keyword: ${keyword} warmed!")
  }

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


  def main(args: Array[String]) {
    //testTotalIndexRun
    //testQueryBestKeyWord
    //testRealTimeCrawler
    //testShowStateByQuery
    //testDecrementIndex
    //testCleanRedisByNamespace
    //testDeleteAllMongoData()
    //testDelAllData
    //testBoolMustQuery
    //testMatchQuery
    //testCount
    //testMatchAllQueryWithCount
    //testWarmCache()
    //testMultiMatchForNgram

    //testBloomFilter

    //testWildcardQuery

    // testIndexCatOfKeywords

    //testFuzzyQuery

    //testLoadEventRegexToCache

    //testqueryBestKeyWord

    // testMatchPhraseQuery()
    testaddSynonym()

  }


  def testaddSynonym() = {
    var keyword = "积蓄有没有"
    val synonyms = requestHttpForSynonym(keyword)
    if (synonyms != null) {
      val synonymArray = synonyms.asInstanceOf[JSONArray]
      if (synonymArray.size() == 0) {
        val setSyns = coreNlp(keyword)
        if (setSyns != null && setSyns.size() > 0) {
          setSyns.foreach { kv =>
            val oj = requestHttpForSynonym(kv)
            if (oj != null) {
              val synonymArray = oj.asInstanceOf[JSONArray]
              keyword += queryExpandBySynonym(synonymArray)
            }
          }
        }
      } else {
        //add synonym
        keyword += queryExpandBySynonym(synonymArray)
      }
    }
    println(keyword)
  }


  def testMatchPhraseQuery() = {
    val keyword = "vr"

    val result = client.matchPhraseQuery(graphIndexName, graphTypName, 0, 10, relevantKwsField_kw, keyword)
    println(result)

    val result2 = client.matchPhraseQuery(graphIndexName, graphTypName, 0, 10, relevantKwsField, keyword)
    println(result2)

    val result1 = client.matchQuery(graphIndexName, graphTypName, 0, 10, relevantKwsField_kw, keyword)
    println(result1)

  }

  def testqueryBestKeyWord() = {
    val result = queryBestKeyWord(null, "中国中冶", 1, true, needSearch = 1)
    println(result)
  }


  def testLoadEventRegexToCache() = {
    val query = "我遭遇了金融危机"
    val startTime = System.currentTimeMillis()
    println("isEvent:" + isEvent(query))
    val endTime = System.currentTimeMillis()
    println("cost:" + (endTime - startTime))

  }

  def testFuzzyQuery() = {
    val preffix = "30025"
    val result = client.fuzzyQuery(graphIndexName, graphTypName, 0, 1, comStockCodeField, preffix)
    println(result.toString)
  }

  def testWildcardQuery() = {
    val preffix = "300*"
    val result = client.wildcardQuery(graphIndexName, graphTypName, 0, 1, comStockCodeField, preffix)
    println(result.toString)
  }


  def testIndexCatOfKeywords = {
    indexCatOfKeywords
  }

  def testBloomFilter() = {
    conf.bloomFilter.add("soledede")
    conf.bloomFilter.add("a")
    conf.bloomFilter.add("b")
    println(conf.bloomFilter.mightContain("soledede"))
  }


  def testMultiMatchForNgram() = {
    val result = client.multiMatchQuery(graphIndexName, graphTypName, 0, 10, "6_SZ_EQ", "keyword", "stock_code", "s_zh")
    println(result)
  }


  def testWarmCache() = {
    for (i <- 0 until 10) {
      BizeEsInterface.warmCache()
      Thread.sleep(1000)
    }

  }

  def testMatchQuery() = {
    val keyword = "中国中冶的"
    val result = client.matchQuery(graphIndexName, graphTypName, 0, 10, keywordField, keyword)
    println(result)
  }

  def testBoolMustQuery() = {
    //val keyword = "\""+"国旅联合"+"\""
    val keyword = "aabc chain"
    val result = client.boolMustQuery(graphIndexName, graphTypName, 0, 10, relevantKwsField_kw, keyword)
    println(result)
  }

  def testMatchAllQueryWithCount() = {
    val result = BizeEsInterface.matchAllQueryWithCount(0, 1828)
    println(result)
    val fOut = new FileOutputStream("D:/java/es_index")
    val ser = JavaSerializer(new SolrClientConf()).newInstance()
    val outputStrem = ser.serializeStream(fOut)
    outputStrem.writeObject(result)
    outputStrem.flush()
    outputStrem.close()

    val fIput = new FileInputStream("D:/java/es_index");
    val inputStream = ser.deserializeStream(fIput)
    val obj = inputStream.readObject[QueryEntityWithCnt]()
    inputStream.close()
    println(obj)

  }

  private def testCount() = {
    println(BizeEsInterface.count())
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
    val result = BizeEsInterface.queryBestKeyWord(null, "百度", null, false, 1)
    println(result)
  }

  private def testTotalIndexRun() = {
    val indexName = "nlp"
    val typName = "graph"
    BizeEsInterface.totalIndexRun(indexName, typName)
  }

  private def testShowStateByQuery() = {
    val query = "百度凤凰"
    val result = BizeEsInterface.showStateAndGetByQuery(query, null, 1)
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
