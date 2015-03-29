package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.services.uBAM.IngestService
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCookie, HttpEntity, MediaTypes}
import spray.httpx.SprayJsonSupport._

class AnalysisUpdateServiceSpec extends VaultFreeSpec with UpdateService with IngestService {

  override val routes = updateRoute

  def actorRefFactory = system

  val openAmResponse = getOpenAmToken.get

  var testDataGuid: String = "invalid-id"
  val analysisUpdate = new AnalysisUpdate(files = Map("vcf" -> "path/to/ingest/vcf", "bai" -> "path/to/ingest/bai", "bam" -> "path/to/ingest/bam"))
  val path = s"/analyses/%s/outputs"

  "AnalysisUpdateService" - {

    "while preparing the ubam test data" - {
      "should successfully store the data" in {
        val files = Map(("bam", "/path/to/ingest/bam"))
        val metadata = Map("ownerId" -> "user")
        val ubamIngest = new UBamIngest(files, metadata)
        Post("/ubams", ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> ingestRoute ~> check {
          status should equal(OK)
          testDataGuid = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when calling POST to the " + path + " path with a valid Vault ID and valid body" - {
      "should return as OK" in {
        Post(path.format(testDataGuid), analysisUpdate) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> updateRoute ~> check {
          status should equal(OK)
          val analysisResponse = responseAs[Analysis]
          analysisResponse.id should be (testDataGuid)
          analysisResponse.files.get isDefinedAt "bam"
          analysisResponse.files.get isDefinedAt "bai"
          analysisResponse.files.get isDefinedAt "vcf"
        }
      }
    }

    // TODO: Update this test once creation no longer returns pre-signed urls when forceLocation is true
    "when calling POST to the " + path + " path with a Analysis object and Force-Location header" - {
      "should return a valid response with paths as part of the file path names" in {
        Post(path.format(testDataGuid), analysisUpdate) ~> addHeader("X-Force-Location", "true") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> updateRoute ~> check {
          status should equal(OK)
          val analysisResponse = responseAs[Analysis]
          analysisResponse.files.get("bam") should include("path/to/ingest/bam")
          analysisResponse.files.get("bai") should include("path/to/ingest/bai")
          analysisResponse.files.get("vcf") should include("path/to/ingest/vcf")
        }
      }
    }

    "when calling POST to the " + path + " path with an invalid Vault ID and valid body" - {
      "should return a Not Found error" in {
        Post(path.format("unknown-not-found-id"), analysisUpdate) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(updateRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling POST to the " + path + " path with an invalid body" - {
      "should return a Bad Request error" in {
        val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
        Post(path.format(testDataGuid), malformedEntity) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(updateRoute) ~> check {
          status should equal(BadRequest)
        }
      }
    }

    "when calling PUT to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Put(path.format(testDataGuid)) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(updateRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }

    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Get(path.format(testDataGuid)) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(updateRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }
  }

}

