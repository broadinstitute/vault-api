package org.broadinstitute.dsde.vault

import org.broadinstitute.dsde.vault.services.HelloWorldService
import spray.http.StatusCodes._

class HelloWorldServiceSpec extends VaultFreeSpec with HelloWorldService {

  def actorRefFactory = system

  "HelloWorldService" - {
    "when calling GET to the /hello path" - {
      "should return a greeting" in {
        Get("/hello") ~> helloRoute ~> check {
          status should equal(OK)
          entity.toString should include("Hello World")
        }
      }
    }

    "when calling GET to the /kermit path" - {
      "should return not found" in {
        Get("/kermit") ~> helloRoute ~> check {
          handled should equal(false)
        }
      }
    }

    "when calling PUT to the /hello path" - {
      "should return a MethodNotAllowed error" in {
        Put("/hello") ~> sealRoute(helloRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

  }

}

