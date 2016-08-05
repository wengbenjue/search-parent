package search.es.client.similarity

import java.io.File

import search.common.config.Configuration
import search.common.util.{Util, Logging}
import search.common.word2vec.Word2VEC
import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2016/8/1.
  */
class DefaultSimilarityCaculateImpl extends SimilarityCaculate with Logging with Configuration {
  val word2VEC = new Word2VEC()
  val in = Util.getContextClassLoader.getResourceAsStream("model/hotnews_2016.bin")
  if (word2VecNewsfinalPath == null || "".equalsIgnoreCase(word2VecNewsfinalPath.trim))
    word2VEC.loadModel(in)
  else word2VEC.loadModel(word2VecNewsfinalPath)
  word2VEC.setTopNSize(topN)

  override def word2Vec(keywords: String, path: String = null): Seq[String] = {
    if (path != null && !path.trim.equalsIgnoreCase("")) {
      word2VEC.loadModel(path)
    }
    val revleventWords = word2VEC.distance(keywords)
    if (revleventWords == null || revleventWords.size() == 0) return Seq.empty[String]
    else revleventWords.map(_.name).toSeq

  }
}

object DefaultSimilarityCaculateImpl {
  def main(args: Array[String]) {
    testGetPathFromResource
    //testLoadModel
  }

  private def testGetPathFromResource() = {
    val in = Util.getContextClassLoader.getResourceAsStream("model/newsfinal11.bin")
    val word2VEC = new Word2VEC()
    word2VEC.loadModel(in)
    word2VEC.setTopNSize(4)
    println(word2VEC.distance("百度"))
  }

  private def testLoadModel() = {
    val word2VEC = new Word2VEC()
    //var path = Util.getContextClassLoader.getResource("model/newsfinal11.bin").getFile
    var path = "file:/D:/search.jar!/model/newsfinal11.bin"
    path = path.replaceAll("file:", "")
    println(path)
    word2VEC.loadModel(path)
    word2VEC.setTopNSize(4)
  }
}
