package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{UBamIngestResponse, UBamIngest, UBam}
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class DescribeServiceSpec extends VaultFreeSpec with UBamDescribeService with UBamIngestService {

  override val routes = uBamDescribeRoute

  def actorRefFactory = system
  val path = "/ubams"
  val openAmResponse = getOpenAmToken.get
  var testingId = "invalid_UUID"

  val files = Map(("bam", "/path/to/ingest/bam"), ("bai", "/path/to/ingest/bai"))
  val metadata = Map("testAttr" -> "testValue")

  "DescribeuBAMService" - {
    "while preparing the ubam test data" - {
      "should successfully store the data" in {
        val ubamIngest = new UBamIngest(files, metadata)
        Post(path, ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
          status should equal(OK)
          testingId = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {
        Get(path + "/" + testingId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamDescribeRoute ~> check {
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

    "when calling GET to the " + path + " path with an unknown Vault ID" - {
      "should return a 404 not found error" in {
        Get(path + "/12345-67890-12345") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamDescribeRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/" + testingId) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/arbitrary_id") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamDescribeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

  }

}

