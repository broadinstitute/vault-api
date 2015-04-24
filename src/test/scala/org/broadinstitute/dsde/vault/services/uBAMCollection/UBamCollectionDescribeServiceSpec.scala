package org.broadinstitute.dsde.vault.services.uBAMCollection

import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamCollection, UBamIngest, UBamIngestResponse,UBamCollectionIngest}
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class UBamCollectionDescribeServiceSpec extends VaultFreeSpec with UBamCollectionDescribeService with UBamCollectionIngestService with UBamIngestService {

  override val routes = ubamCollectionDescribeRoute ~ uBamIngestRoute

  def actorRefFactory = system

  val openAmResponse = getOpenAmToken.get
  var testingUbamId = "ubam_invalid_UUID"
  var testingubamCollectionId = "ubamCollection_invalid_UUID"


  val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
  val metadata = Map("testAttr" -> "testValue")

  val members = Option(Seq[String](testingUbamId))

  "UBamCollectionDescribeServiceSpec" - {


    "while preparing the ubamCollection test data" - {
      "should successfully store the data using the UBam Ingest path" in {
        val ubamIngest = new UBamIngest(files, metadata)
        Post(VaultConfig.Vault.ubamIngestPath, ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
          status should equal(OK)
          testingUbamId = responseAs[UBamIngestResponse].id
        }
      }
    }

    "while preparing the  ubamCollection test data" - {
      "should successfully store the data using the ubamCollection Ingest path" in {
        val ubamCollectionIngest = new UBamCollectionIngest(members, metadata)
        Post(VaultConfig.Vault.ubamCollectionIngestPath, ubamCollectionIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBAMCollectionIngestRoute ~> check {
          status should equal(OK)
          testingubamCollectionId = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when calling GET to the UBamCollection Describe path with a Vault ID" - {
      "should return that ID" in {
        Get(VaultConfig.Vault.ubamCollectionDescribePath(testingubamCollectionId)) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> ubamCollectionDescribeRoute ~> check {
          status should equal(OK)
          val response = responseAs[UBamCollection]
          response.id should be(testingubamCollectionId)
          response.members.get(0) should be(testingUbamId)
          response.metadata should equal(Map("testAttr" -> "testValue"))
        }
      }
    }
  }
}

