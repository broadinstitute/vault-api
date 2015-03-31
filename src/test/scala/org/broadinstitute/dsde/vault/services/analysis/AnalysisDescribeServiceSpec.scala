package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{AnalysisIngestResponse, AnalysisIngest, AnalysisUpdate, Analysis}
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

class AnalysisDescribeServiceSpec extends VaultFreeSpec with AnalysisDescribeService with AnalysisUpdateService with AnalysisIngestService {

  override val routes = analysisDescribeRoute

  def actorRefFactory = system

  val path = "/analyses"
  val openAmResponse = getOpenAmToken.get
  var testId = "invalid_UUID"

  "DescribeAnalysisService" - {
    "when calling POST to the ingest path in order to set up" - {
      "should return as OK" in {
        val ingestPath = "/analyses"
        val analysisIngest = new AnalysisIngest(
          input = List(),
          metadata = Map("ownerId" -> "testUser", "randomData" -> "7")
        )
        Post(ingestPath, analysisIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> analysisIngestRoute ~> check {
          status should equal(OK)
          val respAnalysis = responseAs[AnalysisIngestResponse]
          testId = respAnalysis.id
        }
      }
    }

    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {
        Get(path + "/" + testId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> analysisDescribeRoute ~> check {
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
          respAnalysis.metadata should equal(Map("ownerId" -> "testUser", "randomData" -> "7"))
        }
      }
    }

    "when calling POST to the update path in order to add completed files" - {
      "should return as OK" in {
        val updatePath = s"/analyses/%s/outputs"
        val analysisUpdate = new AnalysisUpdate(files = Map("vcf" -> "path/to/ingest/vcf", "bai" -> "path/to/ingest/bai", "bam" -> "path/to/ingest/bam"))
        Post(updatePath.format(testId), analysisUpdate) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> analysisUpdateRoute ~> check {
          status should equal(OK)
          val analysisResponse = responseAs[Analysis]
          val files = responseAs[Analysis].files
          analysisResponse.id should be (testId)
          files.get isDefinedAt "bam"
          files.get isDefinedAt "bai"
          files.get isDefinedAt "vcf"
        }
      }
    }

    "when calling GET to the " + path + " path with a Vault ID" - {
      "should reference the new files" in {
        Get(path + "/" + testId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> analysisDescribeRoute ~> check {
          status should equal(OK)
          val respAnalysis = responseAs[Analysis]
          respAnalysis.id should equal(testId)
          respAnalysis.files.get shouldNot be(empty)
          respAnalysis.files.get.getOrElse("bam", "error") should (be an (URL) and not be a (UUID))
          respAnalysis.files.get.getOrElse("bai", "error") should (be an (URL) and not be a (UUID))
          respAnalysis.files.get.getOrElse("vcf", "error") should (be an (URL) and not be a (UUID))
          respAnalysis.files.get.getOrElse("bam", "error") shouldNot include("ingest")
          respAnalysis.files.get.getOrElse("bai", "error") shouldNot include("ingest")
          respAnalysis.files.get.getOrElse("vcf", "error") shouldNot include("ingest")
         }
      }
    }

    "when calling GET to the " + path + " path with an invalid Vault ID" - {
      "should return a Not Found error" in {
        Get(path + "/" + "unknown-not-found-id") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(analysisDescribeRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/" + testId) ~> sealRoute(analysisDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/" + testId) ~> sealRoute(analysisDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }
  }

}

