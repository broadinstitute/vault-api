package org.broadinstitute.dsde.vault.services.analysis

import java.util.concurrent.TimeUnit

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{AnalysisIngest, AnalysisIngestResponse}
import org.broadinstitute.dsde.vault.services.analysis.IngestJsonProtocol._
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCookie, HttpEntity, MediaTypes}
import spray.httpx.SprayJsonSupport._

import scala.concurrent.duration.FiniteDuration

class AnalysisIngestServiceSpec extends VaultFreeSpec with IngestService {

  def actorRefFactory = system

  val path = "/analyses"
  implicit val routeTestTimeout = RouteTestTimeout(new FiniteDuration(60, TimeUnit.SECONDS))
  val openAmResponse = getOpenAmToken.get

  "DescribeAnalysisService" - {
    "when calling POST to the " + path + " path" - {
      "should return an ID" in {
        val analysisIngest = new AnalysisIngest(
          input = List("123", "456", "789"),
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

    // TODO: this test will fail until the ingest service is truly implemented
    "when calling POST to the " + path + " path with invalid input" - {
      "should return a Bad Request error" ignore {
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

