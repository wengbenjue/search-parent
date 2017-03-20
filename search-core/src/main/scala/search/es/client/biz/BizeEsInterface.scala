package search.es.client.biz

import java.io.{FileInputStream, FileOutputStream, IOException, PrintWriter}
import java.net.URLEncoder
import java.util
import java.util.{Calendar, Date, UUID}
import javax.servlet.http.HttpServletRequest

import com.alibaba.fastjson.{JSON, JSONArray, JSONObject}
import com.mongodb.{BasicDBList, BasicDBObject}
import edu.stanford.nlp.ling.{CoreAnnotations, CoreLabel}
import edu.stanford.nlp.pipeline.Annotation
import edu.stanford.nlp.util.CoreMap
import org.ansj.splitWord.analysis.ToAnalysis
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.utils.{HttpClientUtils, URIUtils, URLEncodedUtils}
import org.apache.http.util.EntityUtils
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import search.common.algorithm.pinyin.PinyinUtils
import search.common.bloomfilter.mutable.BloomFilter
import search.common.cache.impl.LocalCache
import search.common.cache.pagecache.ESSearchPageCache
import search.common.clock.CloudTimerWorker
import search.common.config.EsConfiguration
import search.common.entity.bizesinterface.{CompanyStock, Industry, _}
import search.common.entity.news.{News, QueryResult, SortStruct}
import search.common.entity.biz.report.{ReportEntity, ResearchReport}
import search.common.entity.state.ProcessState
import search.common.http.HttpClientUtil
import search.common.listener.graph.{Request, UpdateState, WarmCache}
import search.common.redis.RedisClient
import search.common.serializer.JavaSerializer
import search.common.tool.corenlp.CoreNLPSegmentType
import search.common.util._
import search.es.client.EsClient
import search.es.client.util.{EsClientConf, QueryCatType}
import search.common.entity.searchinterface.NiNi
import search.solr.client.{SolrClient, SolrClientConf}

import scala.collection.JavaConversions._
import Numeric.Implicits._
import scala.collection.{JavaConversions, JavaConverters, mutable}
import scala.util.control.Breaks._

/**
  * Created by soledede.weng on 2016/7/28.
  */
private[search] object BizeEsInterface extends Logging with EsConfiguration {
  var conf: EsClientConf = _
  var client: EsClient = _

  var isInited = false

  var calendar = Calendar.getInstance()

  val keywordFieldWithBoost = "keyword^8"
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


  val newsSuggestField = "title"


  //=========================================================================
  //news
  val title = "title^6"
  val auth = "auth^3"
  val summary = "summary"
  val topics = "topics^2"
  val events = "events^2"
  val companys = "companys^2"
  val decayField = "create_on"


  //hl field for news
  var hlList: List[String] = null
  if (hlFields != null && !hlFields.equalsIgnoreCase("")) {
    val hlArrays = hlFields.split(",")
    if (hlArrays != null && hlArrays.length > 0) hlList = hlArrays.toList
  }

  //report_fields

  var reportFieldsList: List[String] = null
  if (report_fields != null && !report_fields.equalsIgnoreCase("")) {
    val reportFieldsArrays = report_fields.split(",")
    if (reportFieldsArrays != null && reportFieldsArrays.length > 0) reportFieldsList = reportFieldsArrays.toList
  }


  val timerPeriodSchedule = new CloudTimerWorker(name = "timerPeriodSchedule", interval = 1000 * 60 * 60 * 24, callback = () => loadEventRegexRule())

  val timerPeriodScheduleForBloomFilter = new CloudTimerWorker(name = "timerPeriodScheduleForBloomFilter", interval = 1000 * 60 * 15, callback = () => addBloomFilterFromGraph())


  val timerPeriodScheduleForLoadEventToCache = new CloudTimerWorker(name = "timerPeriodScheduleForLoadEventToCache", interval = 1000 * 60 * 60 * 22, callback = () => loadEventToCache())


  val timerPeriodScheduleForLoadGraphHotTopicCache = new CloudTimerWorker(name = "timerPeriodScheduleForLoadGraphHotTopicCache", interval = 1000 * 60 * 30, callback = () => loadTopicToCache())


  // val timerPeriodScheduleForindexNewsFromDays = new CloudTimerWorker(name = "timerPeriodScheduleForindexNewsFromDays", interval = 1000 * 60 * 60 * 24, callback = () => indexNewsFromDay(2))

  val timerPeriodScheduleForindexNewsFromMinutes = new CloudTimerWorker(name = "timerPeriodScheduleForindexNewsFromMinutes", interval = 1000 * 60 * 5, callback = () => BizUtil.indexNewsFromMinutes(conf, client, 8))

  //val timerPeriodScheduleForloadDataToDictionary = new CloudTimerWorker(name = "timerPeriodScheduleForloadDataToDictionary", interval = 1000 * 60 * 60 * 24, callback = () => BizUtil.loadDataToDictionary(conf))

  val timerPeriodScheduleFordeleteNewsByRange = new CloudTimerWorker(name = "timerPeriodScheduleFordeleteNewsByRange", interval = 1000 * 60 * 60 * 24, callback = () => BizUtil.deleteNewsByRange(client))

  //自动更新上市公司
  val timerPeriodScheduleForUpdateStockCache = new CloudTimerWorker(name = "timerPeriodScheduleForUpdateStockCache", interval = 1000 * 60 * 60 * 30, callback = () => wrapLoadCacheFromCom())


  var eventRegexRuleSets = new java.util.HashSet[String]()

  def loadEventRegexRule(): Long = {
    loadEventRegexToCache
    -1
  }

  def loadThread = new Thread {
    override def run(): Unit = {
      load()
    }
  }

  //loadThread.setDaemon(true)

  def loadStart() = {
    loadThread.start()
  }


  init()

  def init() = {
    if (!isInited) {
      isInited = true

      this.conf = new EsClientConf()
      this.conf.init()
      this.client = conf.esClient
      //load company weight cache
      loadCompanyWeightCache()
      loadTopicToCache()
      loadIndexIndustryToCache()
      loadEventToCache()
      loadCache
      loadCacheFromCom
      loadStart

      /**
        * Trie Node
        */
      val timerPeriodScheduleForDumpTrie = new CloudTimerWorker(name = "timerPeriodScheduleForDumpTrie", interval = 1000 * 60 * 30, callback = () => BizeEsInterfaceUtils.dumpTrieToDisk(conf))
      timerPeriodScheduleForDumpTrie.startUp()
      BizeEsInterfaceUtils.readDumpTrieFromDisk(conf)
      BizeEsInterfaceUtils.dumpTrieToDisk(conf)
    }


  }


  def load() = {
    //addBloomFilter
    addBloomFilterFromGraph()
    cleanRedisByNamespace(cleanNameSpace)
    //indexCatOfKeywords
    loadEventRegexToCache()
    loadTopicToCache()
    timerPeriodSchedule.startUp()
    timerPeriodScheduleForBloomFilter.startUp()
    timerPeriodScheduleForLoadEventToCache.startUp()
    timerPeriodScheduleForLoadGraphHotTopicCache.startUp()
    //timerPeriodScheduleForindexNewsFromDays.startUp()
    timerPeriodScheduleForindexNewsFromMinutes.startUp()
    timerPeriodScheduleFordeleteNewsByRange.startUp()
    //timerPeriodScheduleForloadDataToDictionary.startUp()
    timerPeriodScheduleForUpdateStockCache.startUp()


    BizUtil.loadDataToDictionary(conf)
    if (needPatchIndex) {
      indexNewsFromMongo(batchMonth)
    }

    //indexNewsFromDay(1)
    //indexNewsFromMinutes(10)

    //将公司信息写入文本文件
    BizeEsInterfaceUtils.writeCompanyToDiskByText()
    //将主题信息写入文本文件
    BizeEsInterfaceUtils.writeTopicToDiskByText()
    //将行业信息写入文本文件
    BizeEsInterfaceUtils.writeIndustryToDiskByText()


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

  def addWordToTrieNode(word: String, id: String): Unit = {
    conf.dictionary.add(word.trim.toUpperCase(), id)
  }

  def addWordToGraphTrieNode(word: String, id: String): Unit = {
    conf.graphDictionary.add(word.trim.toUpperCase(), id)
  }

  def loadCompanyWeightCache() = {
    val comJsonObj = BizUtil.requestHttpByURL(companyWeightUrl)
    val comanyArray: JSONArray = comJsonObj.getJSONArray("message")
    if (comanyArray != null && comanyArray.size() > 0) {
      comanyArray.foreach { com =>
        try {
          val comJobj = com.asInstanceOf[JSONObject]
          val comanyName = comJobj.getString("com")
          var weight: Double = comJobj.getDouble("w")
          // weight = Util.decimalFormat2(weight)
          weight = (weight * 10000).toInt / 10000.0
          LocalCache.conmpanyWeightCache(comanyName) = weight
        } catch {
          case e: Exception =>
        }
      }
    }
  }

  def loadGraphHotTopicWeightCache(loadAll: Boolean = false): Long = {
    val topics = conf.mongoDataManager.findTopicHot(loadAll)
    if (topics != null && topics.size() > 0) {
      val currentTopics = topics.get(0)
      val topic_hots: BasicDBList = if (currentTopics.get("topic_hot") != null) currentTopics.get("topic_hot").asInstanceOf[BasicDBList] else null
      if (topic_hots != null && topic_hots.size() > 0) {
        LocalCache.topicHotWeightCache = new scala.collection.mutable.HashMap[String, Double]()
        topic_hots.foreach { t =>
          val topic = t.asInstanceOf[BasicDBObject]
          val topicName: String = if (topic.getString("topic") != null) topic.getString("topic") else null
          var weight: Double = if (topic.getDouble("hot") != null) topic.getDouble("hot") else 0.0
          if (topicName != null) {
            // weight = Util.decimalFormat2(weight)
            weight = (weight * 10000).toInt / 10000.0
            LocalCache.topicHotWeightCache(topicName) = weight
          }
        }
      }
    }

    -1
  }

  def wrapLoadCacheFromCom(): Long = {
    loadCacheFromCom()
    -1
  }

  def loadCacheFromCom(): String = {
    val stocks = conf.mongoDataManager.findCompanyCode()
    if (stocks != null && stocks.size() > 0) {
      stocks.foreach { dbobj =>
        var comSim: String = null
        comSim = if (dbobj.get("w") != null) dbobj.get("w").toString else null
        val comCode: String = if (dbobj.get("code") != null) dbobj.get("code").toString else null
        val id: String = if (dbobj.get("_id") != null) dbobj.get("_id").toString else null
        var simPy: String = null
        if (comSim != null) {
          simPy = PinyinUtils.getPinYin(comSim, false)
          val company = new CompanyStock(id, comCode, comSim, simPy, 0.0)
          if (LocalCache.conmpanyWeightCache.containsKey(comSim)) {
            company.setWeight(LocalCache.conmpanyWeightCache(comSim))
          }
          LocalCache.companyStockCache(id) = company
          // addWordToTrieNode(comCode, id)
          addWordToTrieNode(comSim, id)
          /*if (simPy != null && !simPy.trim.isEmpty) {
            addWordToTrieNode(simPy, id)
          }*/

          //将拼音添加到自动提示
          addWordToGraphTrieNode(simPy, id)
          //将公司简称添加到自动提示
          addWordToGraphTrieNode(comSim, id)

        }
        if (comSim != null && comCode != null) {
          LocalCache.baseStockCache(comSim) = new BaseStock(comSim, comCode)
          LocalCache.codeToCompanyNameCache(comCode) = comSim
          //LocalCache.stockNameToCodeCache(comSim) = comCode
          //将公司代码添加到自动提示
          addWordToGraphTrieNode(comCode, id)
        }

      }
      logInfo("comoany stock cached in local cache")
    }
    "comoany stock cached in local cache"
  }

  def loadEventToCache(): Long = {
    val events = conf.mongoDataManager.findEvent()
    if (events != null && events.size() > 0) {
      events.map { m =>
        val id: String = if (m.get("_id") != null) m.get("_id").toString.trim else null
        val eventName: String = if (m.get("szh") != null) m.get("szh").toString.trim else null
        val eventWeight: Double = if (m.get("w") != null) m.get("w").toString.toDouble else 0.0
        if (eventName != null && !eventName.equalsIgnoreCase("")) {
          LocalCache.eventCache(id) = new GraphEvent(id, eventName, eventWeight)
          LocalCache.eventSet += eventName
          conf.dictionary.add(eventName, id)
        }
      }
    }
    -1
  }


  def loadTopicToCache(): Long = {
    loadGraphHotTopicWeightCache()
    val topicConps = conf.mongoDataManager.findGrapnTopicConp()
    if (topicConps != null && topicConps.size() > 0) {
      topicConps.map { m =>
        val topicName: String = if (m.get("w") != null) m.get("w").toString.trim else null
        val id: String = if (m.get("_id") != null) m.get("_id").toString.trim else null
        val codeSet: BasicDBList = if (m.get("code") != null) m.get("code").asInstanceOf[BasicDBList] else null

        if (topicName != null && !topicName.equalsIgnoreCase("")) {
          val topic = new GraphTopic(id, topicName, 0.0)
          if (LocalCache.topicHotWeightCache.containsKey(topicName)) {
            topic.setWeight(LocalCache.topicHotWeightCache(topicName))
          }
          LocalCache.topicCache(id) = topic
          conf.dictionary.add(topicName, id)
          logInfo(s"添加Topic到前缀树，topicName:${topicName}")
          //添加code到topip集合的缓存映射
          if (codeSet != null && codeSet.size() > 0) {
            BizeEsInterfaceUtils.cacheTopicSetBystockCodeAndCachStocksByTopic(codeSet.map(_.toString), topicName)
          }
        }
      }
    }
    -1
  }


  def loadIndexIndustryToCache() = {
    val industryJsonObj = BizUtil.requestHttpByURL(industryWeightUrl)
    val industryArray: JSONArray = industryJsonObj.getJSONArray("message")
    if (industryArray != null && industryArray.size() > 0) {
      industryArray.foreach { industry =>
        try {
          val id = UUID.randomUUID().toString
          val industryJobj = industry.asInstanceOf[JSONObject]
          val indName = industryJobj.getString("ind")
          val weitht = industryJobj.getDouble("w")
          LocalCache.industryCache(id) = new Industry(id, indName, weitht)
          conf.dictionary.add(indName, id)
        } catch {
          case e: Exception =>
        }
      }
    }
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


  def addBloomFilterFromGraph(): Long = {

    var cnt = 0
    val objJson = BizUtil.requestHttpByURL(graphNodeDataUrl)
    if (objJson != null) {
      val nodes = objJson.getJSONArray("message")
      if (nodes != null && nodes.size() > 0) {
        conf.bloomFilter = BloomFilter[String](Constants.GRAPH_KEYWORDS_BLOOMFILTER_KEY, expectedElements, falsePositiveRate)
        nodes.foreach(k => conf.bloomFilter.add(k.toString))
      }
      cnt = nodes.size()
    }
    logInfo(s"add bloomFilter successful from graph,size: ${cnt}")
    -1
  }


  def wrapAddBloomFilter(): NiNi = {
    Util.caculateCostTime {
      //addBloomFilter()
      addBloomFilterFromGraph()
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

    //add stock event to index news.dict_news_rule
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


    //add stock industry  to index  news.sens_industry
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


  def wrapIndexByReportsText(docs: java.util.Collection[ResearchReport]): NiNi = {
    Util.caculateCostTime {
      indexReportText(docs)
    }
  }

  /**
    * 研报
    *
    * @return
    */
  def indexReportText(docs: java.util.Collection[ResearchReport]): Boolean = {
    if (docs != null && docs.size() > 0) {
      client.addDocumentsWithMultiThreading(research_report_index_name, research_report_type_name, docs.map(Util.convertBean(_)))
    } else false
  }


  def wrapIndexByReports(docs: java.util.Collection[java.util.Map[String, Object]]): NiNi = {
    Util.caculateCostTime {
      indexReport(docs)
    }
  }

  /**
    * 研报
    *
    * @return
    */
  def indexReport(docs: java.util.Collection[java.util.Map[String, Object]]): Boolean = {
    if (docs != null && docs.size() > 0) {
      client.addDocumentsWithMultiThreading(research_report_index_name, research_report_type_name, docs)
    } else false
  }

  def wrapIndexNews(news: util.Collection[News]): NiNi = {
    Util.caculateCostTime {
      indexNews(news)
    }
  }


  def indexNews(news: util.Collection[News]): Boolean = {
    if (news != null && news.size() > 0) {
      val newsMapList = news.map(Util.convertBean(_))
      client.addDocumentsWithMultiThreading(newsIndexName, newsTypName, newsMapList)
    } else false
  }

  def indexByKeywords(sessionId: String, originQuery: String, keywords: java.util.Collection[String]): NiNi = {
    Util.caculateCostTime {
      indexData(sessionId, originQuery, keywords)
    }
  }


  //index by days
  def indexNewsFromDay(days: Int = 1): Long = {
    var indexCalendar = Calendar.getInstance()
    var day = days
    if (days <= 0 || days > 24) day = 2
    val toNowDay = new Date()
    indexCalendar.setTime(toNowDay)
    indexCalendar.add(Calendar.DATE, -day)
    val formNowDay = indexCalendar.getTime()
    println(s"fromDay:${formNowDay},toDay:${toNowDay}")
    BizUtil.findAndIndexNews(conf, client, formNowDay, toNowDay)
    -1
  }

  /**
    * 多线程建立新闻索引
    */
  def indexNewsFromMongo(topMonth: Int = 1) = {
    try {
      var calendar = Calendar.getInstance()
      for (i <- 0 until topMonth) {
        val currentDate = new Date()
        calendar.setTime(currentDate)
        calendar.add(Calendar.MONTH, -(i + 1))
        val formNowMonth = calendar.getTime()

        calendar.setTime(currentDate)
        calendar.add(Calendar.MONTH, -i)
        val toNowMonth = calendar.getTime()
        logInfo(s"index top ${(i + 1)} to ${i} month,date:formNowMonth:${formNowMonth},toNowMonth:${toNowMonth}")
        indexNewsMulti(formNowMonth, toNowMonth)
        //Thread.sleep(1000 * 60 * 1)
      }
    } catch {
      case e: Exception => logError("index by month failed", e)
    }
    //NewsUtil.writeAuthToFile(NewsUtil.authSet)
  }

  private def indexNewsMulti(formNowMonth: Date, toNowMonth: Date): AnyVal = {
    for (i <- 1 to 8) {
      calendar.setTime(formNowMonth)
      calendar.add(Calendar.DAY_OF_MONTH, (i - 1) * 4)
      val formNowDay = calendar.getTime()


      var toNowDay = toNowMonth
      if (i != 8) {
        calendar.setTime(formNowMonth)
        calendar.add(Calendar.DAY_OF_MONTH, i * 4)
        toNowDay = calendar.getTime()
      }

      println(s"i=${i}开始时间：${formNowDay},结束时间${toNowDay}")

      BizUtil.findAndIndexNews(conf, client, formNowDay, toNowDay)
      //Thread.sleep(1000 * 60 * 3)
    }

    /*val news = conf.mongoDataManager.findHotNews(formNowMonth, toNowMonth)
     val newsMapList = NewsUtil.newsToMapCollection(news)
     if (newsMapList != null && newsMapList.size() > 0) client.addDocumentsWithMultiThreading(newsIndexName, newsTypName, newsMapList)*/
  }


  private def indexNewsMultiByDate(formNowDay: Date, toNowDay: Date): AnyVal = {
    println(s"fromDate:${formNowDay},toDate:${toNowDay}")
    BizUtil.findAndIndexNews(conf, client, formNowDay, toNowDay)
  }

  //从pdf分析出的文本文件建立索引
  def indexFromPdf() = {
    PublicAnnounUtil.loadAllPdfTxt(client)

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


  def wrapPrefix(word: String, maxLengthPerType: Int = 5): NiNi = {
    Util.caculateCostTime {
      prefix(word, maxLengthPerType)
    }
  }

  //自动完成
  def prefix(word: String, maxLengthPerType: Int = 5): AutoCompleteResult = {
    val autoCompleteResult = new AutoCompleteResult()
    val wordAndIdList = conf.graphDictionary.prefixWitId(word.trim.toUpperCase())
    if (wordAndIdList == null || wordAndIdList.size() == 0) return autoCompleteResult
    val companyStockList = new util.ArrayList[CompanyStock]()
    val industryList = new util.ArrayList[Industry]()
    val eventList = new util.ArrayList[GraphEvent]()
    val topicList = new util.ArrayList[GraphTopic]()
    val tmpHashSet = new util.HashSet[String]()
    wordAndIdList.foreach { wordAndId =>
      val id = wordAndId._2
      if (id == null) return autoCompleteResult
      if (!tmpHashSet.contains(id)) {
        tmpHashSet.add(id)
        if (LocalCache.companyStockCache.containsKey(id.trim))
          companyStockList.add(LocalCache.companyStockCache(id.trim))
        if (LocalCache.industryCache.containsKey(id.trim))
          industryList.add(LocalCache.industryCache(id.trim))
        if (LocalCache.eventCache.containsKey(id.trim))
          eventList.add(LocalCache.eventCache(id.trim))
        if (LocalCache.topicCache.containsKey(id.trim))
          topicList.add(LocalCache.topicCache(id.trim))
      }
    }
    val companyTopK = companyStockList.sortBy(_.getWeight)(Ordering[java.lang.Double].reverse).take(5)
    val industryTopK = industryList.sortBy(_.getWeight)(Ordering[java.lang.Double].reverse).take(5)
    val eventTopK = eventList.sortBy(_.getWeight)(Ordering[java.lang.Double].reverse).take(5)
    val topicTopK = topicList.sortBy(_.getWeight)(Ordering[java.lang.Double].reverse).take(5)
    autoCompleteResult.setCompany(companyTopK)
    autoCompleteResult.setIndustry(industryTopK)
    autoCompleteResult.setEvent(eventTopK)
    autoCompleteResult.setTopic(topicTopK)
    autoCompleteResult
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
          imutableState.setCurrentState(KnowledgeGraphStatus.FETCH_PROCESS)
          imutableState.setFinished(FinshedStatus.FINISHED)
          new Result(imutableState, cacheQueryBestKeyWord(query, showLevel, needSearch))
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
    var resultObj = new Result(new ProcessState(1, 1))
    var result = cacheQueryBestKeyWord(query, showLevel, needSearch)
    val state = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
    if (state != null) {
      val state = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
      if (state.getFinished == FinshedStatus.UNFINISHED) {
        state.setFinished(FinshedStatus.FINISHED)
        state.setCurrentState(KnowledgeGraphStatus.FETCH_PROCESS)
      }
      resultObj = new Result(state, result)
    } else if (state == null && result != null) {
      result = queryBestKeyWord(null, query, showLevel, false, needSearch)
      var newState = conf.stateCache.getObj[ProcessState](STATE_PREFFIX + query)
      if (newState == null) newState = new ProcessState(1, 1)
      if (newState.getFinished == FinshedStatus.UNFINISHED) {
        newState.setCurrentState(KnowledgeGraphStatus.FETCH_PROCESS)
        newState.setFinished(FinshedStatus.FINISHED)
      }
      resultObj = new Result(newState, result)
    }
    resultObj
  }

  def triggerQuery(query: String, showLevel: Integer, needSearch: Int): Result = {
    val state = new ProcessState(1, 1)
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


  /**
    * 研报删除
    *
    * @param ids
    * @return
    */
  def wrapDelReportByIds(ids: java.util.Collection[String]): NiNi = {
    Util.caculateCostTime {
      delReportByIds(ids)
    }
  }

  /**
    * 研报删除
    *
    * @param ids
    * @return
    */
  def delReportByIds(ids: java.util.Collection[String]): Boolean = {
    delByIds(research_report_index_name, research_report_type_name, ids)
  }

  /**
    * del by ids
    *
    * @param indexName
    * @param typeName
    * @param ids
    * @return
    */
  def delByIds(indexName: String, typeName: String, ids: java.util.Collection[String]): Boolean = {
    client.delByIds(indexName, typeName, ids)
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
    * @param from
    * @param offset
    * @return
    */
  def wrapQueryResearchReport(query: String, from: Int, offset: Int, analyzer: String): NiNi = {
    Util.caculateCostTime {
      queryResearchReport(query, from, offset,analyzer)
    }
  }

  /**
    * 搜索研报
    *
    * @param query
    * @param from
    * @param offset
    * @param analyzer
    * @return
    */
  def queryResearchReport(query: String, from: Int, offset: Int, analyzer: String): ReportEntity = {
    /* val queryResult = new QueryResult()
     val (count, result) = client.searchQbWithFilterAndSortsWithSuggest(research_report_index_name, research_report_type_name,
       from, offset, null, null, query, null, null, null, queryResult, aggs = null,"id","imgpath","upt","image.p","image.dec","image.t","image.n")
     queryResult.setCount(Integer.valueOf(count.toString))
     queryResult.setResult(result)*/
    if (query == null || "".equalsIgnoreCase(query.trim)) {
      val all = client.matchAllQueryWithCount(research_report_index_name, research_report_type_name,
        from, offset)
      val resultAll = new ReportEntity(all._2, all._1)
      return resultAll
    }
    //"id", "imgpath", "upt", "image_p", "image_dec", "image_t", "image_n","image_url","image_img_id"
    val result = client.multiMatchQuery(research_report_index_name, research_report_type_name,
      from, offset, query,analyzer, reportFieldsList: _*)
    val cnt = client.count(research_report_index_name, research_report_type_name)
    client.matchAllQueryWithCount(research_report_index_name, research_report_type_name,
      from, offset)
    new ReportEntity(result, cnt)
    //wordCounts(queryResult)

    //自定义高亮
    // userHl(query, queryResult)
    // queryResult
  }


  /**
    *
    * @param query
    * @param from
    * @param to
    * @param leastTopMonth
    * @param sorts
    * @return
    */
  def wrapQueryNews(query: String, from: Int, to: Int, leastTopMonth: Int, sort: String, order: String, sorts: java.util.Map[String, String], needHl: Int, analyzer: String): NiNi = {
    Util.caculateCostTime {
      queryNews(query, from, to, leastTopMonth, sort, order, sorts, needHl, analyzer)
    }
  }


  def queryNews(query: String, from: Int, to: Int, leastTopMonth: Int): QueryResult = {
    queryNews(query, from, to, leastTopMonth, sort = null, order = null, sorts = null, analyzer = null)
  }

  /**
    * 新闻搜索
    *
    * @param query
    * @param from
    * @param to
    * @param leastTopMonth
    * @param sorts
    * @param analyzer
    * @return
    */
  def queryNews(query: String, from: Int, to: Int, leastTopMonth: Int, sort: String, order: String, sorts: java.util.Map[String, String], needHl: Int = 0, analyzer: String): QueryResult = {
    val queryResult = new QueryResult()

    var filter = new mutable.HashMap[String, (Object, Boolean)]()
    var topMonth = 6
    if (leastTopMonth != null && leastTopMonth > 0) topMonth = leastTopMonth
    calendar.setTime(new Date())
    calendar.add(Calendar.MONTH, -topMonth)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.HOUR, 0)
    val lowlerBounder = calendar.getTime
    filter("create_on") = ((lowlerBounder, null), true)
    var sortF: mutable.Map[String, String] = null
    if (sorts != null) {
      sortF = JavaConversions.mapAsScalaMap(sorts)
    }



    if (sortF == null) {
      if (sort != null) {
        var orderNew = order
        if (order == null) orderNew = "desc"
        sortF = new mutable.HashMap[String, String]()
        sortF(sort.trim) = orderNew.trim
      }
    }

    //hl
    //val hlList = List("title","summary")
    //val hlList = List("title")
    val companyAggs = AggregationBuilders.terms("companys").field("companys.companys_string").size(10).order(Terms.Order.count(false))
    val eventAggs = AggregationBuilders.terms("events").field("events.events_string").size(30).order(Terms.Order.count(false))
    val topicAggs = AggregationBuilders.terms("topics").field("topics.topics_string").size(10).order(Terms.Order.count(false))

    val aggsSeq = List(companyAggs, eventAggs, topicAggs)

    var hlFields = hlList
    if (needHl != 1) hlFields = null
    if (query == null) hlFields = hlList
    else if (query != null && needHl == 1) hlFields = hlList
    else hlFields = null
    val (count, result) = client.searchQbWithFilterAndSortsWithSuggest(newsIndexName, newsTypName,
      from, to, filter, sortF, query, decayField, newsSuggestField, hlFields, queryResult, aggs = aggsSeq, analyzer,
      title, auth, summary, topics, events, companys)
    queryResult.setCount(Integer.valueOf(count.toString))
    queryResult.setResult(result)

    wordCounts(queryResult)

    //自定义高亮
    userHl(query, queryResult)
    queryResult
  }


  private def userHl(query: String, queryResult: QueryResult): Unit = {
    try {
      if (query != null && !"".equalsIgnoreCase(query)) {
        val terms = ToAnalysis.parse(query)
        if (terms != null) {
          var keyWords = terms.map(_.getName).filter(_.length > 1).toSet
          if (keyWords != null && keyWords.size > 0)
            queryResult.setHlWords(keyWords)
        }
      }
    } catch {
      case e: Exception => logError("hl failed", e)
    }
  }

  private def wordCounts(queryResult: QueryResult): Unit = {
    try {
      if (queryResult.getWordCounts != null && !queryResult.getWordCounts.isEmpty) {
        if (queryResult.getWordCounts.containsKey("companys")) {
          var companyMap: util.LinkedHashMap[String, java.lang.Double] = queryResult.getWordCounts.get("companys")
          val newCompanMap = new util.LinkedHashMap[String, java.lang.Double]
          val sortCompanys = new util.ArrayList[SortStruct]()
          val sumCnt = companyMap.map(_._2.toInt).sum
          companyMap.foreach { case (company, count) =>
            if (LocalCache.baseStockCache.containsKey(company)) {
              val baseStock = LocalCache.baseStockCache.getOrElse(company, null)
              if (baseStock != null) {
                //val code = baseStock.getComCode.replaceAll("_\\S*","")
                val code = baseStock.getComCode
                val newComanyName = s"${company}|${code}"
                val newCompanyCount = java.lang.Double.valueOf(new java.text.DecimalFormat("#0.00").format(count / sumCnt))
                newCompanMap.put(newComanyName, newCompanyCount)
                sortCompanys.add(new SortStruct(newComanyName, newCompanyCount))
              } else {
                newCompanMap.put(company, count)
                sortCompanys.add(new SortStruct(company, count))
              }
            } else {
              newCompanMap.put(company, count)
              sortCompanys.add(new SortStruct(company, count))
            }
          }
          queryResult.getWordCounts.put("companys", newCompanMap)
          queryResult.getRvwSorts.put("companys", sortCompanys)
        }

        if (queryResult.getWordCounts.containsKey("events")) {
          var eventsMap: util.LinkedHashMap[String, java.lang.Double] = queryResult.getWordCounts.get("events")
          val sumCnt = eventsMap.map(_._2.toInt).sum
          val newEventsMap = new util.LinkedHashMap[String, java.lang.Double]
          val sortEvents = new util.ArrayList[SortStruct]()
          eventsMap.foreach { case (event, count) =>
            val events = event.split("[\\|||]{1}")
            if (events.length == 2) {
              val semanticValue = events(1)
              if (semanticValue.trim.toInt != 0) {
                //filter neuter Affective index
                val eventWeight = java.lang.Double.valueOf(new java.text.DecimalFormat("#0.00").format(count / sumCnt))
                newEventsMap.put(event, eventWeight)
                sortEvents.add(new SortStruct(event, eventWeight))
              }
            }
          }
          if (!newEventsMap.isEmpty)
            queryResult.getWordCounts.put("events", newEventsMap)
          queryResult.getRvwSorts.put("events", sortEvents)
        }
        if (queryResult.getWordCounts.containsKey("topics")) {
          var topicsMap: util.LinkedHashMap[String, java.lang.Double] = queryResult.getWordCounts.get("topics")
          val newTopicsMap = new util.LinkedHashMap[String, java.lang.Double]
          val sortTopics = new util.ArrayList[SortStruct]()
          val sumCnt = topicsMap.map(_._2.toInt).sum
          topicsMap.foreach { case (topic, count) =>
            val topicWeights = java.lang.Double.valueOf(new java.text.DecimalFormat("#0.00").format(count / sumCnt))
            newTopicsMap.put(topic, topicWeights)
            sortTopics.add(new SortStruct(topic, topicWeights))
          }
          if (!newTopicsMap.isEmpty)
            queryResult.getWordCounts.put("topics", newTopicsMap)
          queryResult.getRvwSorts.put("topics", sortTopics)
        }
      }
    } catch {
      case e: Exception => logError("word count failed", e)
    }
  }

  /**
    * 查询图谱接口
    *
    * @param sessionId
    * @param query
    * @param showLevel
    * @param reSearch
    * @param needSearch
    * @return
    */
  def queryBestKeyWord(sessionId: String, query: String, showLevel: Integer, reSearch: Boolean = false, needSearch: Int): AnyRef = {
    var keyword = query
    if (keyword == null || keyword.trim.equalsIgnoreCase("")) {
      logError("keyword can't be null")
      return returnNoData
    }



    updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.UNFINISHED)

    if (needSearch != 1) {

      val result = wrapRequestNlp(sessionId, showLevel, keyword, keyword)
      updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
      return result
    }

    def bloomFilterQuery(keyword: String): QueryData = {
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
      result
    }

    if (conf.bloomFilter.mightContain(keyword)) {
      var result: QueryData = bloomFilterQuery(keyword)

      if (result != null) {
        updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
        return result
      }
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



    //duplicate remove
    val keywordArray = keyword.split(",")
    if (keywordArray.length > 1) {
      val keywordSetAll = keywordArray.toSet
      val keywordSet = keywordSetAll.filter(s => s != null && !s.equalsIgnoreCase("null") && !s.equalsIgnoreCase(""))
      keyword = keywordSet.mkString(",")
      for (k <- keywordSet) {
        if (conf.bloomFilter.mightContain(k.trim)) {
          val kv = k.trim
          var result: QueryData = bloomFilterQuery(kv)
          if (result != null) {
            updateState(sessionId, keyword, KnowledgeGraphStatus.SEARCH_QUERY_PROCESS, FinshedStatus.FINISHED)
            keyword = k.trim
            return result
          }
        }
      }
    }
    var targetKeyword: String = null

    def totalRelevantTargetKeyWord(): Unit = {
      //pinyinFieldWithBoost,  companyFieldWithBoost, companyEnFieldWithBoost, word2vecWithBoost, word2vecRwWithBoost, comStockCodeFieldWithBoost, pinyinFieldWithBoost,relevantKwsField_kwWithBoost,keywordStringFieldWithBoost,
      val matchQueryResult1 = client.multiMatchQuery(graphIndexName, graphTypName, 0, 1, keyword, keywordFieldWithBoost, relevantKwsFieldWithBoost, relevantKwsField_kwWithBoost)
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


  def addSynonymToGraph() = {
    addFromDic
    def addFromDic = {
      val synonmMap = conf.mongoDataManager.getSynonmDicFromNewsKeywordDict()
      if (synonmMap != null && !synonmMap.isEmpty) {
        synonmMap.foreach { s =>
          val k = s._1
          val sSet = s._2
          sSet.foreach(requestAddSynonm(k, _))
        }
      }
    }
    addFromSougou
    def addFromSougou() = {

    }


  }


  def requestAddSynonm(query: String, synonym: String, reqType: String = "get"): Int = {
    reqType match {
      case "get" =>
        var url: String = s"${synonymAddUrl}${URLEncoder.encode(query, "UTF-8")}/${URLEncoder.encode(synonym, "UTF-8")}"
        var httpResp: CloseableHttpResponse = null
        try {
          httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null)
          if (httpResp != null) {
            val entity: HttpEntity = httpResp.getEntity
            if (entity != null) {
              val sResponse: String = EntityUtils.toString(entity)
              // val jsonObj = JSON.parseObject(sResponse)
              println(sResponse)
              0
            } else -1
          } else -1
        }
        catch {
          case e: IOException =>
            logError("request synonym add failed!", e)
            -1
        } finally {
          if (httpResp != null)
            HttpClientUtils.closeQuietly(httpResp)
        }
      case _ =>
        -1
    }
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


  /**
    * 删除指定索引type
    * @return
    */
  def warmpDeleteAllData(index: String,typeName: String): NiNi = {
    Util.caculateCostTime {
      delAllDataByType(index,typeName)
    }
  }

  /**
    * 清除研报所有数据
    * @return
    */
  def warmpCleanResearchReport(): NiNi = {
    Util.caculateCostTime {
      cleanResearchReport
    }
  }
  def cleanResearchReport(): Boolean={
    delAllDataByType(research_report_index_name,research_report_type_name)
  }

  def delAllData(): Boolean = {
    conf.graphDictionary.clear()
    client.delAllData(graphIndexName, graphTypName)
  }

  def delAllDataByType(index: String,typeName: String): Boolean = {
    client.delAllData(index, typeName)
  }

  def deleIndex(indexName: String, graphTypeName: String): Boolean = {
    client.delAllData(indexName, graphTypeName)
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


  def filterGraphNodes(): GraphNodes = {
    val allNodeJsonObj = BizUtil.requestHttpByURL(graphNodeDataUrl)
    if (allNodeJsonObj != null) {
      val nodes = allNodeJsonObj.getJSONArray("message")
      if (nodes != null && nodes.size() > 0) {
        val synoynms = conf.mongoDataManager.getSynonmDicFromSynonymWords()
        val newNodes = nodes.filter { n =>
          val nK = n.toString.trim
          !synoynms.containsKey(nK)
        }
        return new GraphNodes(newNodes.toList)
      }
    }
    return new GraphNodes(allNodeJsonObj.toList)
  }


  def reqestForCache(keyword: String) = {
    //BizeEsInterface.queryBestKeyWord(null, keyword, null, false, 1)
    BizeEsInterface.cacheQueryBestKeyWord(keyword, null, 1)
    //val url = "http://54.222.222.172:8999/es/search/keywords/?keyword="
    //requestHttp(keyword, warmUrl)
    logInfo(s"keyword: ${keyword} warmed!")
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
    //testaddSynonym()
    //testAddSynonmToGraph()
    // testAddBloomFilterFromGraph
    //testFilterGraphNodes

    // testTrieNode()


    // testSearchQbWithFilterAndSorts()
    //testSearchQbWithFilterAndSortsWithDecayAndSearch

    //testDeleteAllIndexData()


    // testIndexFromPdf()
    //testIndexNewsFromMongo()
    //indexNewsFromMinutes(5)

    //indexNewsFromDay(2)
    //testLoadDataToDictionary

    //BizUtil.deleteNewsByRange(client)

    //testIndexReport

    testQueryResearchReport
  }


  def testQueryResearchReport(): Unit = {
    var result = queryResearchReport("test", 0, 10,"ik")
    println(result)
  }

  def testIndexReport = {
    val reportList = new util.ArrayList[java.util.Map[String, Object]]()
    val map = new util.HashMap[String, Object]()
    map.put("id", "1")
    map.put("imgpath", "test")
    reportList.add(map)
    // reportList.add(new ReportEntity("北方微电子公司的主要产品及其应用领域", "\"同时，配套资金获大基金等国有资产机构认购，说明大基金等强烈看好公司的发展前景，公司有望获得国家更大力度的扶持 在国家政策的大力扶持下，半导体产业链向大陆转移趋势明确，并购海外优质资产成为快速发展壮大本土产业的主要选项之一 此次七星电子并购北方微电子，强强联合，成为A股最具竞争力的半导体设备厂商 2013年以来，在集成电路设计和封装测试领域发生多起国内资本（或企业）收购海外公司的案例，并取得不错的效果 我们认为，作为国内半导体产业最为薄弱的设备环节，收购海外公司成为国内厂商发展壮大的“捷径” 根据SEMI和中国电子专用设备协会的统计，2014年大陆半导体设备市场规模为44.2亿美元，而国内半导体设备厂商的销售收入仅6.83亿美元，严重依赖进口 大基金投资了国内半导体全产业链，客观上形成了半导体产业的国家队联盟 此次发行股份之后，七星电子也成为了半导体产业国家队的重要一员，有助于与下游客户（中芯、长电、三安等）建立更紧密的合作关系，获得更多的客户订单 因此，国内急需整合优势资源，培育出具备国际竞争力的半导体设备龙头 此次整合之后，公司的高端设备竞争力得到大幅提升，有望实现对更多进口设备的替代，跻身全球领先的半导体设备厂商行列 图2：北方微电子的营业收入情况  图3：北方微电子的净利润情况    数据来源：公司公告，东北证券  数据来源：公司公告，东北证券  图 4：2010-2015年我国自制半导体设备销售规模及自制率  数据来源：公司公告，东北证券  2.4. 收购北方微，强强联合协同效应明显  七星电子和北方微电子双方都是国内领先的半导体设备厂商，双方的设备互补性强，客户基本相同，整合之后，公司可以提供的半导体设备包括刻蚀机（ETCH）、PVD、CVD、立式炉、清洗机、ALD等，覆盖了半导体产线上一半以上的主要设备，从而可以为客户提供整套解决方案，提升综合竞争力 请务必阅读正文后的声明及说明                                                                 5 / 8 七星电子/公司动态报告  图 5：2014年全球前十大半导体设备厂商（单位：亿美元）  数据来源：公司公告，东北证券  2.5. 配套募集资金获大基金认购，加入国家队最强阵容  本次发行股份募集配套资金的发行对象为国家集成电路基金（大基金）、京国瑞基金和芯动能基金，其中大基金斥资6亿元认购，说明大基金非常看好整合后公司的发展前景 作为上游供应商的国内半导体设备厂商将最大程度受益于大陆半导体产业的发展机遇，由弱到强，由低端向高端，逐步替代海外的进口设备 高端设备研发及市场开拓不及预期 表 2：大基金的投资项目 序号 投资标的 投资额（亿元） 具体投向 领域 1 紫光集团 100 紫光旗下芯片业务 设计 2 长电科技 18 参与长电收购星科金鹏 封测 3 中微半导体 4.8 股权投资 设备 4 中芯国际 25 股权投资 制造 5 艾派克 5 参与定增 设计 6 国科微 4 NA 设计 7 格科微 NA NA 设计 8 三安光电 54.39 股权投资 制造 9 北斗星通 15 股权投资 设计（北斗芯片） 10 通富微电 NA 参与收购AMD封测厂 封测 11 华天科技 5 华天西安 封测 数据来源：公开资料，东北证券                                                                           请务必阅读正文后的声明及说明                                                                 6 / 8 七星电子/公司动态报告   表 3：配套融资的认购情况 认购对象 认购金额（万元） 获得上市公司股份数（万股） 国家集成电路基金 60,000.00 3,430.53 京国瑞基金 20,000.00 1,143.51 芯动能基金 13,084.19 748.10 合计 93,084.19 5,322.14 数据来源：公司公告，东北证券  3. 顺应产业链向大陆转移之势，期待设备业崛起  在国家政策的大力扶持下，半导体产业链向大陆转移的趋势确定，上游设备商最为受益 我们预计2015-2017年，公司实现净利润分别为0.45/1.13/1.49亿元，EPS分别为0.13/0.25/0.33元 4. 盈利预测及投资建议  假设按照预案发行股份收购北方微电子并募集资金，且于2016年开始并表 根据Garter的统计，2014年全球半导体设备市场的规模为330亿美元，而国内半导体设备厂商的销售额仅6.83亿美元，国内厂商的全球市占率仅2%，与国际领先厂商的差距巨大 自2014年9月成立以来，大基金投资了国内多家的半导体龙头公司，包括紫光集团、中芯国际、长电科技、三安光电、通富微电、华天科技等 2015年12月，亦庄国投3亿美元收购美国半导体设备Mattson，成为中国半导体设备产业国际并购的第一单，意味着中国半导体设备业也拉开了国际并购的大幕 此次强强联合之后，将形成国内的半导体设备龙头，有望跻身全球一流半导体设备厂商行列，逐步实现高端半导体设备的进口替代 然而，由于半导体设备的技术门槛极高，国内厂商与海外竞争对手的差距非常大，国产设备难以满足最先进制程的需求，而最先进制程往往下游客户投资的主要方向\",\n      \"t\" : \"图 5：2014年全球前十大半导体设备厂商（单位：亿美元）"))
    val result = indexReport(reportList)
    Thread.currentThread().suspend()
    println(result)
  }

  def testLoadDataToDictionary() = {
    BizUtil.loadDataToDictionary(conf)
  }

  def testIndexFromPdf() = {
    indexFromPdf
    Thread.currentThread().suspend()
  }

  def testDeleteAllIndexData() = {
    client.delAllData(newsIndexName, newsTypName)
  }

  def testSearchQbWithFilterAndSorts() = {
    val filter = new mutable.HashMap[String, (Object, Boolean)]()
    //filter("title") = ("苹果",false)
    var calendar = Calendar.getInstance()
    calendar.add(Calendar.DATE, -1)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.HOUR, 0)
    val lowlerBounder = calendar.getTime
    val upperBounder = calendar.getTime
    filter("create_on") = ((lowlerBounder, null), true)
    // filter("create_on") = ((null,upperBounder),true)
    val result = client.searchQbWithFilterAndSorts(newsIndexName, newsTypName, 0, 10, filter, sorts = null)
    println(result)
  }

  def testSearchQbWithFilterAndSortsWithDecayAndSearch() = {
    val query = "苹果"
    val title = "title^6"
    val auth = "auth^3"
    val summary = "summary"
    val topics = "topics^5"
    val events = "events^5"
    val companys = "companys^5"
    val decayField = "create_on"


    var filter = new mutable.HashMap[String, (Object, Boolean)]()
    //filter("title") = ("苹果",false)
    var calendar = Calendar.getInstance()
    calendar.add(Calendar.DATE, -10)
    calendar.set(Calendar.MILLISECOND, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.HOUR, 0)
    val lowlerBounder = calendar.getTime
    val upperBounder = calendar.getTime
    filter("create_on") = ((lowlerBounder, null), true)
    // filter("create_on") = ((null,upperBounder),true)
    var sorts = new mutable.HashMap[String, String]()
    sorts("create_on") = "desc"
    sorts = null

    val result = client.searchQbWithFilterAndSorts(newsIndexName, newsTypName,
      0, 10, filter, sorts, query, decayField, aggs = null,
      title, auth, summary, topics, events, companys)
    println(result)
  }


  def testIndexNewsFromMongo() = {
    indexNewsFromMongo(9)
    Thread.currentThread().suspend()
  }

  def testTrieNode() = {
    val result = conf.dictionary.prefixWitId("QDP".toUpperCase)
    //val result = conf.dictionary.prefix("sd".toUpperCase)
    println(result)

    if (conf.dictionary.contains("QDPJ")) {
      println("have qdpj")
    } else {
      println("no qdpj")
    }
    val id = conf.dictionary.findWithId("青岛啤酒")
    println(id)
    result
  }


  def testFilterGraphNodes() = {
    filterGraphNodes
  }

  def testAddBloomFilterFromGraph() = {
    addBloomFilterFromGraph
    val flag = conf.bloomFilter.mightContain("乐视|酷派|第一大股东")
    println(flag)
  }


  def testAddSynonmToGraph() = {
    addSynonymToGraph
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
