package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model.{Analysis, AnalysisIngest, AnalysisUpdate}
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.scalatest.{DoNotDiscover, Suite}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import org.broadinstitute.dsde.vault.model.Properties._

@DoNotDiscover
class AnalysisRedirectServiceSpec extends VaultFreeSpec with AnalysisRedirectService with AnalysisIngestService with AnalysisUpdateService{

  def actorRefFactory = system
  var testingId = "invalid_UUID"
  var forceTestingId = "invalid_UUID"
  var testProperties: Map[String, String] = Map.empty

  val inputs = List()
  val metadata = Map("testAttr" -> "testValue")
  val files = Map(("bam", "vault/test/test.bam"), ("vcf", "vault/test/test.vcf"))

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "AnalysisRedirectServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        "while preparing the analysis test data" - {
          "should successfully store the data using the Analysis Ingest path" in {
            val analysisIngest = new AnalysisIngest(inputs, metadata)
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
              status should equal(OK)
              val response = responseAs[Analysis]
              testingId = responseAs[Analysis].id
              response.metadata.getOrElse("testAttr", "get failed") should equal("testValue")

              version match {
                case Some(x) if x > 1 =>
                  response.properties shouldNot be(empty)
                  testProperties = response.properties.get
                  testProperties.get(CreatedBy) shouldNot be(empty)
                  testProperties.get(CreatedDate) shouldNot be(empty)
                  testProperties.get(ModifiedBy) should be(empty)
                  testProperties.get(ModifiedDate) should be(empty)
                case _ =>
                  response.properties shouldBe empty
              }
            }
          }
          "should successfully update the data using the Analysis Update path" in {
            val analysisUpdate = new AnalysisUpdate(files)
            Post(VaultConfig.Vault.analysisUpdatePath(testingId).versioned(version), analysisUpdate) ~> addOpenAmCookie ~> analysisUpdateRoute ~> check {
              status should equal(OK)
              val analysisResponse = responseAs[Analysis]
              val files = responseAs[Analysis].files
              analysisResponse.id should be(testingId)
              files.get isDefinedAt "bam"
              files.get isDefinedAt "bai"
              files.get isDefinedAt "vcf"

              version match {
                case Some(x) if x > 1 =>
                  analysisResponse.properties shouldNot be(empty)
                  val newProperties = analysisResponse.properties.get
                  newProperties.get(CreatedBy) should equal(testProperties.get(CreatedBy))
                  newProperties.get(CreatedDate) should equal(testProperties.get(CreatedDate))
                  newProperties.get(ModifiedBy) shouldNot be(empty)
                  newProperties.get(ModifiedDate) shouldNot be(empty)
                case _ =>
                  analysisResponse.properties shouldBe empty
              }
            }
          }
        }

        "when calling GET to the Analysis Redirect path with a valid Vault ID and a valid file type" - {
            "should return a redirect url to the file" in {
            Get(VaultConfig.Vault.analysisRedirectPath(testingId, "bam").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisRedirectRoute) ~> check {
              status should equal(TemporaryRedirect)
            }
          }
        }

        "when calling GET to the Analysis Redirect path with a valid Vault ID and an invalid file type" - {
          "should return a Bad Request response" in {
            Get(VaultConfig.Vault.analysisRedirectPath(testingId, "invalid").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisRedirectRoute) ~> check {
              status should equal(BadRequest)
            }
          }
        }

        "when calling GET to the Analysis Redirect path with an invalid Vault ID and a valid file type" - {
          "should return a a Not Found response" in {
            Get(VaultConfig.Vault.analysisRedirectPath("12345-67890-12345", "bam").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisRedirectRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling PUT to the Analysis Redirect path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.analysisRedirectPath(testingId, "bam").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisRedirectRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "when calling POST to the Analysis Redirect path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Post(VaultConfig.Vault.analysisRedirectPath(testingId, "bam").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisRedirectRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "X-Force-Location API: while preparing the analysis test data" - {
          "should successfully store the data" in {
            val analysisIngest = new AnalysisIngest(inputs, metadata)
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
              status should equal(OK)
              forceTestingId = responseAs[Analysis].id
              responseAs[Analysis].metadata.getOrElse("testAttr", "get failed") should equal("testValue")
            }
          }
          "should successfully update the data" in {
            val analysisUpdate = new AnalysisUpdate(files)
            Post(VaultConfig.Vault.analysisUpdatePath(forceTestingId).versioned(version), analysisUpdate) ~> addHeader("X-Force-Location", "true") ~> addOpenAmCookie ~> analysisUpdateRoute ~> check {
              status should equal(OK)
              val analysisResponse = responseAs[Analysis]
              val files = responseAs[Analysis].files
              analysisResponse.id should be(forceTestingId)
              files.get("bam") should equal("vault/test/test.bam")
              files.get("vcf") should equal("vault/test/test.vcf")
            }
          }
        }

        "X-Force-Location API: when calling GET to the Analysis Redirect path with a valid Vault ID and a valid file type" - {
          "should return a redirect url to the file" in {
            Get(VaultConfig.Vault.analysisRedirectPath(forceTestingId, "bam").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisRedirectRoute) ~> check {
              status should equal(TemporaryRedirect)
              // test that the redirect properly handles the file path we passed in, which includes slashes
              // this test will fail if Google changes how they sign urls
              header("Location") shouldNot be(None)
              header("Location").get.value should include("vault/test/test.bam?GoogleAccessId=")
            }
          }
        }

      }
    }
  }

}
