package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.Analysis
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

class AnalysisDescribeServiceSpec extends VaultFreeSpec with DescribeService {

  def actorRefFactory = system

  val path = "/analyses"
  val openAmResponse = getOpenAmToken.get
  val testId = "f222066b-a822-4d67-b946-3f486fc620ba"

  "DescribeAnalysisService" - {
    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {
        Get(path + "/" + testId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> describeRoute ~> check {
          status should equal(OK)
          // test response as raw string
          entity.toString should include(testId)
          // test that we can unmarshal into an object
          entity.as[Analysis].isRight shouldBe true
          // test the unmarshaled object's contents
          val respAnalysis = responseAs[Analysis]
          respAnalysis.id should equal(testId)
          respAnalysis.input shouldBe empty
          respAnalysis.files.get shouldBe empty
          respAnalysis.metadata should equal(Map("ownerId" -> "testUser"))
        }
      }
    }

    "DescribeAnalysisService" - {
      "when calling GET to the " + path + " path with an invalid Vault ID" - {
        "should return a Not Found error" in {
          Get(path + "/" + "unknown-not-found-id") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(describeRoute) ~> check {
            status should equal(NotFound)
          }
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/" + testId) ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/" + testId) ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }
  }

}

