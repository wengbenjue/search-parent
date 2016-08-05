package search.common.util

import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._

/**
 * Created by soledede on 2015/9/15.
 */
object Json4sHelp {


  def writeTest: JObject = {

    ("soledede" -> "hello ") ~
    ("soledede" -> "hello ") ~
    ("id" -> 1)
  }

}
