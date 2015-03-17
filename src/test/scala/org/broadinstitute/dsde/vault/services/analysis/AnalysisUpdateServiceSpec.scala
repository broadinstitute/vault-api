package org.broadinstitute.dsde.vault.services.analysis

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.AnalysisUpdate
import org.broadinstitute.dsde.vault.services.analysis.UpdateJsonProtocol._
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpEntity, MediaTypes, StatusCodes}
import spray.httpx.SprayJsonSupport._

class AnalysisUpdateServiceSpec extends VaultFreeSpec with UpdateService {

  def actorRefFactory = system

  val testId = "123-456-789"
  val path = s"/analyses/%s/outputs"

  "AnalysisUpdateService" - {
    "when calling POST to the " + path + " path with a valid Vault ID and valid body" - {
      "should return as Accepted" in {
        // TODO: figure out how to pick a valid id
        val validId = "123-456-789"
        val analysisUpdate = new AnalysisUpdate(
          files = Map("vcf" -> "vcfValue", "bam" -> "bamValue")
        )
        Post(path.format(validId), analysisUpdate) ~> updateRoute ~> check {
          status should equal(StatusCodes.Accepted)
        }
      }
    }

    // TODO: this test will fail until the service is properly implemented
    "AnalysisUpdateService" - {
      "when calling POST to the " + path + " path with an invalid Vault ID and valid body" - {
        "should return that ID" ignore {
          val analysisUpdate = new AnalysisUpdate(
            files = Map("vcf" -> "vcfValue", "bam" -> "bamValue")
          )
          Post(path.format("unknown-not-found-id"), analysisUpdate) ~> updateRoute ~> check {
            status should equal(StatusCodes.NotFound)
          }
        }
      }
    }

    // TODO: this test will fail until the service is properly implemented
    "when calling POST to the " + path + " path with an invalid body" - {
      "should return a Bad Request error" ignore {
        val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
        Post(path.format(testId), malformedEntity) ~> updateRoute ~> check {
          status should equal(BadRequest)
        }
      }
    }

    "when calling PUT to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Put(path.format(testId)) ~> sealRoute(updateRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }

    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Get(path.format(testId)) ~> sealRoute(updateRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }
  }

}

