package search.common.word2vec.ansj

import org.ansj.library.UserDefineLibrary
import org.ansj.splitWord.analysis.{NlpAnalysis, IndexAnalysis, ToAnalysis, BaseAnalysis}
import search.common.util.Logging
import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2016/8/1.
  */
object AnsjTest extends Logging{

  def main(args: Array[String]) {
    //testSegmentWord
   // UserDefineLibrary
   testSplitWordByToAnalysis
    userDefinedCut
  }

  def userDefinedCut() = {
    UserDefineLibrary.insertWord("中国电子集团概念板","userDefine",1000)
    val str = "我测试的概念版本,中国电子集团概念板"
    val terms = ToAnalysis.parse(str)
    println(terms)
  }

  def testSegmentWord()= {


    val str = "洁面仪配合洁面深层清洁毛孔 清洁鼻孔面膜碎觉使劲挤才能出一点点皱纹 脸颊毛孔修复的看不见啦 草莓鼻历史遗留问题没辙 脸和脖子差不多颜色的皮肤才是健康的 长期使用安全健康的比同龄人显小五到十岁 28岁的妹子看看你们的鱼尾纹" ;

    println(BaseAnalysis.parse(str))
    val terms = ToAnalysis.parse(str)
    println(terms)
    val keyWords = terms.map(_.getName).filter(_.length>1).toSet
   // println(DicAnalysis.parse(str))
    println(IndexAnalysis.parse(str))
   println(NlpAnalysis.parse(str))
  }

  def testSplitWordByToAnalysis() = {
    logInfo("testSplitWordByToAnalysis")
    var startTime = System.currentTimeMillis()
    val str = "中国电子集团概念板将很快就死定了房价"
    var terms = ToAnalysis.parse(str)
    println(terms)
    var keyWords = terms.map(_.getName).filter(_.length>1).toSet
    println(keyWords)
    var endTime = System.currentTimeMillis()
    println("耗时："+(endTime-startTime))
    terms = ToAnalysis.parse(str)
    terms.map(_.getName).filter(_.length>1).toSet
    println("耗时："+(System.currentTimeMillis()-endTime))

  }

}
