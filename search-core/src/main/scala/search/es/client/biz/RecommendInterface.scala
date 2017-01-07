package search.es.client.biz

import java.util

import search.common.cache.impl.LocalCache
import search.common.entity.bizesinterface.BaseStock
import search.common.entity.searchinterface.NiNi
import search.common.util.Util

import scala.collection.JavaConversions._

/**
  *
  * 推荐
  * Created by soledede.weng on 2017-01-06.
  */
private[search] object RecommendInterface {


  BizeEsInterface.init()

  /**
    * 根据主题获取主题相关的股票列表
    *
    * @param topic
    * @param num
    * @return
    */
  def wrapstockRecommendByTopic(topic: String, num: Int = -1): NiNi = {
    Util.caculateCostTime {
      stockRecommendByTopic(topic, num)
    }
  }

  /**
    * 根据主题获取主题相关的股票列表
    * @param topic
    * @param num
    * @return
    */

  def stockRecommendByTopic(topic: String, num: Int = -1): java.util.Collection[BaseStock] = {
    val list = new util.ArrayList[BaseStock]()
    if (topic == null || topic.trim.equalsIgnoreCase(""))
      return list
    if (!LocalCache.topic2StockCodesCache.contains(topic.trim)) return list
    //获取该概念对应的股票code列表
    val relevantCodes = LocalCache.topic2StockCodesCache(topic.trim)
    val baseStockList = relevantCodes.map { code =>
      val stockName = LocalCache.codeToCompanyNameCache(code)
      LocalCache.baseStockCache(stockName)
    }

    if (num <= 0) return baseStockList
    //查询所有
    baseStockList.take(num)
  }


  /**
    * 根据关键词（股票中文名或者新闻关键词）推荐概念
    *
    * @param keyword
    * @param num
    * @return
    */
  def wrapTopicRecommendByKeyword(keyword: String, num: Int = 10): NiNi = {
    Util.caculateCostTime {
      topicRecommendByKeyword(keyword, num)
    }
  }

  /**
    * recommend related topics by keyword(eg: stock name,stock code,news keyword or questioin)
    *
    * @param keyword
    * @param num
    * @return
    */
  def topicRecommendByKeyword(keyword: String, num: Int): java.util.Set[String] = {
    //get stock entity from local cache
    val name2StockCache = LocalCache.baseStockCache
    if (name2StockCache.contains(keyword)) {
      //recommend topic from stockName
      val baseStock: BaseStock = name2StockCache.getOrElse(keyword, null)
      if (baseStock != null) {
        val stockCode = baseStock.getComCode //get company code
        if (LocalCache.codeToTopicSet.contains(stockCode)) {
          return LocalCache.codeToTopicSet(stockCode).take(num)
        } else return recommendTopicsByNews(keyword, num)
      } else return recommendTopicsByNews(keyword, num)
    } else {
      //recommend by search engine news
      recommendTopicsByNews(keyword, num)
    }
  }


  /**
    * recomment topics by news
    *
    * @param keyword
    * @param num
    * @return
    */
  private def recommendTopicsByNews(keyword: String, num: Int): java.util.Set[String] = {
    val recommendSets = new util.HashSet[String]()
    val queryResult = BizeEsInterface.queryNews(keyword, 0, 40, 12)
    if (queryResult != null) {
      val searchResultList = queryResult.getResult
      if (searchResultList != null && searchResultList.size > 0) {
        searchResultList.foreach { mapResult =>
          if (mapResult.containsKey("topics")) {
            val topics = mapResult.get("topics")
            if (topics != null) {
              recommendSets.addAll(topics.asInstanceOf[util.Collection[String]])
            }
          }
        }
      }
    }
    recommendSets.take(num)
  }

  def main(args: Array[String]) {
    //testTopicRecommendByKeyword
    teststockRecommendByTopic
  }

def teststockRecommendByTopic() = {
  val result = stockRecommendByTopic("虚拟现实")
  println(result)
}

  def testTopicRecommendByKeyword() = {
    val result = topicRecommendByKeyword("云计算", 10)
    println(result)
    val result1 = topicRecommendByKeyword("露天煤业", 10)
    println(result1)
  }


}
