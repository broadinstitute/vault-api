package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.{UBamIngestResponse, UBamIngest}
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class UBamRedirectServiceSpec extends VaultFreeSpec with UBamRedirectService with UBamIngestService {

  override val routes = uBamRedirectRoute

  def actorRefFactory = system
  val path = "/ubams"
  val openAmResponse = getOpenAmToken.get
  var testingId = "invalid_UUID"
  var forceTestingId = "invalid_UUID"

  val files = Map(("bam", "/path/to/ingest/bam"), ("bai", "/path/to/ingest/bai"))
  val metadata = Map("testAttr" -> "testValue")

  "UBamRedirectServiceSpec" - {
    "while preparing the ubam test data" - {
      "should successfully store the data" in {
        val ubamIngest = new UBamIngest(files, metadata)
        Post(path, ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
          status should equal(OK)
          testingId = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when calling GET to the " + path + " path with a valid Vault ID and a valid file type" - {
      "should return a redirect url to the file" in {
        Get(path + "/" + testingId + "/bai") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamRedirectRoute ~> check {
          status should equal(TemporaryRedirect)
        }
      }
    }

    "when calling GET to the " + path + " path with a valid Vault ID and an invalid file type" - {
      "should return a Bad Request response" in {
        Get(path + "/" + testingId + "/invalid") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamRedirectRoute) ~> check {
          status should equal(BadRequest)
        }
      }
    }

    "when calling GET to the " + path + " path with an invalid Vault ID and a valid file type" - {
      "should return a a Not Found response" in {
        Get(path + "/12345-67890-12345/bai") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamRedirectRoute) ~> check {
          status should equal(NotFound)
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/" + testingId + "/bai") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamRedirectRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/" + testingId + "/bai") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(uBamRedirectRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "X-Force-Location API: while preparing the ubam test data" - {
      "should successfully store the data" in {
        val ubamIngest = new UBamIngest(files, metadata)
        Post(path, ubamIngest) ~> addHeader("X-Force-Location", "true") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
          status should equal(OK)
          forceTestingId = responseAs[UBamIngestResponse].id
          files.get("bam").get should equal("/path/to/ingest/bam")
          files.get("bai").get should equal("/path/to/ingest/bai")

        }
      }
    }

    "X-Force-Location API: when calling GET to the " + path + " path with a valid Vault ID and a valid file type" - {
      "should return a redirect url to the file" in {
        Get(path + "/" + forceTestingId + "/bai") ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamRedirectRoute ~> check {
          status should equal(TemporaryRedirect)
          // test that the redirect properly handles the file path we passed in, which includes slashes
          // this test will fail if Google changes how they sign urls
          header("Location") shouldNot be(None)
          header("Location").get.value should include("/path/to/ingest/bai?GoogleAccessId=")
        }
      }
    }

  }

}
