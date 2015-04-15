package org.broadinstitute.dsde.vault.services.lookup

import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.model.{EntitySearchResult, UBamIngest, UBamIngestResponse}
import org.broadinstitute.dsde.vault.services.mock.MockServers
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class LookupServiceSpec extends VaultFreeSpec with LookupService with UBamIngestService with MockServers {

  def actorRefFactory = system

  var testDataGuid: String = "not-a-uuid"
  val testValue = java.util.UUID.randomUUID().toString

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  "LookupServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      val testValue = java.util.UUID.randomUUID().toString
      var testDataGuid: String = "not-a-uuid"

      s"when accessing version = '${v(version)}'" - {

        "while preparing the ubam test data" - {
          "should successfully store the data using the UBam Ingest path" in {
            val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
            val metadata = Map("testAttr" -> "testValue", "uniqueTest" -> testValue)
            val ubamIngest = new UBamIngest(files, metadata)
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              testDataGuid = responseAs[UBamIngestResponse].id
            }
          }
        }

        "when accessing the Lookup path" - {
          "Lookup should return previously stored unmapped BAM" in {
            Get(VaultConfig.Vault.lookupPath("ubam", "uniqueTest", testValue).versioned(version)) ~> addOpenAmCookie ~> lookupRoute ~> check {
              val entitySearchResult = responseAs[EntitySearchResult]
              entitySearchResult.guid should be(testDataGuid)
              entitySearchResult.`type` should be("ubam")
            }
          }

          "Lookup of unknown entity type should return not found" in {
            Get(VaultConfig.Vault.lookupPath("ubam_similar", "uniqueTest", testValue).versioned(version)) ~> sealRoute(lookupRoute) ~> check {
              status === NotFound
            }
          }

          "Lookup of unknown attribute name return not found" in {
            Get(VaultConfig.Vault.lookupPath("ubam", "uniqueTest_similar", testValue).versioned(version)) ~> sealRoute(lookupRoute) ~> check {
              status === NotFound
            }
          }

          "Lookup of unknown attribute value should return not found" in {
            Get(VaultConfig.Vault.lookupPath("ubam", "uniqueTest", "unknownValue").versioned(version)) ~> sealRoute(lookupRoute) ~> check {
              status === NotFound
            }
          }

          "Lookup of mismatched attribute name + value should return not found" in {
            Get(VaultConfig.Vault.lookupPath("ubam", "testAttr", testValue).versioned(version)) ~> sealRoute(lookupRoute) ~> check {
              status === NotFound
            }
          }
        }
      }
    }
  }
}
