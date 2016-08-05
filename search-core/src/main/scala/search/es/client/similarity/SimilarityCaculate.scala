package search.es.client.similarity

/**
  * Created by soledede.weng on 2016/8/1.
  */
private[search] trait SimilarityCaculate {

  def word2Vec(keywords: String,path: String= null): Seq[String]

}
