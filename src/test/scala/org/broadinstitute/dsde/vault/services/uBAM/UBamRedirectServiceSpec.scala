package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamIngest, UBamIngestResponse}
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class UBamRedirectServiceSpec extends VaultFreeSpec with UBamRedirectService with UBamIngestService {

  def actorRefFactory = system

  var testingId = "invalid_UUID"
  var forceTestingId = "invalid_UUID"

  val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
  val metadata = Map("testAttr" -> "testValue")

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "UBamRedirectServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        "while preparing the ubam test data" - {
          "should successfully store the data using the UBam Ingest path" in {
            val ubamIngest = new UBamIngest(files, metadata)
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              testingId = responseAs[UBamIngestResponse].id
            }
          }
        }

        "when calling GET to the UBam Redirect path with a valid Vault ID and a valid file type" - {
          "should return a redirect url to the file" in {
            Get(VaultConfig.Vault.ubamRedirectPath(testingId, "bai").versioned(version)) ~> addOpenAmCookie ~> uBamRedirectRoute ~> check {
              status should equal(TemporaryRedirect)
            }
          }
        }

        "when calling GET to the UBam Redirect path with a valid Vault ID and an invalid file type" - {
          "should return a Bad Request response" in {
            Get(VaultConfig.Vault.ubamRedirectPath(testingId, "invalid").versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamRedirectRoute) ~> check {
              status should equal(BadRequest)
            }
          }
        }

        "when calling GET to the UBam Redirect path with an invalid Vault ID and a valid file type" - {
          "should return a a Not Found response" in {
            Get(VaultConfig.Vault.ubamRedirectPath("12345-67890-12345", "bai").versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamRedirectRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling PUT to the UBam Redirect path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.ubamRedirectPath(testingId, "bai").versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamRedirectRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "when calling POST to the UBam Redirect path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Post(VaultConfig.Vault.ubamRedirectPath(testingId, "bai").versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamRedirectRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "X-Force-Location API: while preparing the ubam test data" - {
          "should successfully store the data using the UBam Ingest path" in {
            val ubamIngest = new UBamIngest(files, metadata)
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addHeader("X-Force-Location", "true") ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              forceTestingId = responseAs[UBamIngestResponse].id
              files.get("bam").get should equal("vault/test/test.bam")
              files.get("bai").get should equal("vault/test/test.bai")

            }
          }
        }

        "X-Force-Location API: when calling GET to the UBam Redirect path with a valid Vault ID and a valid file type" - {
          "should return a redirect url to the file" in {
            Get(VaultConfig.Vault.ubamRedirectPath(forceTestingId, "bai").versioned(version)) ~> addOpenAmCookie ~> uBamRedirectRoute ~> check {
              status should equal(TemporaryRedirect)
              // test that the redirect properly handles the file path we passed in, which includes slashes
              // this test will fail if Google changes how they sign urls
              header("Location") shouldNot be(None)
              header("Location").get.value should include("vault/test/test.bai?GoogleAccessId=")
            }
          }
        }

      }

    }

  }

}
