package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCookie, HttpEntity, MediaTypes}
import spray.httpx.SprayJsonSupport._

class AnalysisUpdateServiceSpec extends VaultFreeSpec with AnalysisUpdateService with UBamIngestService {

  override val routes = analysisUpdateRoute

  def actorRefFactory = system

  val openAmResponse = getOpenAmToken.get

  var testDataGuid: String = "invalid-id"
  val analysisUpdate = new AnalysisUpdate(files = Map("vcf" -> "path/to/ingest/vcf", "bai" -> "path/to/ingest/bai", "bam" -> "path/to/ingest/bam"))
  val path = s"/analyses/%s/outputs"

  "AnalysisUpdateServiceSpec" - {

    "while preparing the ubam test data" - {
      "should successfully store the data" in {
        val files = Map(("bam", "/path/to/ingest/bam"))
        val metadata = Map("testAttr" -> "testValue")
        val ubamIngest = new UBamIngest(files, metadata)
        Post("/ubams", ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
          status should equal(OK)
          testDataGuid = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when calling POST to the " + path + " path with a valid Vault ID and valid body" - {
      "should return as OK" in {
        Post(path.format(testDataGuid), analysisUpdate) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> analysisUpdateRoute ~> check {
          status should equal(OK)
          val analysisResponse = responseAs[Analysis]
          val files = responseAs[Analysis].files
          analysisResponse.id should be (testDataGuid)
          files.get isDefinedAt "bam"
          files.get isDefinedAt "bai"
          files.get isDefinedAt "vcf"
        }
      }
    }

    "when calling POST to the " + path + " path with a Analysis object and 'X-Force-Location' header" - {
      "should return a valid response with paths as part of the file path names" in {
        Post(path.format(testDataGuid), analysisUpdate) ~> addHeader("X-Force-Location", "true") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> analysisUpdateRoute ~> check {
          status should equal(OK)
          val files = responseAs[Analysis].files
          files.get("bam") should equal("path/to/ingest/bam")
          files.get("bai") should equal("path/to/ingest/bai")
          files.get("vcf") should equal("path/to/ingest/vcf")
        }
      }
    }

    "when calling POST to the " + path + " path with an invalid Vault ID and valid body" - {
      "should return a Not Found error" in {
        Post(path.format("unknown-not-found-id"), analysisUpdate) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(analysisUpdateRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling POST to the " + path + " path with an invalid body" - {
      "should return a Bad Request error" in {
        val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
        Post(path.format(testDataGuid), malformedEntity) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(analysisUpdateRoute) ~> check {
          status should equal(BadRequest)
        }
      }
    }

    "when calling PUT to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Put(path.format(testDataGuid)) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(analysisUpdateRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }

    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Get(path.format(testDataGuid)) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(analysisUpdateRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }
  }

}

