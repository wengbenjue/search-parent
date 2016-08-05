package search.common.config

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
  * Created by soledede.weng on 2016/7/27.
  */
private[search] trait RecommendConfiguration extends Configuration {
  val recommendConfig = ConfigFactory.load("recommend.conf")
  /**
    * recommend
    */

  /** Host name/address to start service on. */
  lazy val serviceHost = Try(recommendConfig.getString("recommendConfig.host")).getOrElse("localhost")

  /** Port to start service on. */
  lazy val servicePort = Try(recommendConfig.getInt("recommendConfig.port")).getOrElse(8088)

  //open-off
  lazy val openRecommend = Try(recommendConfig.getBoolean("recommend.open")).getOrElse(false)
}
