package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.AnalysisIngestResponse
import org.broadinstitute.dsde.vault.services.analysis.IngestJsonProtocol._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class AnalysisIngestServiceSpec extends VaultFreeSpec with IngestService {

  def actorRefFactory = system
  val path = "/analyses"

  "DescribeAnalysisService" - {
    "when calling POST to the " + path + " path" - {
      "should return an ID" in {
        Post(path) ~> ingestRoute ~> check {
          status should equal(OK)
          // test response as raw string
          entity.toString should include("id")
          // test response as json, unmarshaled into an object
          val respAnalysis = responseAs[AnalysisIngestResponse]
          val createdId = java.util.UUID.fromString(respAnalysis.id)
          entity.toString should include(createdId.toString)
        }
      }
    }

    "when calling PUT to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Put(path) ~> sealRoute(ingestRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }

    "when calling GET to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Get(path) ~> sealRoute(ingestRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }
  }

}

