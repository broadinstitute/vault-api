package org.broadinstitute.dsde.vault

import org.broadinstitute.dsde.vault.services.HelloWorldService
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

class HelloWorldServiceSpec extends Specification with Specs2RouteTest with HelloWorldService {
  def actorRefFactory = system

  "HelloWorldService" should {

    "return a greeting for GET requests to the /hello path" in {
      Get("/hello") ~> helloRoute ~> check {
        responseAs[String] must contain("Hello World")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> helloRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the /hello path" in {
      Put("/hello") ~> sealRoute(helloRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }

}

