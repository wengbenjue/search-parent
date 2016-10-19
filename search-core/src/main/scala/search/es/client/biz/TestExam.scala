package search.es.client.biz

/**
  * Created by soledede.weng on 2016/10/19.
  */
object TestExam {
  def main(args: Array[String]) {
    val event = "知识产权|1"
    val events = event.split("[\\|||]{1}")
    println(events)
  }

}
