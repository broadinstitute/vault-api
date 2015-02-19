package org.broadinstitute.dsde.vault.services

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json.DefaultJsonProtocol._
import spray.json._
import spray.routing._

@Api(value = "/hello", description = "Hello World Service", produces = "application/json", position = 0)
trait HelloWorldService extends HttpService {

  val routes = helloRoute

  @ApiOperation(value = "Says Hello World", nickname = "hello", httpMethod = "GET", produces = "application/json, application/xml")
  def helloRoute =
    path("hello") {
      get {
        respondWithMediaType(`application/json`) {
          complete {
            new Tuple2("HelloWorldService", "Hello World").toJson.prettyPrint
          }
        }
      }
    }

}
