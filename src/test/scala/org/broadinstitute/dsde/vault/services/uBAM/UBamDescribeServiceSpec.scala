package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model.{UBamIngestResponse, UBamIngest, UBam}
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class UBamDescribeServiceSpec extends VaultFreeSpec with UBamDescribeService with UBamIngestService {

  def actorRefFactory = system
  var testingId = "invalid_UUID"

  val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
  val metadata = Map("testAttr" -> "testValue")

  "UBamDescribeServiceSpec" - {
    "while preparing the ubam test data" - {
      "should successfully store the data using the UBam Ingest path" in {
        val ubamIngest = new UBamIngest(files, metadata)
        Post(VaultConfig.Vault.ubamIngestPath, ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
          status should equal(OK)
          testingId = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when calling GET to the UBam Describe path with a Vault ID" - {
      "should return that ID" in {
        Get(VaultConfig.Vault.ubamDescribePath(testingId)) ~> addOpenAmCookie ~> uBamDescribeRoute ~> check {
          status should equal(OK)
          val response = responseAs[UBam]
          response.id should be(testingId)
          response.metadata should equal(Map("testAttr" -> "testValue"))
          response.files.getOrElse("bam", "error") should (be an (URL) and not be a (UUID))
          response.files.getOrElse("bai", "error") should (be an (URL) and not be a (UUID))
          response.files.getOrElse("bam", "error") shouldNot include("ingest")
          response.files.getOrElse("bai", "error") shouldNot include("ingest")
        }
      }
    }

    "when calling GET to the UBam Describe path with an unknown Vault ID" - {
      "should return a 404 not found error" in {
        Get(VaultConfig.Vault.ubamDescribePath("12345-67890-12345")) ~> addOpenAmCookie ~> sealRoute(uBamDescribeRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the UBam Describe path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(VaultConfig.Vault.ubamDescribePath(testingId)) ~> addOpenAmCookie ~> sealRoute(uBamDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the UBam Describe path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(VaultConfig.Vault.ubamDescribePath("arbitrary_id")) ~> addOpenAmCookie ~> sealRoute(uBamDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

  }

}

