package org.broadinstitute.dsde.vault.services.uBAMCollection

import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.IndexResponseJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.services.index.IndexService
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._


class UBamCollectionDescribeServiceSpec extends VaultFreeSpec with UBamCollectionDescribeService with UBamCollectionIngestService with UBamIngestService with IndexService{

  val routes = ubamCollectionDescribeRoute

  def actorRefFactory = system

  val versions = Table(
    "version",
    1
  )

  var testingUbamId = "ubam_invalid_UUID"
  var testingubamCollectionId = "ubamCollection_invalid_UUID"


  val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
  val metadata = Map("testAttr" -> "testValue")
  val ubamIngest = new UBamIngest(files, metadata)
  var isIndexed: Boolean = false
  "UBamCollectionDescribeServiceSpec" - {

    forAll(versions) { (version: Int) =>
      s"when accessing version = '$version'" - {
        "while preparing the ubamCollection test data" - {
          "should successfully store the data using the UBam Ingest path" in {
            Post(VaultConfig.Vault.ubamIngestPath.versioned(1), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              testingUbamId = responseAs[UBamIngestResponse].id
              assume(testingUbamId != "ubam_invalid_UUID")
            }
          }
        }
        "while preparing the  ubamCollection test data" - {
          "should successfully store the data using the ubamCollection Ingest path" in {
              val members = Some(List(testingUbamId))
              val ubamCollectionIngest = new UBamCollectionIngest(
                members,
                metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
              )
              Post(VaultConfig.Vault.ubamCollectionIngestPath(version), ubamCollectionIngest) ~> addOpenAmCookie ~> uBAMCollectionIngestRoute ~> check {
                status should equal(OK)
                entity.toString should include("id")
                testingubamCollectionId = responseAs[UBamCollectionIngestResponse].id
              }
          }
        }

        "when calling GET to the UBamCollection Describe path with a Vault ID" - {
          "should return that ID" in {
            assume(testingubamCollectionId != "ubamCollection_invalid_UUID")
            Get(VaultConfig.Vault.ubamCollectionDescribePath(testingubamCollectionId, version)) ~> addOpenAmCookie ~> ubamCollectionDescribeRoute ~> check {
              status should equal(OK)
              val response = responseAs[UBamCollection]
              response.id.head should be(testingubamCollectionId)
              response.members.head.head should be(testingUbamId)
              response.metadata should equal(Map("testAttr" -> "testValue", "randomData" -> "7"))
            }
          }
        }

        "POST should index the previously stored Collections" in {
          assume(testingubamCollectionId != "ubamCollection_invalid_UUID")
          Post(VaultConfig.Vault.indexResolvePath("uBAMCollection", version)) ~> addOpenAmCookie ~> indexRoute ~> check {

            import spray.httpx.SprayJsonSupport._
            val indexResult = responseAs[IndexResponse]
            indexResult.messageResult should be("Index  successful")
            isIndexed = true
          }
        }

        "POST should get an error for invalid entity type" in {
          Post(VaultConfig.Vault.indexResolvePath("Test", version)) ~> addOpenAmCookie ~> indexRoute ~> check {
            val indexResult = responseAs[IndexResponse]
            indexResult.messageResult should be("Entity type does not exist")
          }
        }
        import TermSearchJsonProtocol._
       "POST should retrieve the Collections that matches the search criteria " in {
          assume(isIndexed == true)
          val term: TermSearch = new TermSearch("key1","someKey")
          val listTerms: List[TermSearch] =  List.apply(term)
          Post(VaultConfig.Vault.collectionSearchPath(version), listTerms) ~> addOpenAmCookie ~> searchRoute ~> check {
            val collections = responseAs[List[UBamCollection]]
            collections should not be empty
          }
        }
      }
    }
  }

}
