package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.Analysis
import org.broadinstitute.dsde.vault.services.analysis.DescribeJsonProtocol._
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._

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
          // test that we can unmarshal into an object
          entity.as[Analysis].isRight shouldBe true
          // test the unmarshaled object's contents
          val respAnalysis = responseAs[Analysis]
          respAnalysis.id should equal(testId)
          // TODO: once the service is implemented, figure out how to test these for real
          respAnalysis.input should equal(List("123", "456", "789"))
          respAnalysis.files should equal(Map(
            "vcf" -> "http://vault/redirect/url/to/get/file",
            "bam" -> "http://vault/redirect/url/to/get/file",
            "bai" -> "http://vault/redirect/url/to/get/file",
            "adapter_metrics" -> "http://vault/redirect/url/to/get/file"
          ))
          respAnalysis.metadata should equal(Map(
            "analysisId" -> "CES_id",
            "key1" -> "value1",
            "key2" -> "value2",
            "key3" -> "value3",
            "key4" -> "value4",
            "key5" -> "value5"
          ))
        }
      }
    }

    // TODO: this test will fail until the service is properly implemented
    "DescribeAnalysisService" - {
      "when calling GET to the " + path + " path with an invalid Vault ID" - {
        "should return a Not Found error" ignore {
          Get(path + "/" + "unknown-not-found-id") ~> describeRoute ~> check {
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

