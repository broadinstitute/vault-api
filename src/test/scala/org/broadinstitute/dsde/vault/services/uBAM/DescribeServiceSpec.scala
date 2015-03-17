package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{uBAM, uBAMJsonProtocol}
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.unmarshalling._
import spray.httpx.SprayJsonSupport._

/**
 * This is an integration test. It requires an existing uBam ID in dm-ci to function.
 */
class DescribeServiceSpec extends VaultFreeSpec with DescribeService {

  def actorRefFactory = system
  val path = "/ubams"
  val openAmResponse = getOpenAmToken.get
  val testingId = "a31f604e-a5f8-46ae-bdca-5efddc608fb2"

  "DescribeuBAMService" - {
    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {
        Get(path + "/" + testingId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> describeRoute ~> check {
          status should equal(OK)
          entity.toString should include(testingId)
          import uBAMJsonProtocol._
          entity.as[uBAM].isRight shouldBe true
        }
      }
    }

    "when calling GET to the " + path + " path with an unknown Vault ID" - {
      "should return a 404 not found error" in {
        Get(path + "/12345-67890-12345") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(describeRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/" + testingId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/arbitrary_id") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

  }

}

