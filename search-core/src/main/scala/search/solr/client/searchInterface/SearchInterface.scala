package search.solr.client.searchInterface

import java.util
import java.util.concurrent.LinkedBlockingQueue
import javax.servlet.http.HttpServletRequest

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.SolrQuery.ORDER
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.util.SimpleOrderedMap
import search.common.cache.SearchCache
import search.common.config.{SolrConfiguration, Configuration}
import search.common.entity.searchinterface._
import search.solr.client.keyword.HotSearch
import search.solr.client.log.SearchLog
import search.common.util.{Util, Logging}
import search.common.view.control.ControlWebView
import search.solr.client.{SolrClientConf, SolrClient}
import scala.collection.JavaConversions._
import scala.util.control.Breaks._

/**
  * Created by soledede on 2016/2/20.
  */
object SearchInterface extends Logging with SolrConfiguration {


  @volatile var switchCollection: Boolean = false
  @volatile var switchMg: String = null
  @volatile var switchSc: String = null

  var arrayObj: Array[String] = null

  if (filterChanges != null && !filterChanges.trim.equalsIgnoreCase("")) {
    arrayObj = filterChanges.split("&")
  }


  val multiValueArray = Array("picUrl", "cityId")


  val spellcheckSeparator = "_____"


  var solrClient: SolrClient = _
  //val solrClient = SolrClient(new SolrClientConf(), "httpUrl")

  var mongoSearchLog: SearchLog = SearchLog("mongo")

  val filterSplitArray = Array("<->", "->")

  var logQueue: LinkedBlockingQueue[java.util.Map[String, Object]] = new LinkedBlockingQueue[java.util.Map[String, Object]]


  val generalFacetFieldCategory = "da_2955_s"
  //category facet Map(da_2955_s->类别)

  //brandId
  val generalFacetFieldBrandId = "brandId"
  // val generalFacetFieldBrandId = "da_89_s" //brand facet  Map(da_89_s->品牌)


  val keyWordsModelPinyin = s"(original:keyWord^50) OR (sku:keyWord^50) OR (brandZh_ps:keyWord^300) OR (brandEn_ps:keyWord^300) OR (brandZh:keyWord^200) OR (brandEn:keyWord^200) OR (sku:*keyWord*^11) OR (original:*keyWord*^10) OR (text:keyWord^2) OR (pinyin:keyWord^0.002)"
  val keyWordsModel = s"(original:keyWord^50) OR (sku:keyWord^50) OR (brandZh_ps:keyWord^300) OR (brandEn_ps:keyWord^300) OR (brandZh:keyWord^200) OR (brandEn:keyWord^200) OR (sku:*keyWord*^11) OR (original:*keyWord*^10) OR (text:keyWord^2)"

  if (useSolr) init


  def init() = {

    if (webStart) {
      val web = new ControlWebView(monitorPort, new SolrClientConf())
      web.bind()
    }

    solrClient = SolrClient(new SolrClientConf())
  }


  def getTrees(collection: String = defaultCollection, keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String]) = {
    //attributeFilterSearch(collections, keywords, catagoryId, cityId, sorts, null, null, start, rows, categoryIds, isCameFromSearch)
    attributeFilterSearch(collection, keyWords, null, null, null, null, null, 0, 0, null, true)
  }


  //get all categoryIds
  /**
    *
    * @param collection
    * @param keyWords
    * @param cityId
    * @return
    * for NiNi
    * actural java.util.List[Integer]
    */
  def getCategoryIdsByKeyWords(collection: String = defaultCollection, keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String]): NiNi = {

    Util.caculateCostTime {
      SearchCache.cacheCategoryIdsByKeyWords(keyWords, cityId, filters, {
        var field = "categoryId4"
        getCategoryIdsByKeyWords(collection, keyWords, cityId, field, filters)
      })
    }
  }


  /**
    *
    * @param keyWords
    * @param cityId
    * @param sorts
    * @param start
    * @param rows
    * @return
    * for NiNi
    * actural FilterAttributeSearchResult
    */
  def searchByKeywords(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.LinkedHashMap[java.lang.String, java.lang.String], sorts: java.util.LinkedHashMap[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): NiNi = {
    searchByKeywords(defaultCollection, defaultAttrCollection, keyWords, cityId, filters, sorts, start, rows)
  }

  /**
    *
    * search by keywords,must record searchlog for log analysis
    *
    * @param keyWords eg:螺丝钉
    * @param cityId   eg:111
    * @param sorts    eg:Map(price->desc,sales->desc,score->desc)
    * @param start    eg:0
    * @param rows     eg:10
    * @return
    * for NiNi
    * actural FilterAttributeSearchResult
    *
    */
  def searchByKeywords(collection: String = defaultCollection, attrCollection: String = defaultAttrCollection, keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): NiNi = {
    Util.caculateCostTime {
      SearchCache.cacheSearchByKeywords(keyWords, cityId, filters, sorts, start, rows, {
        logInfo(s"search by keywords:$keyWords")

        var (collections: String, attrCollections: String) = setSwitchCollection(collection, attrCollection)


        var filterAttributeSearchResult: FilterAttributeSearchResult = null
        var field = "categoryId4"
        var categoryIds: util.List[Integer] = getCategoryIds(false, collections, keyWords, cityId, field, filters)
        if (categoryIds == null || categoryIds.size() <= 0)
          categoryIds = getCategoryIds(true, collections, keyWords, cityId, field, filters)
        var categoryId = -1
        if (categoryIds != null && categoryIds.size() > 0) {
          categoryId = categoryIds.get(0)
          filterAttributeSearchResult = searchFilterAttributeAndResulAndSearchResulttByCatagoryIdAndKeywords(collections, attrCollections, categoryId, cityId, keyWords, filters, sorts, start, rows, categoryIds, true)
          if (filterAttributeSearchResult != null && filterAttributeSearchResult.getFilterAttributes == null) {
            field = "categoryId3"
            filterAttributeSearchResult = searchFilterAttributeAndResulAndSearchResulttByCatagoryIdAndKeywords(collections, attrCollections, categoryId, cityId, keyWords, filters, sorts, start, rows, categoryIds, true)
          }
        } else filterAttributeSearchResult = searchFilterAttributeAndResulAndSearchResulttByCatagoryIdAndKeywords(collections, attrCollections, null, cityId, keyWords, filters, sorts, start, rows, categoryIds, true)

        filterAttributeSearchResult
      })
    }
  }


  /**
    *
    * search by popularity
    *
    * @return
    * for NiNi
    * actural  java.util.List[String]
    *
    */
  def popularityKeyWords(): NiNi = {
    Util.caculateCostTime {
      val keywords = HotSearch.hotKeywords
      if (keywords == null || keywords.size() == 0) {
        val list = new util.ArrayList[String]()
        list.add("3m")
        list.add("工具箱")
        list
      } else keywords
    }
  }


  def recordSearchLog(keyWords: java.lang.String, req: HttpServletRequest, sessionId: String): Unit = {
    val ip: String = req.getRemoteHost
    val cookies = req.getCookies
    var cookiesString = cookies.map(c => c.getName + ":" + c.getValue).mkString("-")
    if (cookiesString.equals("")) cookiesString = "-"
    val userAgent: String = req.getHeader("User-Agent")
    val userId: String = "-"
    recordSearchLog(keyWords, "-", ip, userAgent, "-", cookiesString, userId, sessionId)
  }


  def recordSearchLog(keyWords: java.lang.String, appKey: java.lang.String, clientIp: java.lang.String, userAgent: java.lang.String, sourceType: java.lang.String, cookies: java.lang.String, userId: java.lang.String): Unit = {
    recordSearchLog(keyWords, appKey, clientIp, userAgent, sourceType, cookies, userId, null)
  }

  /**
    *
    * search keywords log record
    * who where when what
    *
    * @param keyWords
    * @param appKey
    * @param clientIp
    * @param userAgent
    * @param sourceType
    * @param cookies
    * @param userId
    *
    */
  def recordSearchLog(keyWords: java.lang.String, appKey: java.lang.String, clientIp: java.lang.String, userAgent: java.lang.String, sourceType: java.lang.String, cookies: java.lang.String, userId: java.lang.String, sessionId: String): Unit = {
    val currentTime = System.currentTimeMillis()
    logInfo(s"record search log:keyWords:$keyWords-appKey:$appKey-clientIp:$clientIp-userAgent:$userAgent-sourceType:$sourceType-cookies:-$cookies-userId:$userId-currentTime:$currentTime")
    val map = new util.HashMap[String, Object]()
    map.put("keyWords", keyWords)
    map.put("appKey", appKey)
    map.put("clientIp", clientIp)
    map.put("userAgent", userAgent)
    map.put("sourceType", sourceType)
    map.put("cookies", cookies)
    map.put("userId", userId)
    map.put("currentTime", currentTime.toString)
    map.put("sessionId", sessionId)
    logQueue.put(map)
    //mongoSearchLog.write(keyWords, appKey, clientIp, userAgent, sourceType, cookies, userId, Util.timestampToDate(currentTime))


  }

  private var thread = new Thread("search log thread ") {
    setDaemon(true)

    override def run() {
      while (true) {
        mongoSearchLog.write(logQueue.take())
        //stastics hot keywords,just one week
        // HotSearch.recordAndStatictisKeywords(logQueue.take())
      }
    }
  }

  thread.start()


  /**
    *
    * @param keyWords
    * @param cityId
    * @return
    *
    * for NiNi
    * actural  java.util.Map[java.lang.String, java.lang.Integer]
    */
  def suggestByKeyWords(keyWords: java.lang.String, cityId: java.lang.Integer): NiNi = {
    suggestByKeyWords(defaultCollection, keyWords, cityId)
  }

  /**
    *
    * this for autoSuggest in search
    *
    * @param keyWords search keyword
    * @param cityId
    * @return java.util.Map[java.lang.String,java.lang.Integer]   eg:Map("soledede"=>10004)  represent counts of document  the keywords  side in
    *
    *         for NiNi
    *         actural  java.util.Map[java.lang.String, java.lang.Integer]
    */
  def suggestByKeyWords(collection: String = defaultCollection, keyWords: java.lang.String, cityId: java.lang.Integer): NiNi = {
    Util.caculateCostTime {
      SearchCache.cacheSuggestByKeyWords(keyWords, cityId, {
        var (collections: String, attrCollections: String) = setSwitchCollection(collection, defaultAttrCollection)

        if (keyWords != null && !keyWords.trim.equalsIgnoreCase("")) {


          val keyWordsPro = keyWords.trim.toLowerCase
          val keyWordsModel = s"(kw_ik:${keyWordsPro}* OR pinyin:${keyWordsPro}* OR py:${keyWordsPro}*)"

          val fl = "kw"


          val query: SolrQuery = new SolrQuery
          // query.set("qt", "/select")
          query.setQuery(keyWordsModel)

          query.setFields(fl)

          //sort
          query.addSort("weight", SolrQuery.ORDER.desc)

          query.setStart(0)
          query.setRows(20)

          val r = solrClient.searchByQuery(query, defaultSuggestCollection)
          var result: QueryResponse = null
          if (r != null) result = r.asInstanceOf[QueryResponse]
          val docs = getSearchResult(result, null)

          if (docs != null) {
            val keyWordsMap = new java.util.HashMap[java.lang.String, java.lang.Integer]()
            val query: SolrQuery = new SolrQuery
            docs.foreach { doc =>
              val keyword = doc.get("kw").toString.trim
              val count: Int = countKeywordInDocs(collections, keyword, query, cityId)
              if (count > 0) keyWordsMap.put(keyword, count)
            }
            if (!keyWordsMap.isEmpty) keyWordsMap
            else null
          } else null

        }
        else null
      })
    }
  }


  /**
    *
    * @param catagoryId
    * @param cityId
    * @return
    * for NiNi
    * actural  java.util.List[Brand]
    */
  def searchBrandsByCatoryId(catagoryId: java.lang.Integer, cityId: java.lang.Integer): NiNi = {
    searchBrandsByCatoryId(defaultCollection, catagoryId, cityId, null, 0, java.lang.Integer.MAX_VALUE)
  }

  /**
    *
    * get all brands by catoryId
    *
    * @param catagoryId
    * @param cityId
    * @return
    * for NiNi
    * actural  java.util.List[Brand]
    */
  def searchBrandsByCatoryId(collection: String = defaultCollection, catagoryId: java.lang.Integer, cityId: java.lang.Integer): NiNi = {
    searchBrandsByCatoryId(collection, catagoryId, cityId, null, 0, java.lang.Integer.MAX_VALUE)
  }


  /**
    * merge category and filtersearch
    *
    * @param collection
    * @param attrCollection
    * @param keyWords
    * @param catagoryId
    * @param cityId
    * @param sorts
    * @param filters
    * @param filterFieldsValues
    * @param start
    * @param rows
    * @param isCategoryTouch whether come from category
    * @return FilterAttributeSearchResult
    *         for NiNi
    *         actural  FilterAttributeSearchResult
    */
  def attributeFilterSearch(collection: String, attrCollection: String, keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, isCategoryTouch: java.lang.Boolean): NiNi = {
    Util.caculateCostTime {
      SearchCache.cacheAttributeFilterSearch(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isCategoryTouch, {
        if (isCategoryTouch) {
          // searchFilterAttributeAndResultByCatagoryId(collection,attrCollection,catagoryId, cityId)
          searchFilterAttributeAndResulAndSearchResulttByCatagoryIdAndKeywords(collection, attrCollection, catagoryId, cityId, keyWords, filters, null, start, rows, null, false)
        } else attributeFilterSearch(collection, keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, null, false).getData.asInstanceOf[FilterAttributeSearchResult]
      })
    }
  }


  /**
    *
    * @param keyWords
    * @param catagoryId
    * @param cityId
    * @param sorts
    * @param filters
    * @param filterFieldsValues
    * @param start
    * @param rows
    * @param categoryIds
    * @param isComeFromSearch
    * @return
    * for NiNi
    * actural  FilterAttributeSearchResult
    */
  def attributeFilterSearch(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, categoryIds: java.util.List[Integer], isComeFromSearch: Boolean): NiNi = {
    attributeFilterSearch(defaultCollection, keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, categoryIds, isComeFromSearch)
  }


  /**
    *
    * Tips: front should keep the attributeName cache by searchFilterAttributeByCatagoryId
    *
    * @param keyWords
    * @param catagoryId
    * @param cityId
    * @param sorts              eg:Map(price->desc,sales->desc,score->desc)
    * @param filters            eg:Map("t89_s"->"一恒","t214_tf"->"[300 TO *]")  fq
    * @param filterFieldsValues facet.field and facet.querys  eg: Map(
    *                           "t89_s"=>null,
    *                           "t214_tf"=>List("* TO 100","100 TO 200","200 TO *")
    *                           )
    * @param start              eg:0
    * @param rows               eg:10
    * @return FilterAttributeSearchResult
    *         for NiNi
    *         actural  FilterAttributeSearchResult
    */

  def attributeFilterSearch(collection: String, keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false): NiNi = {
    Util.caculateCostTime {
      SearchCache.cacheAttributeFilterSearch(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, categoryIds, isComeFromSearch, {
        var result = searchCore(false, collection, keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, categoryIds, isComeFromSearch)
        val total = result.getSearchResult.getTotal
        if (total == null || total == 0) result = searchCore(true, collection, keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, categoryIds, isComeFromSearch)
        result
      })
    }
  }


  /*private def attributeFilterSearch(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, isCategoryTouch: java.lang.Boolean): FilterAttributeSearchResult = {
    if (isCategoryTouch) searchFilterAttributeAndResultByCatagoryId(defaultCollection, defaultAttrCollection, catagoryId, cityId)
    else attributeFilterSearch(defaultCollection, keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, null, false)
  }*/

  //get all categoryIds
  /**
    *
    * @param collection
    * @param keyWords
    * @param cityId
    * @param field
    * @return
    */
  private def getCategoryIdsByKeyWords(collection: String, keyWords: java.lang.String, cityId: java.lang.Integer, field: String, filters: java.util.Map[java.lang.String, java.lang.String]): java.util.List[Integer] = {
    var cList = getCategoryIds(false, collection, keyWords, cityId, field, filters)
    if (cList == null || cList.size() <= 0) cList = getCategoryIds(false, collection, keyWords, cityId, field, filters)
    cList
  }


  /**
    *
    */
  private def searchCore(orOpen: Boolean = false, collection: String, keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false): FilterAttributeSearchResult = {
    if (cityId != null) {
      var (collections: String, attrCollections: String) = setSwitchCollection(collection, defaultAttrCollection)

      val filterAttributeSearchResult = new FilterAttributeSearchResult()

      if (categoryIds != null && categoryIds.size() > 0) filterAttributeSearchResult.setCategoryIds(categoryIds)

      val msg = new Msg()
      val searchResult = new SearchResult()
      //page
      var sStart: Int = 0
      var sRows: Int = 10

      if (start != null && start > 0) sStart = start
      if (rows != null && rows > 0) sRows = rows




      var keyWord: String = null
      if (keyWords != null && !keyWords.trim.equalsIgnoreCase(""))
        keyWord = keyWords.trim.toLowerCase
      var keyWordsModels = "*:*"
      if (keyWord != null)
      // keyWordsModel = s"(original:$keyWord^50) OR (sku:$keyWord^50) OR (brandZh:$keyWord^200) OR (brandEn:$keyWord^200) OR (sku:*$keyWord*^11) OR (original:*$keyWord*^10) OR (text:$keyWord^2) OR (pinyin:$keyWord^0.002)"
        if (Util.regex(keyWords, "(^[a-zA-Z]+$)")) {
          keyWordsModels = keyWordsModelPinyin.replaceAll("keyWord", keyWord)
        } else
          keyWordsModels = keyWordsModel.replaceAll("keyWord", keyWord)


      val fqGeneral = s"(isRestrictedArea:0 OR cityId:$cityId)"
      val fqCataId = s"(categoryId1:$catagoryId OR categoryId2:$catagoryId OR categoryId3:$catagoryId OR categoryId4:$catagoryId)"

      val query: SolrQuery = new SolrQuery
      query.set("qt", "/select")
      query.setQuery(keyWordsModels)
      query.addFilterQuery(fqGeneral)

      if (!isComeFromSearch && catagoryId != null && catagoryId != -1) //whether is come from category filter
        query.addFilterQuery(fqCataId)


      if (orOpen) query.setParam("q.op", "OR")

      //set filters
      setFilters(filters, query)


      //sort
      // query.addSort("score", SolrQuery.ORDER.desc)
      if (sorts != null && sorts.size() > 0) {
        // eg:  query.addSort("price", SolrQuery.ORDER.desc)
        sorts.foreach { sortOrder =>
          val field = sortOrder._1
          val orderString = sortOrder._2.trim
          var order: ORDER = null
          orderString match {
            case "desc" => order = SolrQuery.ORDER.desc
            case "asc" => order = SolrQuery.ORDER.asc
            case _ => SolrQuery.ORDER.desc
          }
          query.addSort(field, order)
        }
      }


      //facet and facet query
      query.setFacet(true)
      query.setFacetMinCount(1)
      query.setFacetMissing(false)
      query.setFacetLimit(-1)


      //facet for category and d_89_s that represent brand
      if (isComeFromSearch) {
        //just for search
        query.addFacetField(generalFacetFieldCategory)
        query.addFacetField(generalFacetFieldBrandId)
      }


      if (filterFieldsValues != null && filterFieldsValues.size() > 0) {
        filterFieldsValues.foreach { facet =>
          val field = facet._1
          val ranges = facet._2
          if (ranges != null && ranges.size() > 0) {
            //range facet.query
            ranges.foreach(range => query.addFacetQuery(s"$field:$range"))
          } else {
            //facet.field
            query.addFacetField(field)
          }
        }
      }




      //page
      query.setStart(sStart)
      query.setRows(sRows)




      val r = solrClient.searchByQuery(query, collections)
      var result: QueryResponse = null
      if (r != null) result = r.asInstanceOf[QueryResponse]
      getSearchResultByResponse(msg, searchResult, result) //get searchResult

      filterAttributeSearchResult.setSearchResult(searchResult) //set searchResult


      getFacetFieldAndFacetQueryToFilterAttributes(filterAttributeSearchResult, result)
      filterAttributeSearchResult
    } else null
  }


  /**
    * just category
    *
    * @param catagoryId
    * @param cityId
    * @return
    */
  private def attributeFilterSearch(catagoryId: java.lang.Integer, cityId: java.lang.Integer): FilterAttributeSearchResult = {
    var (collections: String, attrCollections: String) = setSwitchCollection(defaultCollection, defaultAttrCollection)
    searchFilterAttributeAndResultByCatagoryId(collections, attrCollections, catagoryId, cityId)
  }


  private def getFacetFieldAndFacetQueryToFilterAttributes(filterAttributeSearchResult: FilterAttributeSearchResult, result: QueryResponse): Unit = {
    if (result != null) {
      val filterAttributes = new util.ArrayList[FilterAttribute]()

      val facetFields = result.getFacetFields
      if (facetFields != null && facetFields.size() > 0) {
        //facet.field
        facetFields.foreach { facetField =>
          val filterAttribute = new FilterAttribute()

          val facetFieldName = facetField.getName
          val facetFieldValues = facetField.getValues
          filterAttribute.setAttrId(facetFieldName)
          filterAttribute.setAttrName(getAttributeNameById(facetFieldName))

          if (facetFieldValues != null && facetFieldValues.size() > 0) {
            val attributeCountMap = new util.HashMap[String, Integer]()
            facetFieldValues.foreach { facetcount =>
              val attributeValue = facetcount.getName
              val count = facetcount.getCount.toInt
              attributeCountMap.put(attributeValue, count)
            }
            filterAttribute.setAttrValues(attributeCountMap)
            filterAttribute.setRangeValue(false)
            filterAttributes.add(filterAttribute)
          }
        }
      }

      //facet.query
      val facetQuerys = result.getFacetQuery

      if (facetQuerys != null && !facetQuerys.isEmpty) {
        //facet.query
        /**
          * "t87_tf:[* TO 0}":0,
          * "t87_tf:[0 TO 10}":0,
          * "t87_tf:[10 TO 20}":1,
          * "t87_tf:[20 TO 30}":2,
          * "t87_tf:[30 TO *}":4},
          */
        val facetQueryCountMap = new util.HashMap[String, util.Map[String, Integer]]()

        facetQuerys.foreach { facetQuery =>
          val query = facetQuery._1
          val count = facetQuery._2
          if (count > 0) {
            val queryFields = query.split(":")
            val field = queryFields(0)
            val attrValue = queryFields(1)
            if (!facetQueryCountMap.contains(field.trim)) {
              val countMap = new util.HashMap[String, Integer]()
              countMap.put(attrValue, count)
              facetQueryCountMap.put(field.trim, countMap)
            } else {
              facetQueryCountMap.get(field.trim).put(attrValue, count)
            }
          }

        }
        if (!facetQueryCountMap.isEmpty) {
          facetQueryCountMap.foreach { facetQuery =>
            val attributeId = facetQuery._1
            val attributeName = getAttributeNameById(attributeId)
            val attributeValues = facetQuery._2
            val isRangeValue = true
            val filterAttribute = new FilterAttribute(attributeId, attributeName, attributeValues, isRangeValue)
            filterAttributes.add(filterAttribute)
          }

        }

      }


      if (filterAttributes.size() != 0) filterAttributeSearchResult.setFilterAttributes(filterAttributes)
    }
  }


  /**
    *
    * @param catagoryId
    * @param cityId
    * @param sorts eg:Map(price->desc,sales->desc,score->desc)
    * @param start eg:0
    * @param rows  eg:10
    * @return java.util.List[Brand]
    *         for NiNi
    *         actural  java.util.List[Brand]
    */
  private def searchBrandsByCatoryId(collection: String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): NiNi = {
    Util.caculateCostTime {
      SearchCache.cacheSearchBrandsByCatoryId(catagoryId, cityId, sorts, start, rows, {
        if (catagoryId != null) {
          var (collections: String, attrCollections: String) = setSwitchCollection(collection, defaultAttrCollection)
          //page
          var sStart: Int = 0
          var sRows: Int = 10

          if (start != null && start > 0) sStart = start
          if (rows != null && rows > 0) sRows = rows


          val keyWordsModel = "*:*"

          val fqGeneral = s"(isRestrictedArea:0 OR cityId:$cityId)"
          val fq = s"(categoryId1:$catagoryId OR categoryId2:$catagoryId OR categoryId3:$catagoryId OR categoryId4:$catagoryId)"

          val fl = "brandId,brandEn,brandZh"

          val query: SolrQuery = new SolrQuery
          query.set("qt", "/select")
          query.setQuery(keyWordsModel)
          query.addFilterQuery(fqGeneral)
          query.addFilterQuery(fq)
          query.setFields(fl)

          //sort
          query.addSort("score", SolrQuery.ORDER.desc)
          if (sorts != null && sorts.size() > 0) {
            // eg:  query.addSort("price", SolrQuery.ORDER.desc)
            sorts.foreach { sortOrder =>
              val field = sortOrder._1
              val orderString = sortOrder._2.trim
              var order: ORDER = null
              orderString match {
                case "desc" => order = SolrQuery.ORDER.desc
                case "asc" => order = SolrQuery.ORDER.asc
                case _ => SolrQuery.ORDER.desc
              }
              query.addSort(field, order)
            }
          }

          //page
          query.setStart(sStart)
          query.setRows(sRows)

          val r = solrClient.searchByQuery(query, collections)
          var result: QueryResponse = null
          if (r != null) result = r.asInstanceOf[QueryResponse]
          val brandIdToBrandMap = getBrandsSearchResultUniqueById(result)

          if (brandIdToBrandMap != null) {
            val brandList = new java.util.ArrayList[Brand]()
            brandIdToBrandMap.foreach { idToBrand =>
              brandList.add(idToBrand._2)
            }
            if (brandList.size() > 0) brandList
            else null
          } else null
        } else null
      })
    }
  }

  /**
    *
    * get brands    Map(brandId->Brand)
    *
    * @param result
    * @return
    */
  private def getBrandsSearchResultUniqueById(result: QueryResponse): java.util.Map[java.lang.Integer, Brand] = {

    //get Result
    if (result != null) {
      val resultBrandIdMap: java.util.Map[java.lang.Integer, Brand] = new java.util.HashMap[java.lang.Integer, Brand]() //brand result Map(brandId->Brand)
      val response = result.getResults
      if (response != null) {
        response.foreach { doc =>
          val brandId = doc.getFieldValue("brandId").toString.toInt
          if (!resultBrandIdMap.contains(brandId)) {
            val brandEn = doc.getFieldValue("brandEn").toString
            val brandZh = doc.getFieldValue("brandZh").toString
            val brand = new Brand(brandId, brandZh, brandEn)
            resultBrandIdMap.put(brandId, brand)
          }
        }
      }
      if (!resultBrandIdMap.isEmpty) resultBrandIdMap
      else null
    }
    else null
  }

  private def countKeywordInDocs(collection: String, keyword: Object, query: SolrQuery, cityId: java.lang.Integer): Int = {
    if (keyword != null) {
      val keyWord = keyword.toString.trim.toLowerCase
      //val keyWordsModel = s"(original:$keyWord^50) OR (sku:$keyWord^50) OR (brandZh:$keyWord^200) OR (brandEn:$keyWord^200) OR (sku:*$keyWord*^11) OR (original:*$keyWord*^10) OR (text:$keyWord^2) OR (pinyin:$keyWord^0.002)
      val keyWordsModels = keyWordsModel.replaceAll("keyWord", keyword.toString)
      val fq = s"isRestrictedArea:0 OR cityId:$cityId"


      query.set("qt", "/select")
      query.setQuery(keyWordsModels)
      if (cityId != null) {
        query.setFilterQueries(fq)
      }
      //query.setQuery(keyWordsModels)
      query.setRows(1)
      val r = solrClient.searchByQuery(query, collection)
      var result: QueryResponse = null
      if (r != null) result = r.asInstanceOf[QueryResponse]
      if (result != null) {
        val resultDocs = result.getResults
        if (resultDocs != null && resultDocs.size() > 0) {
          val numfound = resultDocs.getNumFound
          numfound.toInt
        }
        else 0
      } else 0
    } else 0
  }


  /**
    *
    * data dictionary attribute id to attribute name
    *
    * @param attributeId
    * @return
    */
  private def getAttributeNameById(attributeId: String): String = {
    if (attributeId == null || attributeId.trim.equals("")) return null
    if (attributeId.equals(generalFacetFieldCategory)) return "类别"
    if (attributeId.equals(generalFacetFieldBrandId)) return "品牌"
    val attrCache = FilterAttribute.attrIdToattrName
    var attrName: String = null
    if (attrCache.contains(attributeId.trim)) {
      attrName = attrCache.get(attributeId.trim)
    }
    attrName
  }


  /**
    *
    * set data dictionary attribute id to attribute name
    *
    * @param attributeId
    * @param attributeName
    * @return
    */
  private def setAttributeNameById(attributeId: String, attributeName: String): Unit = {
    val attrCache = FilterAttribute.attrIdToattrName
    if (!attrCache.contains(attributeId.trim)) {
      attrCache.put(attributeId.trim, attributeName.trim)
    }
  }


  private def groupBucket(query: SolrQuery, collection: String, q: String, fq: String, json_facet: String, filters: java.util.Map[java.lang.String, java.lang.String]): java.util.List[SimpleOrderedMap[java.lang.Object]] = {
    if (json_facet == null) return null

    query.set("qt", "/select")
    if (q != null && !q.trim.equalsIgnoreCase("")) query.setQuery(q) else query.setQuery("*:*")
    if (fq != null && !fq.trim.equalsIgnoreCase(""))
      query.set("fq", fq)
    query.setParam("json.facet", json_facet)
    query.setRows(0)
    query.setStart(0)

    if (filters != null && !filters.isEmpty()) setFilters(filters, query)

    val rt = solrClient.searchByQuery(query, collection)
    if (rt == null) return null
    else {
      val r = rt.asInstanceOf[QueryResponse]
      val fMap = r.getResponse
      if (fMap != null) {
        val facetsMap = fMap.get("facets").asInstanceOf[SimpleOrderedMap[java.lang.Object]]
        if (facetsMap != null && !facetsMap.isEmpty && facetsMap.size() > 0) {
          val count = facetsMap.get("count").toString.toInt
          if (count > 0) {
            val catagoryMap = facetsMap.get("categories").asInstanceOf[SimpleOrderedMap[java.util.List[SimpleOrderedMap[java.lang.Object]]]]
            val bucketList = catagoryMap.get("buckets")
            bucketList
          } else null
        } else null
      } else null
    }
  }


  /**
    * get category ids
    *
    * @param keyWords
    * @param cityId
    * @param field
    * @return
    */
  private def getCategoryIds(orOpen: Boolean, collection: String, keyWords: java.lang.String, cityId: java.lang.Integer, field: String, filters: java.util.Map[java.lang.String, java.lang.String]): util.List[Integer] = {
    var keyWordsModels = "*:*"
    if (keyWords != null) {
      val keyWord = keyWords.trim.toLowerCase
      if (Util.regex(keyWords, "(^[a-zA-Z]+$)")) {
        keyWordsModels = keyWordsModelPinyin.replaceAll("keyWord", keyWord)
      } else
        keyWordsModels = keyWordsModel.replaceAll("keyWord", keyWord)
    }

    val fq = s"isRestrictedArea:0 OR cityId:$cityId"

    var jsonFacet = s"{categories:{type:terms,field:$field,limit:-1,sort:{count:desc}}}"
    jsonFacet = jsonFacet.replaceAll(":", "\\:")
    val query: SolrQuery = new SolrQuery
    if (orOpen) query.setParam("q.op", "OR")
    val categoryResultMap = groupBucket(query, collection, keyWordsModels, fq, jsonFacet, filters)
    var categoryIds: util.List[Integer] = null
    if (categoryResultMap != null) {
      categoryIds = new util.ArrayList[Integer]()
      val categoryResult = categoryResultMap.foreach { kv =>
        val categoryId = kv.get("val").toString.trim.toInt
        categoryIds.add(categoryId)
        //val count = kv.get("count").asInstanceOf[Int]
      }
    }
    categoryIds
  }


  /**
    *
    * @param categoryId
    * @param cityId
    * @param sorts eg:Map(price->desc,sales->desc,score->desc)
    * @param start eg:0
    * @param rows  eg:10
    * @return SearchResult
    */
  private def searchByCategoryId(categoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): SearchResult = {

    if (categoryId != null) {
      val msg = new Msg()
      val searchResult = new SearchResult()
      /* if (keyWords == null && cityId == null) {
         msg.setMsg("keyWords and cityId not null")
         searchResult.setMsg(msg)
         return searchResult
       }*/
      /*if (keyWords == null) {
        msg.setMsg("keyWords not null")
        searchResult.setMsg(msg)
        return searchResult
      }*/
      if (cityId == null) {
        msg.setMsg("cityId not null")
        searchResult.setMsg(msg)
        return searchResult
      }

      //page
      var sStart: Int = 0
      var sRows: Int = 10

      if (start != null && start > 0) sStart = start
      if (rows != null && rows > 0) sRows = rows


      val keyWordsModel = "*:*"

      val fqGeneral = s"(isRestrictedArea:0 OR cityId:$cityId)"
      val fq = s"(categoryId1:$categoryId OR categoryId2:$categoryId OR categoryId3:$categoryId OR categoryId4:$categoryId)"


      val query: SolrQuery = new SolrQuery
      query.set("qt", "/select")
      query.setQuery(keyWordsModel)
      query.addFilterQuery(fqGeneral)
      query.addFilterQuery(fq)

      //sort
      query.addSort("score", SolrQuery.ORDER.desc)
      if (sorts != null && sorts.size() > 0) {
        // eg:  query.addSort("price", SolrQuery.ORDER.desc)
        sorts.foreach { sortOrder =>
          val field = sortOrder._1
          val orderString = sortOrder._2.trim
          var order: ORDER = null
          orderString match {
            case "desc" => order = SolrQuery.ORDER.desc
            case "asc" => order = SolrQuery.ORDER.asc
            case _ => SolrQuery.ORDER.desc
          }
          query.addSort(field, order)
        }
      }

      //page
      query.setStart(sStart)
      query.setRows(sRows)

      val r = solrClient.searchByQuery(query, defaultCollection)
      var result: QueryResponse = null
      if (r != null) result = r.asInstanceOf[QueryResponse]

      val resultSearch = getSearchResult(result, searchResult) //get response result
      if (resultSearch != null && resultSearch.size > 0) searchResult.setResult(resultSearch) //set response resut
      searchResult
    } else null
  }


  /**
    *
    * get result spellcheck highlightini once
    *
    * @param msg
    * @param searchResult
    * @param result
    */
  private def getSearchResultByResponse(msg: Msg, searchResult: SearchResult, result: QueryResponse): Unit = {
    val resultSearch = getSearchResult(result, searchResult) //get response result
    if (resultSearch != null && resultSearch.size > 0) searchResult.setResult(resultSearch) //set response resut

    //  highlighting
    val highlighting = getHighlightingList(result)
    if (highlighting != null && highlighting.size() > 0) {
      val filterHighlightins = highlighting.filter(!_._2.isEmpty)
      if (filterHighlightins != null && !filterHighlightins.isEmpty && filterHighlightins.size > 0)
        searchResult.setHighlighting(filterHighlightins)
    }

    //spellcheck
    val spellchecks = getSpellCheckList(result)

    if (spellchecks != null && spellchecks.size() > 0) searchResult.setSpellChecks(spellchecks)

    msg.setMsg("success!")
    msg.setCode(0)
    searchResult.setMsg(msg)
  }

  /**
    *
    * get spellcheck list
    *
    * @param result
    * @return
    */
  private def getSpellCheckList(result: QueryResponse): java.util.HashMap[java.lang.String, java.util.List[java.lang.String]] = {
    if (result != null) {
      val spellChecks = new java.util.HashMap[java.lang.String, java.util.List[java.lang.String]]()
      val spellcheckResponse = result.getSpellCheckResponse
      if (spellcheckResponse != null) {
        val collateResults = spellcheckResponse.getCollatedResults
        val spellcheckCorrectionsSet = new util.HashSet[java.lang.String]()
        if (collateResults != null && collateResults.size() > 0) {
          collateResults.foreach { collation =>
            val missspellingCorrection = collation.getMisspellingsAndCorrections
            if (missspellingCorrection != null && missspellingCorrection.size() > 0) {
              missspellingCorrection.foreach { correction =>
                val originalWord = correction.getOriginal
                val correctionWord = correction.getCorrection
                spellcheckCorrectionsSet.add(originalWord + spellcheckSeparator + correctionWord)
              }
            }
          }
        }
        if (spellcheckCorrectionsSet.size() > 0) {
          spellcheckCorrectionsSet.foreach { spellcheck =>
            val spellcheckArray = spellcheck.split(spellcheckSeparator)
            val original = spellcheckArray(0)
            val correct = spellcheckArray(1)
            if (spellChecks.contains(original)) {
              val correctList = spellChecks.get(original)
              correctList.add(correct)
              spellChecks.put(original, correctList)
            } else {
              val initialCorrectList = new java.util.ArrayList[java.lang.String]()
              initialCorrectList.add(correct)
              spellChecks.put(original, initialCorrectList)
            }
          }
        }

      }
      spellChecks
    } else null
  }

  /**
    *
    * get highlighting list
    *
    * @param result
    * @return
    */
  private def getHighlightingList(result: QueryResponse): java.util.Map[java.lang.String, java.util.Map[java.lang.String, java.util.List[java.lang.String]]] = {
    if (result != null) {
      return result.getHighlighting
    } else null
  }

  /**
    *
    * get response Result
    *
    * @param result
    * @return
    */
  private def getSearchResult(result: QueryResponse, searchResult: SearchResult): java.util.List[util.Map[java.lang.String, Object]] = {
    val resultList: java.util.List[util.Map[java.lang.String, Object]] = new java.util.ArrayList[util.Map[java.lang.String, Object]]() //search result
    //get Result
    if (result != null) {
      // println("params:"+result.getHeader.get("params"))
      val response = result.getResults
      if (response != null) {
        if (searchResult != null) {
          val totalNum = response.getNumFound
          searchResult.setTotal(totalNum.toInt)
        }
        response.foreach { doc =>
          val resultMap: java.util.Map[java.lang.String, Object] = new java.util.HashMap[java.lang.String, Object]()
          val fields = doc.getFieldNames

          fields.foreach { fieldName =>
            val valueObj = doc.getFieldValue(fieldName)
            var isMult: Boolean = false
            breakable {
              multiValueArray.foreach { multiVl =>
                if (multiVl.equalsIgnoreCase(fieldName)) {
                  isMult = true
                  break
                }
              }
            }
            if (isMult) {
              val valueList = valueObj.asInstanceOf[java.util.List[Object]]
              resultMap.put(fieldName, valueList)
            }
            else resultMap.put(fieldName, valueObj)
          }
          if (!resultMap.isEmpty)
            resultList.add(resultMap)
        }
      }
    }
    resultList
  }


  /**
    *
    * get filter atrtributes by catagoryid
    *
    * @param catagoryId
    * @param cityId
    * @return FilterAttribute
    */
  private def searchFilterAttributeByCatagoryId(catagoryId: java.lang.Integer, cityId: java.lang.Integer): java.util.List[FilterAttribute] = {
    if (catagoryId != null && cityId != null) {
      val q = s"catid_s:$catagoryId"

      val fl = "filterId_s,attDescZh_s,range_s"

      val query: SolrQuery = new SolrQuery
      query.set("qt", "/select")
      query.setQuery(q)


      query.setFields(fl)

      query.addSort("attSort_ti", SolrQuery.ORDER.desc) //sort

      val r = solrClient.searchByQuery(query, defaultAttrCollection)
      var result: QueryResponse = null
      if (r != null) result = r.asInstanceOf[QueryResponse]
      val resultSearch = getSearchResult(result, null) //get response result

      var filterAttributeSearchResult: FilterAttributeSearchResult = null


      if (resultSearch != null) {
        val filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
        resultSearch.foreach { doc =>
          val attributeId = doc.get("filterId_s").toString
          val attributeName = doc.get("attDescZh_s").toString
          setAttributeNameById(attributeId, attributeName) //set cache

          //add facet and facet.query
          val ranges = doc.get("range_s")
          if (ranges != null && !ranges.toString.trim.equalsIgnoreCase("")) {
            //add facet query
            val rangesArray = ranges.toString.split("\\|")
            val rangeList = new util.ArrayList[String]()
            var count: Int = 0
            if (rangesArray.size > 0 && !rangesArray(0).trim.equalsIgnoreCase("") && !rangesArray(0).trim.equalsIgnoreCase("\"\"")) {
              rangesArray.foreach { query =>
                if (count == 0) {
                  val lU = query.split("-")
                  val minV = lU(0).trim
                  rangeList.add(s"[* TO ${minV}}")

                } else if (count == rangesArray.length - 1) {
                  val lU = query.split("-")
                  val maxV = lU(1).trim
                  rangeList.add(s"[${maxV} TO *}")
                }
                val rangeQ = query.replaceAll("-", " TO ")
                rangeList.add(s"[${rangeQ.trim}}")
                count += 1
              }
            }
            if (rangeList.size() > 0)
              filterFieldsValues.put(attributeId, rangeList)
            else filterFieldsValues.put(attributeId, null)

          } else {
            //just facet.field
            filterFieldsValues.put(attributeId, null)
          }
        }

        filterAttributeSearchResult = attributeFilterSearch(defaultCollection, null, catagoryId, cityId, null, null, filterFieldsValues, null, null).getData.asInstanceOf[FilterAttributeSearchResult]
      }

      if (filterAttributeSearchResult == null) return null
      else return filterAttributeSearchResult.getFilterAttributes

    } else null
  }


  /**
    *
    * @param catagoryId
    * @param cityId
    * @return FilterAttributeSearchResult
    */
  private def searchFilterAttributeAndResultByCatagoryId(collection: String = defaultCollection, attCollection: String = defaultAttrCollection, catagoryId: java.lang.Integer, cityId: java.lang.Integer): FilterAttributeSearchResult = {
    if (catagoryId != null && cityId != null) {
      var (collections: String, attrCollections: String) = setSwitchCollection(collection, attCollection)


      val q = s"catid_s:$catagoryId"

      val fl = "filterId_s,attDescZh_s,range_s"

      val query: SolrQuery = new SolrQuery
      query.set("qt", "/select")
      query.setQuery(q)


      query.setFields(fl)

      query.addSort("attSort_ti", SolrQuery.ORDER.desc) //sort

      val r = solrClient.searchByQuery(query, attrCollections)
      var result: QueryResponse = null
      if (r != null) result = r.asInstanceOf[QueryResponse]
      val resultSearch = getSearchResult(result, null) //get response result

      var filterAttributeSearchResult: FilterAttributeSearchResult = null


      if (resultSearch != null) {
        val filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
        resultSearch.foreach { doc =>
          val attributeId = doc.get("filterId_s").toString
          val attributeName = doc.get("attDescZh_s").toString
          setAttributeNameById(attributeId, attributeName) //set cache

          //add facet and facet.query
          val ranges = doc.get("range_s")
          if (ranges != null && !ranges.toString.trim.equalsIgnoreCase("")) {
            //add facet query
            val rangesArray = ranges.toString.split("\\|")
            val rangeList = new util.ArrayList[String]()
            var count: Int = 0
            if (rangesArray.size > 0 && !rangesArray(0).trim.equalsIgnoreCase("") && !rangesArray(0).trim.equalsIgnoreCase("\"\"")) {
              rangesArray.foreach { query =>
                if (count == 0) {
                  val lU = query.split("-")
                  val minV = lU(0).trim
                  rangeList.add(s"[* TO ${minV}}")

                } else if (count == rangesArray.length - 1) {
                  val lU = query.split("-")
                  val maxV = lU(1).trim
                  rangeList.add(s"[${maxV} TO *}")
                }
                val rangeQ = query.replaceAll("-", " TO ")
                rangeList.add(s"[${rangeQ.trim}}")
                count += 1
              }
            }
            if (rangeList.size() > 0)
              filterFieldsValues.put(attributeId, rangeList)
            else filterFieldsValues.put(attributeId, null)

          } else {
            //just facet.field
            filterFieldsValues.put(attributeId, null)
          }
        }

        filterAttributeSearchResult = attributeFilterSearch(collections, null, catagoryId, cityId, null, null, filterFieldsValues, null, null, null, false).getData.asInstanceOf[FilterAttributeSearchResult]
      }

      if (filterAttributeSearchResult == null) return null
      else return filterAttributeSearchResult

    } else null
  }


  /**
    *
    * @param catagoryId
    * @param cityId
    * @param keywords
    * @return
    */
  private def searchFilterAttributeAndResulAndSearchResulttByCatagoryIdAndKeywords(collection: String = defaultCollection, attrCollection: String = defaultAttrCollection, catagoryId: java.lang.Integer, cityId: java.lang.Integer, keywords: String, filters: java.util.Map[java.lang.String, java.lang.String], sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer, categoryIds: java.util.List[Integer] = null, isCameFromSearch: Boolean = true): FilterAttributeSearchResult = {
    var filterAttributeSearchResult: FilterAttributeSearchResult = null
    var (collections: String, attrCollections: String) = setSwitchCollection(collection, attrCollection)
    if (catagoryId != null && cityId != null && catagoryId != -1) {
      val q = s"catid_s:$catagoryId"

      val fl = "filterId_s,attDescZh_s,range_s"

      val query: SolrQuery = new SolrQuery
      query.set("qt", "/select")
      query.setQuery(q)


      query.setFields(fl)

      query.addSort("attSort_ti", SolrQuery.ORDER.asc) //sort

      val r = solrClient.searchByQuery(query, attrCollections)
      var result: QueryResponse = null
      if (r != null) result = r.asInstanceOf[QueryResponse]
      val resultSearch = getSearchResult(result, null) //get response result


      if (resultSearch != null) {
        val filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
        resultSearch.foreach { doc =>
          val attributeId = doc.get("filterId_s").toString
          val attributeName = doc.get("attDescZh_s").toString
          setAttributeNameById(attributeId, attributeName) //set cache

          //add facet and facet.query
          val ranges = doc.get("range_s")
          if (ranges != null && !ranges.toString.trim.equalsIgnoreCase("")) {
            //add facet query
            val rangesArray = ranges.toString.split("\\|")
            val rangeList = new util.ArrayList[String]()
            var count: Int = 0
            if (rangesArray.size > 0 && !rangesArray(0).trim.equalsIgnoreCase("") && !rangesArray(0).trim.equalsIgnoreCase("\"\"")) {
              rangesArray.foreach { query =>
                if (count == 0) {
                  val lU = query.split("-")
                  val minV = lU(0).trim
                  rangeList.add(s"[* TO ${minV}}")

                } else if (count == rangesArray.length - 1) {
                  val lU = query.split("-")
                  val maxV = lU(1).trim
                  rangeList.add(s"[${maxV} TO *}")
                }
                val rangeQ = query.replaceAll("-", " TO ")
                rangeList.add(s"[${rangeQ.trim}}")
                count += 1
              }
            }
            if (rangeList.size() > 0)
              filterFieldsValues.put(attributeId, rangeList)
            else filterFieldsValues.put(attributeId, null)

          } else {
            //just facet.field
            filterFieldsValues.put(attributeId, null)
          }
        }

        filterAttributeSearchResult = attributeFilterSearch(collections, keywords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, categoryIds, isCameFromSearch).getData.asInstanceOf[FilterAttributeSearchResult]
      }

      filterAttributeSearchResult

    } else {
      filterAttributeSearchResult = attributeFilterSearch(collections, keywords, catagoryId, cityId, sorts, null, null, start, rows, categoryIds, isCameFromSearch).getData.asInstanceOf[FilterAttributeSearchResult]
      filterAttributeSearchResult
    }
  }


  private def setSwitchCollection(collection: String, attrCollection: String): (String, String) = {
    var collections: String = collection
    var attrCollections: String = attrCollection

    if (SearchInterface.switchCollection) {
      if (SearchInterface.switchMg != null && !"null".equalsIgnoreCase(SearchInterface.switchMg.trim)) collections = SearchInterface.switchMg
      if (SearchInterface.switchSc != null && !"null".equalsIgnoreCase(SearchInterface.switchSc.trim)) attrCollections = SearchInterface.switchSc
    }
    (collections, attrCollections)
  }

  def replaceSense(valueString: String): String = {
    var relpaseString = valueString
    relpaseString = relpaseString.replaceAll("\\ ", "\\\\ ")
    arrayObj.foreach { s =>
      val sepaSense = s.split("=>")
      relpaseString = relpaseString.replaceAll(sepaSense(0).trim, sepaSense(1).trim)
    }
    relpaseString
  }


  def replaceAndFUpper(value: String): (String, String) = {
    var v1 = value.charAt(0).toUpper + value.substring(1)
    var v2 = value.charAt(0).toLower + value.substring(1)
    v1 = replaceSense(v1)
    v2 = replaceSense(v2)
    (v1, v2)
  }

  /**
    *
    * set filters
    *
    * @param filters
    * @param query
    */
  private def setFilters(filters: util.Map[String, String], query: SolrQuery): Unit = {

    if (filters != null && filters.size() > 0) {

      filters.foreach { fV =>
        val field = fV._1
        var value = fV._2
        if (value != null && !value.equalsIgnoreCase("")) {
          var valuesArray: Array[String] = null
          //multiply select
          breakable {
            for (i <- 0 to filterSplitArray.length - 1) {
              valuesArray = value.split(filterSplitArray(i).trim)
              if (valuesArray.length > 1) break
            }
          }
          var fqString = new StringBuilder()
          if (valuesArray != null && valuesArray.length > 1) {
            fqString.append("(")
            //t89_s:(memmert+OR+Memmert+OR+honeywell+OR+Honeywell)
            valuesArray.foreach { filterValue =>
              var value = filterValue.trim
              //fq=t89_s:(memmert OR Memmert OR honeywell OR Honeywell)
              letterSpaceProcess(value, fqString)
            }
            val fq = fqString.substring(0, fqString.lastIndexOf("OR") - 1) + ")"
            query.addFilterQuery(s"$field:$fq")
          } else {

            //fq=t89_s:(memmert OR Memmert OR honeywell OR Honeywell)
            fqString.append("(")
            letterSpaceProcess(value, fqString)
            val fq = fqString.substring(0, fqString.lastIndexOf("OR") - 1) + ")"
            query.addFilterQuery(s"$field:$fq")

          }


        }
      }
    }
  }


  private def letterSpaceProcess(orginalVal: String, fqString: StringBuilder): Unit = {
    var value = orginalVal
    val vA = value.split(" ")
    if (vA.length > 1) {
      val vS = new StringBuilder()
      val vS1 = new StringBuilder()
      vA.foreach { s =>
        if (Util.regex(s, "^[A-Za-z]+")) {
          var v1 = s.charAt(0).toUpper + s.substring(1)
          var v2 = s.charAt(0).toLower + s.substring(1)
          vS.append(v1)
          vS.append(" ")
          vS1.append(v2)
          vS1.append(" ")
        } else {
          vS.append(s)
          vS.append(" ")
          vS1.append(s)
          vS1.append(" ")
        }
      }
      if (!vS.isEmpty && !vS1.isEmpty) {
        var v1 = vS.toString().trim
        var v2 = vS1.toString().trim
        if (v1.equals(v2)) {
          v1 = replaceSense(v1)
          fqString.append(s"$v1 OR ")
        } else {
          v1 = replaceSense(v1)
          v2 = replaceSense(v2)
          fqString.append(s"$v1 OR $v2 OR ")
        }
      }

    } else {
      if (Util.regex(value, "^[A-Za-z]+")) {

        val (v1, v2) = replaceAndFUpper(value)

        fqString.append(s"$v1 OR $v2 OR ")
        // val fq = s"$field:($v1 OR $v2)"
        //query.addFilterQuery(fq)
      } else {
        value = replaceSense(value)
        fqString.append(s"$value OR ")
        // query.addFilterQuery(s"$field:$value")
      }
    }
  }

  /**
    *
    * set filters
    *
    * @param filtersNoChange
    * @param query
    */
  private def setFiltersBack(filtersNoChange: util.Map[String, String], query: SolrQuery): Unit = {

    if (filtersNoChange != null && filtersNoChange.size() > 0) {

      val filters = filtersNoChange.flatMap { case (k, v) =>
        val vA = v.split(" ")
        if (vA.length > 1) {
          val vS = new StringBuilder()
          val vS1 = new StringBuilder()
          vA.foreach { s =>
            if (Util.regex(s, "^[A-Za-z]+")) {
              var v1 = s.charAt(0).toUpper + s.substring(1)
              var v2 = s.charAt(0).toLower + s.substring(1)
              vS.append(v1)
              vS.append(" ")
              vS1.append(v2)
              vS1.append(" ")
            } else {
              vS.append(s)
              vS.append(" ")
              vS1.append(s)
              vS1.append(" ")
            }
          }
          Map(k -> vS.toString.trim, k -> vS1.toString.trim)
        }
        else Map(k -> v)
      }




      filters.foreach { fV =>
        val field = fV._1
        var value = fV._2
        if (value != null && !value.equalsIgnoreCase("")) {
          var valuesArray: Array[String] = null
          //multiply select
          breakable {
            for (i <- 0 to filterSplitArray.length - 1) {
              valuesArray = value.split(filterSplitArray(i).trim)
              if (valuesArray.length > 1) break
            }
          }

          if (valuesArray != null && valuesArray.length > 1) {
            val fqString = new StringBuilder()
            fqString.append("(")
            //t89_s:(memmert+OR+Memmert+OR+honeywell+OR+Honeywell)
            valuesArray.foreach { filterValue =>
              var value = filterValue.trim
              //fq=t89_s:(memmert OR Memmert OR honeywell OR Honeywell)
              if (Util.regex(value, "^[A-Za-z]+$")) {

                val (v1, v2) = replaceAndFUpper(value)

                fqString.append(s"$v1 OR $v2 OR ")
                // val fq = s"$field:($v1 OR $v2)"
                //query.addFilterQuery(fq)
              } else {
                value = replaceSense(value)
                fqString.append(s"$value OR ")
                // query.addFilterQuery(s"$field:$value")
              }
            }
            val fq = fqString.substring(0, fqString.lastIndexOf("OR") - 1) + ")"
            query.addFilterQuery(s"$field:$fq")
          } else {
            //fq=t89_s:(memmert OR Memmert OR honeywell OR Honeywell)
            if (Util.regex(value, "^[A-Za-z]+$")) {
              val (v1, v2) = replaceAndFUpper(value)
              val fq = s"$field:($v1 OR $v2)"
              query.addFilterQuery(fq)
            } else {
              value = replaceSense(value)
              query.addFilterQuery(s"$field:$value")
            }
          }


        }
      }
    }
  }

  /**
    *
    * search by keywords,must record searchlog for log analysis
    *
    * @param keyWords eg:螺丝钉
    * @param cityId   eg:111
    * @param sorts    eg:Map(price->desc,sales->desc,score->desc)
    * @param start    eg:0
    * @param rows     eg:10
    * @return SearchResult
    */
  private def queryByKeywords(keyWords: java.lang.String, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): SearchResult = {
    logInfo(s"search by keywords:$keyWords")
    val msg = new Msg()
    val searchResult = new SearchResult()
    /* if (keyWords == null && cityId == null) {
       msg.setMsg("keyWords and cityId not null")
       searchResult.setMsg(msg)
       return searchResult
     }*/
    /*if (keyWords == null) {
      msg.setMsg("keyWords not null")
      searchResult.setMsg(msg)
      return searchResult
    }*/
    if (cityId == null) {
      msg.setMsg("cityId not null")
      searchResult.setMsg(msg)
      return searchResult
    }

    //page
    var sStart: Int = 0
    var sRows: Int = 10

    if (start != null && start > 0) sStart = start
    if (rows != null && rows > 0) sRows = rows

    var keyWordsModel = "*:*"
    if (keyWords != null) {
      val keyWord = keyWords.trim.toLowerCase
      keyWordsModel = s"(original:$keyWord^50) OR (sku:$keyWord^50) OR (brandZh:$keyWord^200) OR (brandEn:$keyWord^200) OR (sku:*$keyWord*^11) OR (original:*$keyWord*^10) OR (text:$keyWord^2) OR (pinyin:$keyWord^0.002)"
    }

    val fq = s"isRestrictedArea:0 OR cityId:$cityId"


    val query: SolrQuery = new SolrQuery
    query.set("qt", "/select")
    query.setQuery(keyWordsModel)
    query.setFilterQueries(fq)

    //sort
    query.addSort("score", SolrQuery.ORDER.desc)

    if (sorts != null && sorts.size() > 0) {
      // eg:  query.addSort("price", SolrQuery.ORDER.desc)
      sorts.foreach { sortOrder =>
        val field = sortOrder._1
        val orderString = sortOrder._2.trim
        var order: ORDER = null
        orderString match {
          case "desc" => order = SolrQuery.ORDER.desc
          case "asc" => order = SolrQuery.ORDER.asc
          case _ => SolrQuery.ORDER.desc
        }
        query.addSort(field, order)
      }
    }

    //page
    query.setStart(sStart)
    query.setRows(sRows)

    val r = solrClient.searchByQuery(query, defaultCollection)
    var result: QueryResponse = null
    if (r != null) result = r.asInstanceOf[QueryResponse]

    val resultSearch = getSearchResult(result, searchResult) //get response result
    if (resultSearch != null && resultSearch.size > 0) searchResult.setResult(resultSearch) //set response resut
    searchResult
  }

}


object testSearchInterface {


  def main(args: Array[String]) {

    //searchByKeywords
    //  testMoniSearchKeywords

    //testSearchFilterAttributeByCatagoryId
    //testAttributeFilterSearch

    //testSearchBrandsByCatoryId

    //testSuggestByKeyWords

    testRecordSearchLog

    //testCountKeywordInDocs


    //testSplit
    //testRegex
    // testReplace
    // testMaxInt
    //testSubString


    //testSearchByCategoryId

    //testMoniSearchKeywordsFilters

    //testQDotOrSearch

    //testCatidsByKeywords()


    //testNullConvert()
  }

  def testNullConvert() = {
    println(null.asInstanceOf[FilterAttributeSearchResult])
  }

  def testCatidsByKeywords() = {
    val result = SearchInterface.getCategoryIdsByKeyWords("mergescloud_prod", "博世", 321, null)
    println(result)
    val result1 = SearchInterface.getCategoryIdsByKeyWords("mergescloud_prod", "工具", 321, null)
    val result2 = SearchInterface.getCategoryIdsByKeyWords("mergescloud_prod", "工具", 321, null)
    println(result1)
  }

  def testQDotOrSearch() = {

    val result3 = SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆锯片", 363, null, null, 0, 10)

    val categoryResult = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "圆锯片", null, 321, null, null, null, 0, 10, true)
    //  Thread.sleep(1000 * 60 * 1)
    val categoryResult1 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "圆锯片", null, 321, null, null, null, 0, 10, true)

    val categoryResult2 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "圆锯片", null, 321, null, null, null, 0, 10, true)
    //  Thread.sleep(1000 * 60 * 1)
    val categoryResult3 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "圆锯片", null, 321, null, null, null, 0, 10, true)
    println(categoryResult3)


  }

  def testMoniSearchKeywordsFilters() = {

    val categoryResult = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "绝缘扳手", 4318, 321, null, null, null, 0, 10, true)

    val filters = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters.put("da_2955_s", "Memmert")
    //filters.put("da_1186_s", "1/2\\\" <-> 12\\\"")
    // filters.put("da_1186_s", "1/2\\\\\\\"")
    val vs = "1/2\""
    val s = "\""
    val re = "\\\\\""
    val csc = vs.replaceAll(s, re)
    // filters.put("da_1186_s", "1/2\\\"")
    //  filters.put("da_1186_s", csc)
    filters.put("da_1186_s", "1/2\" -> 10\"")

    // filters.put("da_1186_s", "10\\\" <-> 12\\\"")
    var filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues.put("da_89_s", null)
    filterFieldsValues.put("da_1186_s", null)
    filterFieldsValues.put("da_3095_s", null)
    filterFieldsValues.put("da_528_s", null)
    filterFieldsValues.put("da_25_s", null)


    val categoryResultSortFilter = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "绝缘扳手", 4318, 321, null, filters, filterFieldsValues, 0, 10, false)

    val categoryResult1 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 15905, 321, null, null, null, 0, 10, true)

    val filters1 = new java.util.HashMap[java.lang.String, java.lang.String]()
    // filters1.put("da_2955_s", "Memmert")
    filters1.put("da_89_s", "worksafe")

    var filterFieldsValues1 = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues1.put("da_89_s", null)
    filterFieldsValues1.put("da_2955_s", null)
    filterFieldsValues1.put("da_1430_s", null)
    filterFieldsValues1.put("da_1025_s", null)
    val categoryResultSortFilter1 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 15905, 321, null, filters1, filterFieldsValues1, 0, 10, false)


    val categoryResult2 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 15093, 321, null, null, null, 0, 10, true)

    val filters2 = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters.put("da_2955_s", "Memmert")
    val daokan = "dow corning/道康宁"
    val cDaoKan = daokan.replaceAll(" ", "\\ ")
    // filters2.put("da_89_s", "Dow\\ Corning/道康宁")
    filters2.put("da_89_s", cDaoKan)

    var filterFieldsValues2 = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues2.put("da_89_s", null)
    filterFieldsValues2.put("da_1186_s", null)

    val categoryResultSortFilter2 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 15093, 321, null, filters2, filterFieldsValues2, 0, 10, false)

    println(categoryResultSortFilter2)


    val categoryResult3 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 16559, 321, null, null, null, 0, 10, true)

    val filters3 = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters3.put("da_2955_s", "公制\\（metric\\)")
    filters3.put("da_2955_s", "公制（metric)")

    var filterFieldsValues3 = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues3.put("da_2955_s", null)

    val categoryResultSortFilter3 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 16559, 321, null, filters3, filterFieldsValues3, 0, 10, false)

    println(categoryResultSortFilter3)


  }


  def testMoniSearchKeywords() = {
    // val result = SearchInterface.searchByKeywords("mergescloud", "screencloud", "防毒面具", 363, null, null, 0, 10)
    // val categoryResult = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "防毒面具", 2324, 321, null, null, null, 0, 10, true)
    val result = SearchInterface.searchByKeywords("mergescloud", "screencloud", "实验室产品", 3, null, null, 0, 10)

    val categoryResult = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "实验室产品", 903, 321, null, null, null, 0, 10, true)

    println(result)

    val categoryResultSort = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "防毒面具", 2324, 321, null, null, null, 0, 10, true)

    val filters = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters.put("da_2955_s", "Memmert")
    filters.put("da_89_s", "华德液压")

    var filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues.put("da_7894_s", null)
    filterFieldsValues.put("da_2955_s", null)
    filterFieldsValues.put("da_2477_s", null)
    filterFieldsValues.put("da_2223_s", null)
    filterFieldsValues.put("da_1127_s", null)
    filterFieldsValues.put("da_281_s", null)
    filterFieldsValues.put("da_155_s", null)
    filterFieldsValues.put("da_89_s", null)

    val categoryResultSort1 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 11956, 321, null, null, filterFieldsValues, 0, 10, true)
    val categoryResultSortFilter = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 11956, 321, null, filters, filterFieldsValues, 0, 10, false)

    println(categoryResultSortFilter)


  }


  def testSearchByKeywords = {

    var sorts = new java.util.LinkedHashMap[java.lang.String, java.lang.String]
    //sorts.put("price", "desc")
    sorts.put("price", "asc")
    //sorts.put("score", "desc")
    //  val result = SearchInterface.searchByKeywords("防护口罩", 456, sorts, 0, 10)
    //val result = SearchInterface.searchByKeywords("西格玛", 363, null, 0, 10)
    //val result = SearchInterface.searchByKeywords("1234567", 363, null, 0, 10)
    var starTime = System.currentTimeMillis()
    // val result1 = SearchInterface.searchByKeywords("西格玛", 363, null, 0, 10)
    //val result2 = SearchInterface.searchByKeywords("LAA001", 363, null, 0, 10)
    val result3 = SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆筒", 363, null, sorts, 0, 10)
    sorts = new java.util.LinkedHashMap[java.lang.String, java.lang.String]
    sorts.put("price", "asc")
    val result4 = SearchInterface.searchByKeywords("mergescloud", "screencloud", "西格玛", 363, null, null, 0, 10)
    val result4_1 = SearchInterface.searchByKeywords("mergescloud", "screencloud", "xigema", 363, null, null, 0, 10)
    val result5 = SearchInterface.searchByKeywords("mergescloud", "screencloud", "3m", 363, null, sorts, 0, 10)

    val result6 = SearchInterface.searchByKeywords("mergescloud", "screencloud", "LAA00s1", 363, null, sorts, 0, 10)
    var endTime = System.currentTimeMillis()
    println(result3)
    println(endTime - starTime)

    starTime = System.currentTimeMillis()
    val result7 = SearchInterface.searchByKeywords("mergescloud_prod", "screencloud_prod", "欧文", 363, null, sorts, 0, 10)
    endTime = System.currentTimeMillis()
    println("cost:" + (endTime - starTime))
    println(result7)
  }

  def queryByKeywords = {

    val sorts = new java.util.HashMap[java.lang.String, java.lang.String]
    sorts.put("price", "desc")
    //sorts.put("score", "desc")
    //  val result = SearchInterface.searchByKeywords("防护口罩", 456, sorts, 0, 10)
    //val result = SearchInterface.searchByKeywords("西格玛", 363, null, 0, 10)
    val result = SearchInterface.searchByKeywords("mergescloud", "screencloud", "优特", 363, null, null, 0, 10)
    val starTime = System.currentTimeMillis()
    // SearchInterface.queryByKeywords("优特", 363, null, 0, 10)
    val endTime = System.currentTimeMillis()
    println(result)
    println(endTime - starTime)
  }

  def testSearchByCategoryId() = {
    //val result = SearchInterface.searchByCategoryId("mergescloud",14887, 321, null, 0, 10)
    //  println(result)

  }

  def testSearchFilterAttributeByCatagoryId() = {
    //val result = SearchInterface.searchFilterAttributeByCatagoryId(1001739, 456)
    // val result = SearchInterface.searchFilterAttributeByCatagoryId(2660, 321)
    //val result = SearchInterface.searchFilterAttributeByCatagoryId(1225, 321)
    //  println(result)
  }

  def testAttributeFilterSearch = {
    //keywords catid cityid sorts filters filterFieldsValues start rows
    val sorts = new java.util.HashMap[java.lang.String, java.lang.String]
    sorts.put("price", "asc")
    sorts.put("score", "desc")

    val filters = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters.put("t89_s", "Memmert")
    filters.put("t89_s", "Memmert <-> honeywell")
    filters.put("t87_tf", "[0 TO *}")

    var filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues.put("t89_s", null)
    val rangeList = new util.ArrayList[String]()
    rangeList.add("[* TO 0}")
    rangeList.add("[0 TO 10}")
    rangeList.add("[10 TO 20}")
    rangeList.add("[20 TO 30}")
    rangeList.add("[30 TO *}")
    filterFieldsValues.put("t87_tf", rangeList)
    //SearchInterface.attributeFilterSearch(null, 1001739, 456, sorts, null, filterFieldsValues, 0, 10)
    // val result = SearchInterface.attributeFilterSearch(null, 1001739, 456, sorts, filters, filterFieldsValues, 0, 10)
    // val result = SearchInterface.attributeFilterSearch("3M", 1001739, 456, sorts, filters, filterFieldsValues, 0, 10)

    var filters1 = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters.put("t89_s", "Memmert")
    //filters1.put("da_1385_s", "1/4")
    /// filters1.put("da_89_s", "亚德客")
    // filters1.put("da_2306_s", "附锁型")
    //filters1.put("da_2178_s", "二位三通")

    var filterFieldsValues1 = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues1.put("da_1385_s", null)
    filterFieldsValues1.put("da_2178_s", null)
    filterFieldsValues1.put("da_2306_s", null)
    filterFieldsValues1.put("da_89_s", null)


    // val result = SearchInterface.attributeFilterSearch(null, 1225, 321, null, filters1, filterFieldsValues1, 0, 10)

    // val result = SearchInterface.attributeFilterSearch(1225, 321)
    /* val result = SearchInterface.attributeFilterSearch(null,1225, 321,null,null,null,null,null,true)
     val result1 = SearchInterface.attributeFilterSearch(null, 1225, 321, null, filters1, filterFieldsValues1, 0, 10,false)*/

    val categoryResult = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 15760, 321, null, null, null, null, null, true)

    val result = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 521, 321, null, null, null, null, null, true)


    val result2 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, null, 321, null, null, null, 0, 2, true)



    //attributeFilterSearch(collection: String, keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.Map[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false)


    var result5 = SearchInterface.attributeFilterSearch("mergescloud", null, null, 321, null, null, null, 0, 3, null, true)
    result5 = SearchInterface.attributeFilterSearch("mergescloud", null, null, 321, null, null, null, 0, 3, null, true)


    var result6 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 1237, 321, null, null, null, 0, 12, true)
    result6 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", null, 1237, 321, null, null, null, 0, 12, true)

    filters1 = new java.util.HashMap[java.lang.String, java.lang.String]()
    //filters1.put("da_2955_s",null)
    filters1.put("brandId", "203")
    filterFieldsValues = new util.LinkedHashMap[java.lang.String, util.List[java.lang.String]]()
    filterFieldsValues.put("brandId", null)
    val result3 = SearchInterface.attributeFilterSearch("mergescloud", "screencloud", "大", -1, 321, sorts, filters1, filterFieldsValues, 0, 10, true)



    println(result)
  }

  def testSearchBrandsByCatoryId() = {
    val result = SearchInterface.searchBrandsByCatoryId("mergescloud", 2660, 321)
    val result1 = SearchInterface.searchBrandsByCatoryId("mergescloud", 2660, 321)
    println(result)
  }

  def testSuggestByKeyWords() = {

    val result = SearchInterface.suggestByKeyWords("mergescloud", "史丹利", 456)
    var startTime = System.currentTimeMillis().toDouble
    SearchInterface.suggestByKeyWords("mergescloud", "dai", 456)
    var endTime = System.currentTimeMillis().toDouble
    println(s"cost time:${(endTime - startTime) / 1000}s")

    startTime = System.currentTimeMillis().toDouble
    SearchInterface.suggestByKeyWords("mergescloud", "dai", 456)
    endTime = System.currentTimeMillis().toDouble
    println(s"cost time:${(endTime - startTime) / 1000}s")
    Thread.sleep(1000 * 2)

    startTime = System.currentTimeMillis().toDouble
    SearchInterface.suggestByKeyWords("mergescloud", "dai", 456)
    endTime = System.currentTimeMillis().toDouble
    println(s"cost time:${(endTime - startTime) / 1000}s")

    println(result)
  }

  def testRecordSearchLog() = {
    /**
      * keyWords
      * appKey
      * clientIp
      * userAgent
      * sourceType
      * cookies
      * userId
      */
    val hotKeywords = SearchInterface.popularityKeyWords()
    val startTime = System.currentTimeMillis()
    SearchInterface.recordSearchLog("sd", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("第三方V", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("问", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("32", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("额外", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("稍等", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("松岛枫", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("士大夫", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("水电费", "swe2323", null, "Useragent", "android", null, "undn3")
    SearchInterface.recordSearchLog("定位", "swe2323", null, "Useragent", "android", null, "undn3")
    Thread.sleep(3000)
    val hotKeywords1 = SearchInterface.popularityKeyWords()
    //Thread.sleep(6000)
    SearchInterface.recordSearchLog("密封", "swe2323", null, "Useragent", "android", null, "undn3")
    Thread.sleep(3000)
    val hotKeyword2 = SearchInterface.popularityKeyWords()
    val endTime = System.currentTimeMillis()
    println(endTime - startTime)


  }

  def testSplit() = {
    //  val testString = "t87_tf:[* TO 0}"
    //  val array = testString.split(":")
    //  println(array)


    val spac: String = "skdl"
    val sp1 = spac.split(" ")
    println(sp1)

    val test = "memmert<->Honeywell"
    val tesArray = test.split("<->")
    println(tesArray.toString)

    val testOrString = "memmert OR Honeywell"
    val testOrStringArray = testOrString.split("OR")
    println(testOrStringArray)

    val vls = "_32"
    val valsArray = vls.split("_")
    println(valsArray)


  }


  def testReplace() = {
    var value = "dow corning/道康宁"
    val value1 = value.split(" ")
    println(value1)
    if (Util.regex(value1(0), "^[A-Za-z]+")) println("true") else println("false")
    if (Util.regex(value1(1), "^[A-Za-z]+")) println("true") else println("false")

  }

  def testRegex() = {
    // val value = "中Mmemert"
    //val value = "[Mmemert"
    // var value = "mmemert"
    var value = "dow\\ corning/道康宁"

    if (Util.regex(value, "^[A-Za-z]+$")) println("true") else println("false")

    if (Util.regex(value, "^[A-Za-z]+$")) {
      val v1 = value.charAt(0).toUpper + value.substring(1)
      val v2 = value.charAt(0).toLower + value.substring(1)
      println(v1 + "=" + v2)
    }
    val keyWords = "h的elloword"
    if (Util.regex(keyWords, "(^[a-zA-Z]+$)")) {
      println("英文或拼音")
    } else {
      println("非拼音或英文")
    }
  }

  def testCountKeywordInDocs() = {
    //SearchInterface.countKeywordInDocs("3m", new SolrQuery(), 456)
  }

  def testMaxInt() = {
    val v = java.lang.Integer.MAX_VALUE
    println(v)
  }

  def testSubString() = {
    val s = new StringBuilder("(memmert OR Memmert OR honeywell OR Honeywell OR ")
    val sS = s.substring(0, s.lastIndexOf("OR") - 1)
    val laS = sS + ")"
    println(laS)

  }


}
