package org.broadinstitute.dsde.vault.services.uBAMCollection

import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamCollectionIngest, UBamCollectionIngestResponse, UBamIngest, UBamIngestResponse}
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.scalatest.BeforeAndAfterAll
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class UBamCollectionIngestServiceSpec extends VaultFreeSpec with UBamCollectionIngestService with UBamIngestService with BeforeAndAfterAll {

  def actorRefFactory = system
  val openAmResponse = getOpenAmToken.get

  var createdUBams: Seq[String] = Seq("bad", "ids")

  override val routes = uBAMCollectionIngestRoute
  import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._

  override def beforeAll(): Unit = {

    // test ubam for ingest
    val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
    val metadata = Map("testAttr" -> "testValue")
    val ubamIngest = new UBamIngest(files, metadata)

    // create a few ubams
    createdUBams = (for (x <- 1 to 3) yield

    Post(VaultConfig.Vault.ubamIngestPath, ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
      status should equal(OK)
      responseAs[UBamIngestResponse].id
    }

      ).sorted.toSeq.asInstanceOf[Seq[String]]

  }

  "UBamCollectionIngestServiceSpec" - {
    "when calling POST to the UBam Collection Ingest path with valid members and valid metadata" - {
      "should return an ID" in {
        val members = Some(List(createdUBams.head))
        val ubamCollectionIngest = new UBamCollectionIngest(
          members,
          metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
        )

        Post(VaultConfig.Vault.ubamCollectionIngestPath, ubamCollectionIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBAMCollectionIngestRoute ~> check {
          status should equal(OK)
          // test response as raw string
          entity.toString should include("id")
          // test response as json, unmarshaled into an object
          val respCollection = responseAs[UBamCollectionIngestResponse]
          val createdId = java.util.UUID.fromString(respCollection.id)
          entity.toString should include(createdId.toString)
        }
      }
    }
  }
}
