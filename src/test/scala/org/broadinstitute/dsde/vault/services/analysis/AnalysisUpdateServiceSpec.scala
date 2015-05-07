package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCookie, HttpEntity, MediaTypes}
import spray.httpx.SprayJsonSupport._

class AnalysisUpdateServiceSpec extends VaultFreeSpec with AnalysisUpdateService with UBamIngestService {

  def actorRefFactory = system

  var testDataGuid: String = "invalid-id"
  val analysisUpdate = new AnalysisUpdate(files = Map("vcf" -> "vault/test/test.vcf", "bai" -> "vault/test/test.bai", "bam" -> "vault/test/test.bam"))

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "AnalysisUpdateServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        "while preparing the ubam test data" - {
          "should successfully store the data using the UBam Ingest path" in {
            val files = Map(("bam", "vault/test/test.bam"))
            val metadata = Map("testAttr" -> "testValue")
            val ubamIngest = new UBamIngest(files, metadata)
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              testDataGuid = responseAs[UBamIngestResponse].id
            }
          }
        }

        "when calling POST to the Analysis Update path with a valid Vault ID and valid body" - {
          "should return as OK" in {
            Post(VaultConfig.Vault.analysisUpdatePath(testDataGuid).versioned(version), analysisUpdate) ~> addOpenAmCookie ~> analysisUpdateRoute ~> check {
              status should equal(OK)
              val analysisResponse = responseAs[Analysis]
              val files = responseAs[Analysis].files
              analysisResponse.id should be(testDataGuid)
              files.get isDefinedAt "bam"
              files.get isDefinedAt "bai"
              files.get isDefinedAt "vcf"
            }
          }
        }

        "when calling POST to the Analysis Update path with a Analysis object and 'X-Force-Location' header" - {
          "should return a valid response with paths as part of the file path names" in {
            Post(VaultConfig.Vault.analysisUpdatePath(testDataGuid).versioned(version), analysisUpdate) ~> addHeader("X-Force-Location", "true") ~> addOpenAmCookie ~> analysisUpdateRoute ~> check {
              status should equal(OK)
              val files = responseAs[Analysis].files
              files.get("bam") should equal("vault/test/test.bam")
              files.get("bai") should equal("vault/test/test.bai")
              files.get("vcf") should equal("vault/test/test.vcf")
            }
          }
        }

        "when calling POST to the Analysis Update path with an invalid Vault ID and valid body" - {
          "should return a Not Found error" in {
            Post(VaultConfig.Vault.analysisUpdatePath("unknown-not-found-id").versioned(version), analysisUpdate) ~> addOpenAmCookie ~> sealRoute(analysisUpdateRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling POST to the Analysis Update path with an invalid body" - {
          "should return a Bad Request error" in {
            val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
            Post(VaultConfig.Vault.analysisUpdatePath(testDataGuid).versioned(version), malformedEntity) ~> addOpenAmCookie ~> sealRoute(analysisUpdateRoute) ~> check {
              status should equal(BadRequest)
            }
          }
        }

        "when calling PUT to the Analysis Update path" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.analysisUpdatePath(testDataGuid).versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisUpdateRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: POST")
            }
          }
        }

        "when calling GET to the Analysis Update path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Get(VaultConfig.Vault.analysisUpdatePath(testDataGuid).versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisUpdateRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: POST")
            }
          }
        }
      }
    }

  }
}
