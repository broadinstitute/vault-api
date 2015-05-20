package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.model.{AnalysisIngest, AnalysisIngestResponse, UBamIngest, UBamIngestResponse}
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.scalatest.BeforeAndAfterAll
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpEntity, MediaTypes}

class AnalysisIngestServiceSpec extends VaultFreeSpec with AnalysisIngestService with UBamIngestService with BeforeAndAfterAll {

  import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol.{impAnalysisIngest, impAnalysisIngestResponse}
  import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol.{impUBamIngest, impUBamIngestResponse}
  import spray.httpx.SprayJsonSupport._
  def actorRefFactory = system

  var createdUBams: Seq[String] = Seq("bad", "ids")

  override def beforeAll(): Unit = {

    // test ubam for ingest
    val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
    val metadata = Map("testAttr" -> "testValue")
    val ubamIngest = new UBamIngest(files, metadata)

    // create a few ubams
    createdUBams = (for (x <- 1 to 3) yield

      Post(VaultConfig.Vault.ubamIngestPath, ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
        status should equal(OK)
        responseAs[UBamIngestResponse].id
      }

      ).sorted.toSeq.asInstanceOf[Seq[String]]

  }

  // TODO: clean up the ubams once that is possible
  // override def afterAll(): Unit = {}

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "AnalysisIngestServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        "when calling POST to the Analysis Ingest path with empty input and valid metadata" - {
          "should return an ID" in {
            val analysisIngest = new AnalysisIngest(
              input = List(),
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
              status should equal(OK)
              // test response as raw string
              entity.toString should include("id")
              // test response as json, unmarshaled into an object
              val respAnalysis = responseAs[AnalysisIngestResponse]
              val createdId = java.util.UUID.fromString(respAnalysis.id)
              entity.toString should include(createdId.toString)
            }
          }
        }

        "when calling POST to the Analysis Ingest path with a single valid input and valid metadata" - {
          "should return an ID" in {
            val analysisIngest = new AnalysisIngest(
              input = List(createdUBams.head),
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
              status should equal(OK)
              // test response as raw string
              entity.toString should include("id")
              // test response as json, unmarshaled into an object
              val respAnalysis = responseAs[AnalysisIngestResponse]
              val createdId = java.util.UUID.fromString(respAnalysis.id)
              entity.toString should include(createdId.toString)
            }
          }
        }

        "when calling POST to the Analysis Ingest path with a single valid input, single invalid input, and valid metadata" - {
          "should return  a Not Found error" in {
            val analysisIngest = new AnalysisIngest(
              input = List(createdUBams.head) :+ "intentionallyBadForeignKey",
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> sealRoute(analysisIngestRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling POST to the Analysis Ingest path with multiple valid input and valid metadata" - {
          "should return an ID" in {
            val analysisIngest = new AnalysisIngest(
              input = createdUBams.toList,
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> analysisIngestRoute ~> check {
              status should equal(OK)
              // test response as raw string
              entity.toString should include("id")
              // test response as json, unmarshaled into an object
              val respAnalysis = responseAs[AnalysisIngestResponse]
              val createdId = java.util.UUID.fromString(respAnalysis.id)
              entity.toString should include(createdId.toString)
            }
          }
        }


        "when calling POST to the Analysis Ingest path with multiple valid input, single invalid input, and valid metadata" - {
          "should return a Not Found error" in {
            val analysisIngest = new AnalysisIngest(
              input = createdUBams.toList :+ "intentionallyBadForeignKey",
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), analysisIngest) ~> addOpenAmCookie ~> sealRoute(analysisIngestRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling POST to the Analysis Ingest path with an invalid object" - {
          "should return a Bad Request error" in {
            val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
            Post(VaultConfig.Vault.analysisIngestPath.versioned(version), malformedEntity) ~> addOpenAmCookie ~> sealRoute(analysisIngestRoute) ~> check {
              status should equal(BadRequest)
            }
          }
        }

        "when calling PUT to the Analysis Ingest path" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.analysisIngestPath.versioned(version)) ~> sealRoute(analysisIngestRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: POST")
            }
          }
        }

        "when calling GET to the Analysis Ingest path" - {
          "should return a MethodNotAllowed error" in {
            Get(VaultConfig.Vault.analysisIngestPath.versioned(version)) ~> sealRoute(analysisIngestRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: POST")
            }
          }
        }
      }
    }
  }

}

