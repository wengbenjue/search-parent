package search.recommend.rest

import java.io.{FileOutputStream, OutputStream, InputStream, ByteArrayInputStream}
import java.util.regex.Pattern

import akka.actor.{Actor}
import akka.util.ByteString
import com.alibaba.fastjson.util.Base64

import java.text.{ParseException, SimpleDateFormat}
import java.util.{Date}

import net.liftweb.json.{DateFormat, Formats}
import search.common.config.{RecommendConfiguration, SolrConfiguration, Configuration}
import search.recommend.domain.Failure
import search.common.entity.{ScreenCloud, MergeCloud, Msg}
import search.recommend.ui.RecommendUI
import search.common.util.Logging
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol._
import spray.http._
import spray.httpx.unmarshalling._
import spray.routing._
import net.liftweb.json.Serialization._
import spray.json.DefaultJsonProtocol
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http._
import HttpCharsets._
import MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.util._
import spray.httpx.LiftJsonSupport


import scala.collection.mutable.ListBuffer


/*object MyS extends DefaultJsonProtocol {
  implicit val rmsgFormat = jsonFormat2(RMsg)
}*/

case class RMsg(result: Seq[String], code: Int)

object RMsg {
  implicit val rmsgFormat = jsonFormat2(RMsg.apply)
}

case class RecommendParameters(var userId: Option[String], var catagoryId: Option[String], var brandId: Option[String], var docId: Option[String], var number: Int = 0)

object RecommendParameters {
  implicit val recomPFormat = jsonFormat5(RecommendParameters.apply)
}


case class Base64Post(start: String, size: String, filename: String, filedata: String)

object Base64Post {
  implicit val base64Post = jsonFormat4(Base64Post.apply)
}


/**
  *
  * REST Service actor.
  */
class RestRecommendServiceActor extends Actor with RestService {

  implicit def actorRefFactory = context

  def receive = if (openRecommend) runRoute(rest) else null.asInstanceOf[Actor.Receive]
}

/**
  * REST Service
  */
trait RestService extends HttpService with Logging with SolrConfiguration with RecommendConfiguration with spray.httpx.SprayJsonSupport {

  import SprayJsonSupport._

  //import RJsonProtocol._


  var defaultRecommendUI: RecommendUI = null



  if (openRecommend) {
    defaultRecommendUI = RecommendUI()
  }


  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = new Formats {
    val dateFormat = new DateFormat {
      val sdf = new SimpleDateFormat("yyyy-MM-dd")

      def parse(s: String): Option[Date] = try {
        Some(sdf.parse(s))
      } catch {
        case e: Exception => None
      }

      def format(d: Date): String = sdf.format(d)
    }
  }

  implicit val string2Date = new FromStringDeserializer[Date] {
    def apply(value: String) = {
      val sdf = new SimpleDateFormat("yyyy-MM-dd")
      try Right(sdf.parse(value))
      catch {
        case e: ParseException => {
          Left(MalformedContent("'%s' is not a valid Date value" format (value), e))
        }
      }
    }
  }

  implicit val recommendRejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse {
      response =>
        response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          write(Map("error" -> response.entity.asString))))
    } {
      RejectionHandler.Default(rejections)
    }
  }


  /*path("recommend" / "sku") {

       //
         post {
           entity(Unmarshaller(MediaTypes.`application/json`) {
             case httpEntity: HttpEntity =>
               read[RecommendParameters](httpEntity.asString(HttpCharsets.`UTF-8`))
           }){
             parameters: RecommendParameters =>
               ctx: RequestContext =>
                 handleRequest(ctx) {
                   if (parameters.number == null || parameters.number <= 0 || parameters.number > 80)
                     Right(Msg("推荐数量必须大于0,并且不能超过80个", -1))
                   else Right(defaultRecommendUI.recommendByUserIdOrCatagoryIdOrBrandId(parameters.userId, parameters.catagoryId, parameters.brandId, parameters.number))
                 }
           }
         }
     } ~*/

  val rest = respondWithMediaType(MediaTypes.`application/json`) {
    //"res" / Segment 匹配字符串
    //数字 LongNumber
    path("recommend" / "catagory" / Segment) {
      keyword =>
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              val catagory = defaultRecommendUI.recommendMostLikeCatagoryIdByKeywords(keyword)
              if (catagory == null) Right(Msg("未能匹配到合适类目!", -1))
              else Right(catagory)
            }
        }
    } ~
      path("recommends" / Segment / IntNumber) {
        //
        (userId, number) =>
          get {
            ctx: RequestContext =>
              handleRequest(ctx) {
                if (number == null || number <= 0 || number > 80)
                  Right(Msg("推荐数量必须大于0,并且不能超过80个", -1))
                else Right(defaultRecommendUI.recommendByUserId(userId, number))
              }
          }
      } ~
      path("recommend" / Segment / Segment / Segment / Segment / IntNumber) {
        //
        (userId, catagoryId, brandId, docId, number) =>
          get {
            ctx: RequestContext =>
              handleRequest(ctx) {
                if (number == null || number <= 0 || number > 80)
                  Right(Msg("推荐数量必须大于0,并且不能超过80个", -1))
                else Right(defaultRecommendUI.recommendByUserIdOrCatagoryIdOrBrandId(userId, catagoryId, brandId, docId, number))
              }
          }
      } ~
      path("recommend" / "sku") {

        //
        post {
          entity(as[RecommendParameters]) {
            parameters: RecommendParameters =>
              ctx: RequestContext =>
                handleRequest(ctx) {
                  if (parameters.number == null || parameters.number <= 0 || parameters.number > 80)
                    Right(Msg("推荐数量必须大于0,并且不能超过80个", -1))
                  else Right(defaultRecommendUI.recommendByUserIdOrCatagoryIdOrBrandId(parameters.userId.getOrElse(null), parameters.catagoryId.getOrElse(null), parameters.brandId.getOrElse(null), parameters.docId.getOrElse(null), parameters.number))
                }
          }
        }
      } ~
      path("mergecloud") {
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              Right(new MergeCloud())
            }
        }
      } ~
      path("mergeclouds") {
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              val list = new ListBuffer[MergeCloud]()
              val m1 = new MergeCloud()
              m1.id = "1"
              m1.isRestrictedArea = 1
              m1.price = 2452.4
              m1.categoryId3 = 1001739
              m1.t89_s = "百事通"
              m1.t87_tf = 100
              list += m1
              val m2 = new MergeCloud()
              m2.id = "2"
              m2.title = "奔驰轮胎,车主可随叫随到"
              m2.cityId = "4_5_9_243"
              m2.isRestrictedArea = 1
              m2.price = 2
              m2.categoryId3 = 1001739
              m2.t89_s = "世达"
              m2.t87_tf = -19
              list += m2
              val m3 = new MergeCloud()
              m3.id = "3"
              m3.cityId = "321_34_135_5"
              m3.title = "碳化火钳"
              m3.isRestrictedArea = 1
              m3.price = 349
              m3.categoryId4 = 1001739
              m3.t89_s = "世达"
              m3.t87_tf = 4
              list += m3
              val m4 = new MergeCloud()
              m4.id = "4"
              m4.isRestrictedArea = 1
              m4.price = 245
              m4.title = "螺旋式流花玻璃"
              m4.categoryId3 = 1001739
              m4.t89_s = "百事通"
              m4.t87_tf = 34
              list += m4
              val m5 = new MergeCloud()
              m5.id = "5"
              m5.title = "橡胶塑料袋10袋起卖"
              m5.cityId = "2_3562_42_90"
              m5.isRestrictedArea = 1
              m5.price = 98
              m5.categoryId3 = 1001739
              m5.t89_s = "世达"
              m5.t87_tf = 2
              list += m5
              val m6 = new MergeCloud()
              m6.id = "6"
              m6.cityId = "564"
              m6.title = "防寒马甲"
              m6.isRestrictedArea = 1
              m6.price = 84
              m6.categoryId3 = 1001739
              m6.t89_s = "福大"
              m6.t87_tf = 45
              list += m6

              val m7 = new MergeCloud()
              m7.id = "7"
              m7.price = 12
              m7.title = "防护眼睛配件"
              m7.categoryId4 = 1001739
              list += m7
              val m8 = new MergeCloud()
              m8.id = "8"
              m8.price = 30
              m8.title = "六角电锤"
              m8.categoryId3 = 1001739
              list += m8
              val m9 = new MergeCloud()
              m9.id = "9"
              m9.price = 19
              m9.title = "六角螺丝刀"
              m9.categoryId3 = 1001739
              m9.t89_s = "soledede"
              m9.t87_tf = 30
              list += m9

              val m10 = new MergeCloud()
              m10.id = "10"
              m10.price = 34
              m10.categoryId3 = 1001739
              m10.t89_s = "memmert"
              m10.title = "龙珠泡11w"
              m10.t87_tf = 36
              list += m10

              val m11 = new MergeCloud()
              m11.id = "11"
              m11.price = 3.4
              m11.title = "卤素灯1000w"
              m11.categoryId3 = 1001739
              m11.t89_s = "Memmert"
              m11.t87_tf = 14
              list += m11

              val m12 = new MergeCloud()
              m12.id = "12"
              m12.brandId = 8724
              m12.brandEn = "ie"
              m12.brandZh = "世达"
              m12.title = "带表量仪"
              list += m12

              val m13 = new MergeCloud()
              m13.id = "13"
              m13.brandId = 125
              m13.brandEn = "kwe"
              m13.brandZh = "蟋蟀"
              m13.title = "daide"
              list += m13

              val m14 = new MergeCloud()
              m14.id = "14"
              m14.brandId = 125
              m14.brandEn = "kwe"
              m14.brandZh = "蟋蟀"
              m14.title = "带灯按钮"
              list += m14

              val m15 = new MergeCloud()
              m15.id = "15"
              m15.brandId = 125
              m15.brandEn = "kwe"
              m15.brandZh = "蟋蟀"
              m15.title = "带盖尿液收集试管,带阀气缸,带护锥中心钻"
              list += m15


              Right(list)
            }
        }
      } ~
      path("screenclouds") {
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              val list = new ListBuffer[ScreenCloud]()
              val s1 = new ScreenCloud()
              list += s1
              val s2 = new ScreenCloud()
              s2.id = "1003484_t87_s"
              s2.filterId_s = "t87_tf"
              s2.attDescZh_s = "规格"
              s2.range_s = "0-10|10-20|20-30"
              list += s2
              Right(list)
            }
        }
      }
  }


  lazy val index =
    <html>
      <body>
        <h1>欢迎...</h1>
      </body>
    </html>


  /**
    * Handles an incoming request and create valid response for it.
    *
    * @param ctx         request context
    * @param successCode HTTP Status code for success
    * @param action      action to perform
    */
  protected def handleRequest(ctx: RequestContext, successCode: StatusCode = StatusCodes.OK)(action: => Either[Failure, _]) {
    action match {
      case Right(result: Object) =>
        ctx.complete(successCode, write(result))
      case Left(error: Failure) =>
        ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
      case _ =>
        ctx.complete(StatusCodes.InternalServerError)
    }
  }
}


