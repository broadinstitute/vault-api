package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{AnalysisIngest, AnalysisIngestResponse}
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCookie, HttpEntity, MediaTypes}

class AnalysisIngestServiceSpec extends VaultFreeSpec with IngestService {

  import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol.impAnalysisIngest
  import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol.impAnalysisIngestResponse
  import spray.httpx.SprayJsonSupport._
  def actorRefFactory = system

  val path = "/analyses"
  val openAmResponse = getOpenAmToken.get

  "AnalysisIngestService" - {
    "when calling POST to the " + path + " path with empty input and valid metadata" - {
      "should return an ID" in {
        val analysisIngest = new AnalysisIngest(
          input = List(),
          metadata = Map("ownerId" -> "testUser", "randomData" -> "7")
        )
        Post(path, analysisIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> ingestRoute ~> check {
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

    "when calling POST to the " + path + " path with invalid input" - {
      "should return a Bad Request error" in {
        val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
        Post(path, malformedEntity) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(ingestRoute) ~> check {
          status should equal(BadRequest)
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

