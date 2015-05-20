package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamIngest, UBamIngestResponse}
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpEntity, MediaTypes}
import spray.httpx.SprayJsonSupport._

class UBamIngestServiceSpec extends VaultFreeSpec with UBamIngestService {

  def actorRefFactory = system

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "UBamIngestServiceSpec" - {

    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        val ubamIngest = new UBamIngest(
          files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai")),
          metadata = Map(("testAttr", "testValue"), ("randomData", "7"))
        )

        "when calling POST to the UBam Ingest path with a UBamIngest object" - {
          "should return a valid response" in {
            // As designed, the API returns an object that only contains an id and files, but not the supplied metadata
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              responseAs[String] should include("bam")
              responseAs[String] should include("bai")
              responseAs[String] shouldNot include("randomData")
              responseAs[String] shouldNot include("testUser")
            }
          }
        }

        "when calling POST to the UBam Ingest path with a UBamIngest object and 'X-Force-Location' header" - {
          "should return a valid response with the provided file paths" in {
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addHeader("X-Force-Location", "true") ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              val files = responseAs[UBamIngestResponse].files
              files.get("bam").get should equal("vault/test/test.bam")
              files.get("bai").get should equal("vault/test/test.bai")
            }
          }
        }

        "when calling GET to the UBam Ingest path" - {
          "should return a MethodNotAllowed error" in {
            Get(VaultConfig.Vault.ubamIngestPath.versioned(version)) ~> sealRoute(uBamIngestRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: POST")
            }
          }
        }

        "when calling PUT to the UBam Ingest path" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.ubamIngestPath.versioned(version)) ~> sealRoute(uBamIngestRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: POST")
            }
          }
        }

        "when calling POST to the UBam Ingest path with a malformed UBamIngest object" - {
          "should return an invalid response" in {
            val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), malformedEntity) ~> addOpenAmCookie ~> sealRoute(uBamIngestRoute) ~> check {
              status should equal(BadRequest)
            }
          }
        }

      }

    }

  }

}
