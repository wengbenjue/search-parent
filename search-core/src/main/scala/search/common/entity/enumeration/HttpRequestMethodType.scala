package search.common.entity.enumeration

/**
 * The type of Http Request Method
 */
object HttpRequestMethodType  extends Enumeration{
  type Type = Value 
  val GET = Value("get")
  val POST = Value("post")
}