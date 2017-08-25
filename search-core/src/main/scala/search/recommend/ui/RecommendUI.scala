package search.recommend.ui

import search.recommend.domain.entity.RecommendResult
import search.common.entity.User

/**
  * Created by soledede on 2016/4/17.
  */
trait RecommendUI {

  def recommendByUser(user: User,number:Int): Seq[String] = null

  def recommendByUserId(userId: String,number:Int): Seq[RecommendResult] = null

  def recommendByCatagoryByUser(user: User,number:Int): Seq[String] = null

  def recommendMostLikeCatagoryIdByKeywords(keyword: String): String = null

  def recommendByUserIdOrCatagoryIdOrBrandId(userId: String,catagoryId: String,brandId: String,docId: String,number:Int):Seq[RecommendResult] = null
}

object RecommendUI{
  def apply(rType: String = "default"): RecommendUI = {
    rType match {
      case "default" => GeneralRecommendUIImpl()
    }
  }
}
