package org.broadinstitute.dsde.vault.services.lookup

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model.{UBamIngestResponse, UBamIngest, EntitySearchResult}
import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class LookupServiceSpec extends VaultFreeSpec with LookupService with UBamIngestService {

  override val routes = lookupRoute

  def actorRefFactory = system

  val openAmResponse = getOpenAmToken.get

  var testDataGuid: String = "not-a-uuid"
  val testValue = java.util.UUID.randomUUID().toString

  "LookupServiceSpec" - {
    "while preparing the ubam test data" - {
      "should successfully store the data using the UBam Ingest path" in {
        val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
        val metadata = Map("testAttr" -> "testValue", "uniqueTest" -> testValue)
        val ubamIngest = new UBamIngest(files, metadata)
        Post(VaultConfig.Vault.ubamIngestPath, ubamIngest) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~> uBamIngestRoute ~> check {
          status should equal(OK)
          testDataGuid = responseAs[UBamIngestResponse].id
        }
      }
    }

    "when accessing the Lookup path" - {
      "Lookup should return previously stored unmapped BAM" in {
            Get(VaultConfig.Vault.lookupPath("ubam", "uniqueTest", testValue)) ~> Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId)) ~>  lookupRoute ~> check {
              val entitySearchResult = responseAs[EntitySearchResult]
              entitySearchResult.guid should be(testDataGuid)
               entitySearchResult.`type` should be("ubam")
            }
      }

      "Lookup of unknown entity type should return not found" in {
        Get(VaultConfig.Vault.lookupPath("ubam_similar", "uniqueTest", testValue)) ~> sealRoute(lookupRoute) ~> check {
          status === NotFound
        }
      }

      "Lookup of unknown attribute name return not found" in {
        Get(VaultConfig.Vault.lookupPath("ubam", "uniqueTest_similar", testValue)) ~> sealRoute(lookupRoute) ~> check {
          status === NotFound
        }
      }

      "Lookup of unknown attribute value should return not found" in {
        Get(VaultConfig.Vault.lookupPath("ubam", "uniqueTest", "unknownValue")) ~> sealRoute(lookupRoute) ~> check {
          status === NotFound
        }
      }

      "Lookup of mismatched attribute name + value should return not found" in {
        Get(VaultConfig.Vault.lookupPath("ubam", "testAttr", testValue)) ~> sealRoute(lookupRoute) ~> check {
          status === NotFound
        }
      }
    }
  }
}
