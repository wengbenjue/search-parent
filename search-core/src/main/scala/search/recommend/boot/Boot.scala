package search.recommend.boot

import akka.actor.{Props, ActorSystem}
import akka.io.IO
import search.common.config.{RecommendConfiguration, Configuration}
import search.recommend.rest.RestRecommendServiceActor
import spray.can.Http

object Boot extends App with RecommendConfiguration {

  // create an actor system for application
  implicit val system = ActorSystem("rest-service-recomend")

  // create and start rest service actor
  val restService = system.actorOf(Props[RestRecommendServiceActor], "rest-endpoint")

  // start HTTP server with rest service actor as a handler
  IO(Http) ! Http.Bind(restService, serviceHost, servicePort)
}