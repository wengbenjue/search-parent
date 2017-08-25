package search.common.cache

import search.common.config.SolrConfiguration
import search.common.entity.searchinterface.{Brand, FilterAttributeSearchResult}

import scala.collection.mutable.StringBuilder
import scala.collection.JavaConversions._

/**
  * Created by soledede on 2016/4/16.
  */
private[search] object SearchCache extends SolrConfiguration {
  val cache = KVCache("jRedis")
  val separator = "&_&"


  def cacheCategoryIdsByKeyWords(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], block: => java.util.List[Integer]): java.util.List[Integer] = {
    val cache = getCategoryIdsByKeyWordsCache(keyWords, cityId, filters)
    if (cache != null) cache
    else {
      val result = block
      if (result != null && result.size() > 0)
        putCategoryIdsByKeyWordsCache(keyWords, cityId, filters, result)
      result
    }
  }

  private def getCategoryIdsByKeyWordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String]): java.util.List[Integer] = {
    val stringBuilder = categoryIdsByKeyWordsCache(keyWords, cityId, filters)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[java.util.List[Integer]](stringBuilder.toString())
    else null
  }

  private def putCategoryIdsByKeyWordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], catList: java.util.List[Integer]): Unit = {
    val stringBuilder = categoryIdsByKeyWordsCache(keyWords, cityId, filters)
    if (stringBuilder != null && !stringBuilder.isEmpty && catList != null && catList.size() > 0)
      cache.put(stringBuilder.toString.trim, catList, cacheTime)
  }

  private def categoryIdsByKeyWordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String]): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("categoryIdsByKeyWordsCache").append(separator)
    if (keyWords != null && !keyWords.trim.equalsIgnoreCase(""))
      stringBuilder.append(keyWords.trim).append(separator)
    if (cityId != null && cityId > 0)
      stringBuilder.append(cityId).append(separator)
    if (filters != null && filters.size() > 0) {
      filters.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }
    stringBuilder
  }


  //search by keywords

  def cacheSearchByKeywords(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer, block: => FilterAttributeSearchResult): FilterAttributeSearchResult = {
    val cache = getSearchByKeywordsCache(keyWords, cityId, filters, sorts, start, rows)
    if (cache != null) cache
    else {
      val result = block
      if (result != null)
        putSearchByKeywordsCache(keyWords, cityId, filters, sorts, start, rows, result)
      result
    }
  }


  private def putSearchByKeywordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer, filterAttributeSearchResult: FilterAttributeSearchResult): Unit = {
    val stringBuilder = searchByKeywordsCache(keyWords, cityId, filters, sorts, start, rows)
    if (stringBuilder != null && !stringBuilder.isEmpty && filterAttributeSearchResult != null)
      cache.put(stringBuilder.toString.trim, filterAttributeSearchResult, cacheTime)
  }


  private def getSearchByKeywordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): FilterAttributeSearchResult = {
    val stringBuilder = searchByKeywordsCache(keyWords, cityId, filters, sorts, start, rows)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[FilterAttributeSearchResult](stringBuilder.toString())
    else null
  }

  private def searchByKeywordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, filters: java.util.Map[java.lang.String, java.lang.String], sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("searchByKeywordsCache").append(separator)
    if (keyWords != null && !keyWords.trim.equalsIgnoreCase(""))
      stringBuilder.append(keyWords.trim).append(separator)
    if (cityId != null && cityId > 0)
      stringBuilder.append(cityId).append(separator)
    if (filters != null && filters.size() > 0) {
      filters.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }

    if (sorts != null && sorts.size() > 0) {
      sorts.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }

    if (start != null && start >= 0 && rows != null && rows >= 0) {
      stringBuilder.append(start).append(separator)
      stringBuilder.append(rows).append(separator)
    }

    stringBuilder
  }


  //suggest


  def cacheSuggestByKeyWords(keyWords: java.lang.String, cityId: java.lang.Integer, block: => java.util.Map[java.lang.String, java.lang.Integer]): java.util.Map[java.lang.String, java.lang.Integer] = {
    val cache = getSuggestByKeyWordsCache(keyWords, cityId)
    if (cache != null) cache
    else {
      val result = block
      if (result != null)
        putSuggestByKeyWordsCache(keyWords, cityId, result)
      result
    }
  }

  private def putSuggestByKeyWordsCache(keyWords: java.lang.String, cityId: java.lang.Integer, suggestKeywordCount: java.util.Map[java.lang.String, java.lang.Integer]): Unit = {
    val stringBuilder = suggestByKeyWordsCache(keyWords, cityId)
    if (stringBuilder != null && !stringBuilder.isEmpty && suggestKeywordCount != null && !suggestKeywordCount.isEmpty)
      cache.put(stringBuilder.toString.trim, suggestKeywordCount, cacheTime)
  }


  private def getSuggestByKeyWordsCache(keyWords: java.lang.String, cityId: java.lang.Integer): java.util.Map[java.lang.String, java.lang.Integer] = {
    val stringBuilder = suggestByKeyWordsCache(keyWords, cityId)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[java.util.Map[java.lang.String, java.lang.Integer]](stringBuilder.toString())
    else null
  }

  private def suggestByKeyWordsCache(keyWords: java.lang.String, cityId: java.lang.Integer): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("suggestByKeyWordsCache").append(separator)
    if (keyWords != null && !keyWords.trim.equalsIgnoreCase(""))
      stringBuilder.append(keyWords.trim).append(separator)
    if (cityId != null && cityId > 0)
      stringBuilder.append(cityId).append(separator)
    stringBuilder
  }


  //search brands by catoryId
  def cacheSearchBrandsByCatoryId(catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer, block: => java.util.List[Brand]): java.util.List[Brand] = {
    val cache = getSearchBrandsByCatoryIdCache(catagoryId, cityId, sorts, start, rows)
    if (cache != null) cache
    else {
      val result = block
      if (result != null)
        putSearchBrandsByCatoryIdCache(catagoryId, cityId, sorts, start, rows, result)
      result
    }
  }


  private def putSearchBrandsByCatoryIdCache(catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer, brands: java.util.List[Brand]): Unit = {
    val stringBuilder = searchBrandsByCatoryIdCache(catagoryId, cityId, sorts, start, rows)
    if (stringBuilder != null && !stringBuilder.isEmpty && brands != null && brands.size() > 0)
      cache.put(stringBuilder.toString.trim, brands, cacheTime)
  }

  private def getSearchBrandsByCatoryIdCache(catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): java.util.List[Brand] = {
    val stringBuilder = searchBrandsByCatoryIdCache(catagoryId, cityId, sorts, start, rows)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[java.util.List[Brand]](stringBuilder.toString())
    else null
  }

  private def searchBrandsByCatoryIdCache(catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], start: java.lang.Integer, rows: java.lang.Integer): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("searchBrandsByCatoryIdCache").append(separator)
    if (catagoryId != null && catagoryId >= 0)
      stringBuilder.append(catagoryId).append(separator)
    if (cityId != null && cityId > 0)
      stringBuilder.append(cityId).append(separator)
    if (sorts != null && sorts.size() > 0) {
      sorts.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }
    if (start != null && start >= 0 && rows != null && rows >= 0) {
      stringBuilder.append(start).append(separator)
      stringBuilder.append(rows).append(separator)
    }
    stringBuilder
  }


  //search attributers  and search result from catagory, isCategoryTouch=true first catagory, isCategoryTouch=false  catagory filter search


  def cacheAttributeFilterSearch(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, isCategoryTouch: java.lang.Boolean, block: => FilterAttributeSearchResult): FilterAttributeSearchResult = {
    val cache = getAttributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isCategoryTouch)
    if (cache != null) cache
    else {
      val result = block
      if (result != null)
        putAttributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isCategoryTouch,result)
      result
    }
  }

  private def putAttributeFilterSearchCache(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, isCategoryTouch: java.lang.Boolean, filterAttributeSearchResult: FilterAttributeSearchResult): Unit = {
    val stringBuilder = attributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isCategoryTouch)
    if (stringBuilder != null && !stringBuilder.isEmpty && filterAttributeSearchResult != null)
      cache.put(stringBuilder.toString.trim, filterAttributeSearchResult, cacheTime)
  }

  private def getAttributeFilterSearchCache(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, isCategoryTouch: java.lang.Boolean): FilterAttributeSearchResult = {
    val stringBuilder = attributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isCategoryTouch)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[FilterAttributeSearchResult](stringBuilder.toString())
    else null
  }

  private def attributeFilterSearchCache(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, isCategoryTouch: java.lang.Boolean): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("attributeFilterSearchCache").append(separator)
    if (catagoryId != null && catagoryId >= 0)
      stringBuilder.append(catagoryId).append(separator)

    if (keyWords != null && !keyWords.trim.equalsIgnoreCase(""))
      stringBuilder.append(keyWords.trim).append(separator)
    if (cityId != null && cityId > 0)
      stringBuilder.append(cityId).append(separator)
    if (filters != null && filters.size() > 0) {
      filters.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }

    if (sorts != null && sorts.size() > 0) {
      sorts.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }

    if (start != null && start >= 0 && rows != null && rows >= 0) {
      stringBuilder.append(start).append(separator)
      stringBuilder.append(rows).append(separator)
    }

    if (filterFieldsValues != null && filterFieldsValues.size() > 0) {
      filterFieldsValues.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        if (v != null && v.size() > 0) {
          v.foreach { filterV =>
            if (filterV != null && filterV.trim.equalsIgnoreCase(""))
              stringBuilder.append(k.trim).append(separator)
          }
        }
      }
    }
    if(isCategoryTouch!=null)
    stringBuilder.append(isCategoryTouch.toString).append(separator)
    stringBuilder
  }




  //fulter attrribute search
  //search attributers  and search result from catagory, isCategoryTouch=true first catagory, isCategoryTouch=false  catagory filter search

  def cacheAttributeFilterSearch(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer, categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false, block: => FilterAttributeSearchResult): FilterAttributeSearchResult = {
    val cache = getAttributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isComeFromSearch)
    if (cache != null) cache
    else {
      val result = block
      if (result != null)
        putAttributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isComeFromSearch,result)
      result
    }
  }

  private def putAttributeFilterSearchCache(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer,categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false, filterAttributeSearchResult: FilterAttributeSearchResult): Unit = {
    val stringBuilder = attributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isComeFromSearch)
    if (stringBuilder != null && !stringBuilder.isEmpty && filterAttributeSearchResult != null)
      cache.put(stringBuilder.toString.trim, filterAttributeSearchResult, cacheTime)
  }

  private def getAttributeFilterSearchCache(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer,categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false): FilterAttributeSearchResult = {
    val stringBuilder = attributeFilterSearchCache(keyWords, catagoryId, cityId, sorts, filters, filterFieldsValues, start, rows, isComeFromSearch)
    if (stringBuilder != null && !stringBuilder.isEmpty)
      cache.getObj[FilterAttributeSearchResult](stringBuilder.toString())
    else null
  }

  private def attributeFilterSearchCache(keyWords: java.lang.String, catagoryId: java.lang.Integer, cityId: java.lang.Integer, sorts: java.util.Map[java.lang.String, java.lang.String], filters: java.util.Map[java.lang.String, java.lang.String], filterFieldsValues: java.util.LinkedHashMap[java.lang.String, java.util.List[java.lang.String]], start: java.lang.Integer, rows: java.lang.Integer,categoryIds: java.util.List[Integer] = null, isComeFromSearch: Boolean = false): StringBuilder = {
    val stringBuilder = new StringBuilder
    stringBuilder.append("attributeFilterSearchCache1").append(separator)
    if (catagoryId != null && catagoryId >= 0)
      stringBuilder.append(catagoryId).append(separator)

    if (keyWords != null && !keyWords.trim.equalsIgnoreCase(""))
      stringBuilder.append(keyWords.trim).append(separator)
    if (cityId != null && cityId > 0)
      stringBuilder.append(cityId).append(separator)
    if (filters != null && filters.size() > 0) {
      filters.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }

    if (sorts != null && sorts.size() > 0) {
      sorts.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        stringBuilder.append(v.trim).append(separator)
      }
    }

    if (start != null && start >= 0 && rows != null && rows >= 0) {
      stringBuilder.append(start).append(separator)
      stringBuilder.append(rows).append(separator)
    }

    if (filterFieldsValues != null && filterFieldsValues.size() > 0) {
      filterFieldsValues.foreach { case (k, v) =>
        stringBuilder.append(k.trim).append(separator)
        if (v != null && v.size() > 0) {
          v.foreach { filterV =>
            if (filterV != null && filterV.trim.equalsIgnoreCase(""))
              stringBuilder.append(k.trim).append(separator)
          }
        }
      }
    }
    if (categoryIds != null && categoryIds.size()>0) {
      stringBuilder.append("null").append(separator)
    }
    if(isComeFromSearch!=null)
    stringBuilder.append(isComeFromSearch.toString).append(separator)
    stringBuilder
  }


















}
