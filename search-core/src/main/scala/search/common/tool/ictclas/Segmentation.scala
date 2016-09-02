package search.common.tool.ictclas

import com.sun.jna.{Library, Native}

/**
  * Created by soledede.weng on 2016/9/2.
  */
trait Segmentation {
  val USER_DEFINE: String = "x"

  def addUserWord(userWord: String)

  def parse(paragraph: String, showNature: Boolean): String

}


