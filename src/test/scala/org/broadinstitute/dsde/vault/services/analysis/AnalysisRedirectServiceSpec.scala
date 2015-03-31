package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{Analysis, AnalysisUpdate, AnalysisIngest}
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class AnalysisRedirectServiceSpec extends VaultFreeSpec with RedirectService with IngestAnalysisService with UpdateService {

  override val routes = redirectRoute

  def actorRefFactory = system
  val path = "/analyses"
  val openAmResponse = getOpenAmToken.get
  var testingId = "invalid_UUID"
  var forceTestingId = "invalid_UUID"

  val inputs = List()
  val metadata = Map("ownerId" -> "user")
  val files = Map(("bam", "/path/to/outputs/bam"), ("vcf", "/path/to/outputs/vcf"))

  "Analysis Redirect Service" - {
    "while preparing the analysis test data" - {
      "should successfully store the data" in {
        val analysisIngest = new AnalysisIngest(inputs, metadata)
        Post(path, analysisIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> ingestRoute ~> check {
          status should equal(OK)
          testingId = responseAs[Analysis].id
          responseAs[Analysis].metadata.getOrElse("ownerId", "get failed") should equal("user")
        }
      }
      "should successfully update the data" in {
        val analysisUpdate = new AnalysisUpdate(files)
        Post(path + "/" + testingId + "/outputs", analysisUpdate) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> updateRoute ~> check {
          status should equal(OK)
          val analysisResponse = responseAs[Analysis]
          val files = responseAs[Analysis].files
          analysisResponse.id should be (testingId)
          files.get isDefinedAt "bam"
          files.get isDefinedAt "bai"
          files.get isDefinedAt "vcf"
        }
      }
    }

    "when calling GET to the " + path + " path with a valid Vault ID and a valid file type" - {
      "should return a redirect url to the file" in {
        Get(path + "/" + testingId + "/bam") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(redirectRoute) ~> check {
          status should equal(TemporaryRedirect)
        }
      }
    }

    "when calling GET to the " + path + " path with a valid Vault ID and an invalid file type" - {
      "should return a Bad Request response" in {
        Get(path + "/" + testingId + "/invalid") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(redirectRoute) ~> check {
          status should equal(BadRequest)
        }
      }
    }

    "when calling GET to the " + path + " path with an invalid Vault ID and a valid file type" - {
      "should return a a Not Found response" in {
        Get(path + "/12345-67890-12345/bam") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(redirectRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/" + testingId + "/bam") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(redirectRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/" + testingId + "/bam") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(redirectRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "X-Force-Location API: while preparing the analysis test data" - {
      "should successfully store the data" in {
        val analysisIngest = new AnalysisIngest(inputs, metadata)
        Post(path, analysisIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> ingestRoute ~> check {
          status should equal(OK)
          forceTestingId = responseAs[Analysis].id
          responseAs[Analysis].metadata.getOrElse("ownerId", "get failed") should equal("user")
        }
      }
      "should successfully update the data" in {
        val analysisUpdate = new AnalysisUpdate(files)
        Post(path + "/" + forceTestingId + "/outputs", analysisUpdate) ~> addHeader("X-Force-Location", "true") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> updateRoute ~> check {
          status should equal(OK)
          val analysisResponse = responseAs[Analysis]
          val files = responseAs[Analysis].files
          analysisResponse.id should be (forceTestingId)
          files.get("bam") should equal("/path/to/outputs/bam")
          files.get("vcf") should equal("/path/to/outputs/vcf")
        }
      }
    }

    "X-Force-Location API: when calling GET to the " + path + " path with a valid Vault ID and a valid file type" - {
      "should return a redirect url to the file" in {
        Get(path + "/" + forceTestingId + "/bam") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(redirectRoute) ~> check {
          status should equal(TemporaryRedirect)
        }
      }
    }


  }

}
