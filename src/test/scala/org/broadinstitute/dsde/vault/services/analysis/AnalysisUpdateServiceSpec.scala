package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.Analysis
import org.broadinstitute.dsde.vault.services.analysis.DescribeJsonProtocol._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class AnalysisUpdateServiceSpec extends VaultFreeSpec with DescribeService {

  def actorRefFactory = system
  val path = "/analyses"

  "DescribeAnalysisService" - {
    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {
        val testId = "arbitrary_id"
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
        Put(path + "/arbitrary_id") ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/arbitrary_id") ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }
  }

}

