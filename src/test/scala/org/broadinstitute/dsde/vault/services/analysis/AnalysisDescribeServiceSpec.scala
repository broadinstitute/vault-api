package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.Analysis
import spray.httpx.SprayJsonSupport._
import org.broadinstitute.dsde.vault.services.analysis.DescribeJsonProtocol._
import spray.http.StatusCodes._

class AnalysisDescribeServiceSpec extends VaultFreeSpec with DescribeService {

  def actorRefFactory = system
  val path = "/analyses"
  val testId = "arbitrary_id"

  "DescribeAnalysisService" - {
    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {

        Get(path + "/" + testId) ~> describeRoute ~> check {
          status should equal(OK)
          // test response as raw string
          entity.toString should include(testId)
          // test response as json, unmarshaled into an object
          val respAnalysis = responseAs[Analysis]
          respAnalysis.id should equal(testId)
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

