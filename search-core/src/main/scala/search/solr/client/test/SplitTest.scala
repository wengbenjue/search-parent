package search.solr.client.test

/**
  * Created by soledede on 2016/2/18.
  */
object SplitTest {

  def main(args: Array[String]) {
    val vals = "sfsdss|sdfsw".split("\\|") //multiValued Array
    println(vals.size)
  }

}
