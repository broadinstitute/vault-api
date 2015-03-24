package org.broadinstitute.dsde.vault.services.uBAM

import java.util.concurrent.TimeUnit

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.uBAMIngest
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.http.{ContentType, HttpCookie, HttpEntity, MediaTypes}

import scala.concurrent.duration.FiniteDuration

class IngestServiceSpec extends VaultFreeSpec with IngestService {

  def actorRefFactory = system
  val path = "/ubams"
  val openAmResponse = getOpenAmToken.get

  "IngestServiceSpec" - {

    "when calling POST to the " + path + " path with a uBAMIngest object" - {
      "should return a valid response" in {
        // As designed, the API returns an object that only contains an id and files, but not the supplied metadata
        val ubamIngest = new uBAMIngest(
          files = Map(("bam", "/path/to/ingest/bam"),("bai", "/path/to/ingest/bai")),
          metadata = Map(("ownerId", "testUser"),("randomData", "7"))
        )
        import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
        import spray.httpx.SprayJsonSupport._
        Post(path, ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> ingestRoute ~> check {
          status should equal(OK)
          responseAs[String] should include("bam")
          responseAs[String] should include("bai")
          responseAs[String] shouldNot include("randomData")
          responseAs[String] shouldNot include("testUser")
        }
      }
    }

    "when calling GET to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Get(path) ~> sealRoute(ingestRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }

    "when calling PUT to the " + path + " path" - {
      "should return a MethodNotAllowed error" in {
        Put(path) ~> sealRoute(ingestRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: POST")
        }
      }
    }

    "when calling POST to the " + path + " path with a malformed uBAMIngest object" - {
      "should return an invalid response" in {
        val malformedEntity = HttpEntity(ContentType(MediaTypes.`application/json`), """{"random":"data"}""")
        Post(path, malformedEntity) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> sealRoute(ingestRoute) ~> check {
          status should equal(BadRequest)
        }
      }
    }

  }

}
