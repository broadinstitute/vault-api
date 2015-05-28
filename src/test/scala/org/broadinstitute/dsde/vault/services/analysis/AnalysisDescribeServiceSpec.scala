package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model.{Analysis, AnalysisIngest, AnalysisIngestResponse, AnalysisUpdate}
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.scalatest.{DoNotDiscover, Suite}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import org.broadinstitute.dsde.vault.model.Properties._

@DoNotDiscover
class AnalysisDescribeServiceSpec extends VaultFreeSpec with AnalysisDescribeService with AnalysisUpdateService with AnalysisIngestService{

  def actorRefFactory = system

  var testId = "invalid_UUID"
  var testProperties: Map[String, String] = Map.empty 

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "AnalysisDescribeServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        "when calling POST to the Analysis Ingest path in order to set up" - {
          "should return as OK" in {
            val analysisIngest = new AnalysisIngest(
              input = List(),
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
              status should equal(OK)
              val respAnalysis = responseAs[AnalysisIngestResponse]
              testId = respAnalysis.id

              version match {
                case Some(x) if x > 1 =>
                  respAnalysis.properties shouldNot be(empty)
                  testProperties = respAnalysis.properties.get
                  testProperties.get(CreatedBy) shouldNot be(empty)
                  testProperties.get(CreatedDate) shouldNot be(empty)
                  testProperties.get(ModifiedBy) shouldBe empty
                  testProperties.get(ModifiedDate) shouldBe empty
                case _ =>
                  respAnalysis.properties shouldBe empty
              }
            }
          }
        }

        "when calling GET to the Analysis Describe path with a Vault ID" - {
          "should return that ID" in {
            Get(VaultConfig.Vault.analysisDescribePath(testId).versioned(version)) ~> addOpenAmCookie ~> analysisDescribeRoute ~> check {
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

              version match {
                case Some(x) if x > 1 =>
                  respAnalysis.properties shouldNot be(empty)
                  respAnalysis.properties.get should equal(testProperties)
                case _ =>
                  respAnalysis.properties shouldBe empty
              }

            }
          }
        }

        "when calling POST to the Analysis Update path in order to add completed files" - {
          "should return as OK" in {
            val analysisUpdate = new AnalysisUpdate(files = Map("vcf" -> "pathToVcf", "bai" -> "pathToBai", "bam" -> "pathToBam"))
            Post(VaultConfig.Vault.analysisUpdatePath(testId).versioned(version), analysisUpdate) ~> addOpenAmCookie ~> analysisUpdateRoute ~> check {
              status should equal(OK)
              val analysisResponse = responseAs[Analysis]
              val files = responseAs[Analysis].files
              analysisResponse.id should be(testId)
              files.get isDefinedAt "bam"
              files.get isDefinedAt "bai"
              files.get isDefinedAt "vcf"

              version match {
                case Some(x) if x > 1 =>
                  analysisResponse.properties shouldNot be(empty)
                  testProperties = analysisResponse.properties.get
                  testProperties.get(CreatedBy) shouldNot be(empty)
                  testProperties.get(CreatedDate) shouldNot be(empty)
                  testProperties.get(ModifiedBy) shouldNot be(empty)
                  testProperties.get(ModifiedDate) shouldNot be(empty)
                case _ =>
                  analysisResponse.properties shouldBe empty
              }
            }
          }
        }

        "when calling GET to the Analysis Describe path with a Vault ID" - {
          "should reference the new files" in {
            Get(VaultConfig.Vault.analysisDescribePath(testId).versioned(version)) ~> addOpenAmCookie ~> analysisDescribeRoute ~> check {
              status should equal(OK)
              val respAnalysis = responseAs[Analysis]
              respAnalysis.id should equal(testId)
              respAnalysis.files.get shouldNot be(empty)
              respAnalysis.files.get.getOrElse("bam", "error") should (be an (URL) and not be a(UUID))
              respAnalysis.files.get.getOrElse("bai", "error") should (be an (URL) and not be a(UUID))
              respAnalysis.files.get.getOrElse("vcf", "error") should (be an (URL) and not be a(UUID))
              respAnalysis.files.get.getOrElse("bam", "error") shouldNot include("ingest")
              respAnalysis.files.get.getOrElse("bai", "error") shouldNot include("ingest")
              respAnalysis.files.get.getOrElse("vcf", "error") shouldNot include("ingest")

              version match {
                case Some(x) if x > 1 =>
                  respAnalysis.properties shouldNot be(empty)
                  respAnalysis.properties.get should equal(testProperties)
                case _ =>
                  respAnalysis.properties shouldBe empty
              }
            }
          }
        }

        "when calling GET to the Analysis Describe path with an invalid Vault ID" - {
          "should return a Not Found error" in {
            Get(VaultConfig.Vault.analysisDescribePath("unknown-not-found-id").versioned(version)) ~> addOpenAmCookie ~> sealRoute(analysisDescribeRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling PUT to the Analysis Describe path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.analysisDescribePath(testId).versioned(version)) ~> sealRoute(analysisDescribeRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "when calling POST to the Analysis Describe path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Post(VaultConfig.Vault.analysisDescribePath(testId).versioned(version)) ~> sealRoute(analysisDescribeRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }
      }
    }
  }
}

