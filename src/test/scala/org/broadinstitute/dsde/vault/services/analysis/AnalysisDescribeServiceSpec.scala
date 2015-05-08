package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model.{AnalysisIngestResponse, AnalysisIngest, AnalysisUpdate, Analysis}
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

class AnalysisDescribeServiceSpec extends VaultFreeSpec with AnalysisDescribeService with AnalysisUpdateService with AnalysisIngestService {

  def actorRefFactory = system

  var testId = "invalid_UUID"

  "AnalysisDescribeServiceSpec" - {
    "when calling POST to the Analysis Ingest path in order to set up" - {
      "should return as OK" in {
        val analysisIngest = new AnalysisIngest(
          input = List(),
          metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
        )
        Post(VaultConfig.Vault.analysisIngestPath, analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
          status should equal(OK)
          val respAnalysis = responseAs[AnalysisIngestResponse]
          testId = respAnalysis.id
        }
      }
    }

    "when calling GET to the Analysis Describe path with a Vault ID" - {
      "should return that ID" in {
        Get(VaultConfig.Vault.analysisDescribePath(testId)) ~> addOpenAmCookie ~> analysisDescribeRoute ~> check {
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
          respAnalysis.metadata should equal(Map("testAttr" -> "testValue", "randomData" -> "7"))
        }
      }
    }

    "when calling POST to the Analysis Update path in order to add completed files" - {
      "should return as OK" in {
        val analysisUpdate = new AnalysisUpdate(files = Map("vcf" -> "vault/test/test.vcf", "bai" -> "vault/test/test.bai", "bam" -> "vault/test/test.bam"))
        Post(VaultConfig.Vault.analysisUpdatePath(testId), analysisUpdate) ~> addOpenAmCookie ~> analysisUpdateRoute ~> check {
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

    "when calling GET to the Analysis Describe path with a Vault ID" - {
      "should reference the new files" in {
        Get(VaultConfig.Vault.analysisDescribePath(testId)) ~> addOpenAmCookie ~> analysisDescribeRoute ~> check {
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

    "when calling GET to the Analysis Describe path with an invalid Vault ID" - {
      "should return a Not Found error" in {
        Get(VaultConfig.Vault.analysisDescribePath("unknown-not-found-id")) ~> addOpenAmCookie ~> sealRoute(analysisDescribeRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the Analysis Describe path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(VaultConfig.Vault.analysisDescribePath(testId)) ~> sealRoute(analysisDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the Analysis Describe path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(VaultConfig.Vault.analysisDescribePath(testId)) ~> sealRoute(analysisDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }
  }
}

