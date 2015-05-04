package org.broadinstitute.dsde.vault.services.uBAMCollection

import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamCollectionIngest, UBamIngest, UBamIngestResponse}
import org.broadinstitute.dsde.vault.services.uBAM.UBamIngestService
import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.scalatest.{BeforeAndAfter, DoNotDiscover, Suite}
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

@DoNotDiscover
class UBamCollectionIngestServiceSpec extends VaultFreeSpec with UBamCollectionIngestService with UBamIngestService with BeforeAndAfter with Suite  {

  def actorRefFactory = system

  val versions = Table(
    "version",
    1
  )

  var createdUBams: Seq[String] = Seq("bad", "ids")

  val routes = uBAMCollectionIngestRoute
  import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._

  before{

    // test ubam for ingest
    val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
    val metadata = Map("testAttr" -> "testValue")
    val ubamIngest = new UBamIngest(files, metadata)

    // create a few ubams
    createdUBams = (for (x <- 1 to 3) yield

    Post(VaultConfig.Vault.ubamIngestPath.versioned(1), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
      status should equal(OK)
      responseAs[UBamIngestResponse].id
    }

      ).sorted.toSeq.asInstanceOf[Seq[String]]

  }

  "UBamCollectionIngestServiceSpec" - {
    forAll(versions) { (version: Int) =>

      s"when accessing version = '$version'" - {
        "when calling POST to the UBam Collection Ingest path with valid members and valid metadata" - {
          "should return an ID" in {
            val members = Some(List(createdUBams.head))
            val ubamCollectionIngest = new UBamCollectionIngest(
              members,
              metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
            )

            Post(VaultConfig.Vault.ubamCollectionIngestPath(version), ubamCollectionIngest) ~> addOpenAmCookie ~> uBAMCollectionIngestRoute ~> check {
              status should equal(OK)
              entity.toString should include("id")
              entity.toString should include("members")
              entity.toString should include("metadata")
              entity.toString should include("properties")
            }
          }
        }
      }
    }
  }
}
