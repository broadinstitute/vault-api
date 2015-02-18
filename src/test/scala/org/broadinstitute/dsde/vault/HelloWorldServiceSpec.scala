package org.broadinstitute.dsde.vault

import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

class HelloWorldServiceSpec extends Specification with Specs2RouteTest with HelloWorldService {
  def actorRefFactory = system

  "HelloWorldService" should {

    "return a greeting for GET requests to the root path" in {
      Get() ~> myRoute ~> check {
        responseAs[String] must contain("Hello World")
      }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/kermit") ~> myRoute ~> check {
        handled must beFalse
      }
    }

    "return a MethodNotAllowed error for PUT requests to the root path" in {
      Put() ~> sealRoute(myRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET"
      }
    }
  }

}

