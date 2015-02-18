package org.broadinstitute.dsde.vault

import akka.actor.Actor
import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.routing._

import spray.json._
import DefaultJsonProtocol._ // if you don't supply your own Protocol (see below)


//the actor which will accept request and distribute to other actors/objects
class HelloWorldServiceActor extends Actor with HelloWorldService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)

}

// this trait defines our service behavior independently from the service actor
@Api(value = "/", description = "Hello World Operation", produces = "application/json", position = 2)
trait HelloWorldService extends HttpService {

  val serviceValue = new Tuple2("HelloWorldService", "Hello World")

  @ApiOperation(httpMethod = "GET", response = classOf[String], value = "Returns a Hello World json object")
  val myRoute =
    path("") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            serviceValue.toJson.prettyPrint
          }
        }
      }
    }
}