package org.broadinstitute.dsde.vault.services

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.generic.{GenericDescribeService, GenericIngestService}

import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import spray.http.StatusCodes.{OK, NotFound}
import spray.httpx.SprayJsonSupport._

class GenericServiceSpec extends VaultFreeSpec with GenericIngestService with GenericDescribeService {

  def actorRefFactory = system

  var uBamGuid: String = "INVALID_UBAM_GUID"
  var bamGuid: String = "INVALID_BAM_GUID"
  var baiGuid: String = "INVALID_BAI_GUID"

  "GenericService" - {

    val routes = giRoute ~ gdRoutes

    val versions = Table(
      "version",
      1
    )

    forAll(versions) { (version: Int) =>
      val ingestPath = VaultConfig.Vault.genericIngestPath(version)

      s"when accessing the $ingestPath path" - {
        val aValue = "arbitrary_value"

        val uBAMAttrs = Map("ownerId" -> "me", "queryAttr" -> aValue)
        val bamAttrs = Map("bamFile" -> "test.bam")
        val baiAttrs = Map("baiFile" -> "test.bai")
        val bamRelAttrs = Map("name" -> "bam")
        val baiRelAttrs = Map("name" -> "bai")
        val objSpec = DataObjectSpecification(ingestMethod = "new", source = None)

        val ingest = GenericIngest(
          Some(List(GenericEntityIngest("unmappedBAM", None, None, uBAMAttrs),
            GenericEntityIngest("bamFile", None, Some(objSpec), bamAttrs),
            GenericEntityIngest("baiFile", None, Some(objSpec), baiAttrs))),
          Some(List(GenericRelationshipIngest("fileType", "$0", "$1", bamRelAttrs),
            GenericRelationshipIngest("fileType", "$0", "$2", baiRelAttrs))))

        "POST should store a new uBAM generically" in {
          Post(ingestPath, ingest) ~> addOpenAmCookie ~> sealRoute(routes) ~> check {
            status should be(OK)  // TODO: 201/Created?
            val entities = responseAs[List[GenericEntity]]

            entities should have length 3

            entities(0).entityType should be("unmappedBAM")
            entities(0).attrs.nonEmpty should be(true)
            entities(0).attrs.get should be(uBAMAttrs)
            entities(0).guid should be a UUID
            uBamGuid = entities(0).guid   // save for later tests

            entities(1).entityType should be("bamFile")
            entities(1).attrs.nonEmpty should be(true)
            entities(1).attrs.get should be(bamAttrs)
            entities(1).guid should be a UUID
            bamGuid = entities(1).guid   // save for later tests

            entities(2).entityType should be("baiFile")
            entities(2).attrs.nonEmpty should be(true)
            entities(2).attrs.get should be(baiAttrs)
            entities(2).guid should be a UUID
            baiGuid = entities(2).guid   // save for later tests

            entities.tail foreach { ent =>
              ent.signedPutUrl.nonEmpty should be(true)
              ent.signedPutUrl.get should be a URL
              ent.signedGetUrl.isEmpty should be(true)
              ent.sysAttrs.bossID.isEmpty should be(true)
            }
          }
        }

       "fetchEntity should retrieve the uBAM" in {
          Get(VaultConfig.Vault.genericDescribePath(version, uBamGuid)) ~> addOpenAmCookie ~> sealRoute(routes) ~> check {
            status should be(OK)
            val uBAM = responseAs[GenericEntity]
            uBAM.guid shouldBe uBamGuid
            uBAM.entityType shouldBe "unmappedBAM"
            uBAM.sysAttrs.bossID shouldBe empty
            uBAM.sysAttrs.createdBy shouldBe "TestAccount"
            uBAM.attrs shouldBe Some(uBAMAttrs)
          }
        }

        "fetchEntity on an unknown id should return a not found error" in {
          Get(VaultConfig.Vault.genericDescribePath(version, "unknown-id")) ~> addOpenAmCookie ~> sealRoute(routes) ~> check {
            status should be(NotFound)
          }
        }

        "findDownstream from the uBAM ought to return the bam and bai files" in {
          Get(VaultConfig.Vault.genericDescribeDownPath(version, uBamGuid)) ~> addOpenAmCookie ~> sealRoute(routes) ~> check {
            status should be(OK)
            val dRelEnts = responseAs[List[GenericRelEnt]]
            dRelEnts should have length 2

            dRelEnts foreach { relEnt =>
              relEnt.entity.signedPutUrl.isEmpty should be(true)
              relEnt.entity.signedGetUrl.nonEmpty should be(true)
              relEnt.entity.signedGetUrl.get should be a URL
              relEnt.entity.sysAttrs.bossID.isEmpty should be(true)
            }

            val bamRelEnt = if (dRelEnts(0).entity.guid == bamGuid) dRelEnts(0) else dRelEnts(1)
            bamRelEnt.relationship.relationType shouldBe "fileType"
            bamRelEnt.relationship.attrs shouldBe Some(bamRelAttrs)
            bamRelEnt.entity.guid shouldBe (bamGuid)
            bamRelEnt.entity.entityType shouldBe "bamFile"
            bamRelEnt.entity.sysAttrs.createdBy shouldBe "TestAccount"
            bamRelEnt.entity.attrs shouldBe Some(bamAttrs)

            val baiRelEnt = if (dRelEnts(1).entity.guid == baiGuid) dRelEnts(1) else dRelEnts(0)
            baiRelEnt.relationship.relationType shouldBe "fileType"
            baiRelEnt.relationship.attrs shouldBe Some(baiRelAttrs)
            baiRelEnt.entity.guid shouldBe (baiGuid)
            baiRelEnt.entity.entityType shouldBe "baiFile"
            baiRelEnt.entity.sysAttrs.createdBy shouldBe "TestAccount"
            baiRelEnt.entity.attrs shouldBe Some(baiAttrs)
          }
        }

        "findUpstream from the bam ought to return the uBAM" in {
          Get(VaultConfig.Vault.genericDescribeUpPath(version, bamGuid)) ~> addOpenAmCookie ~> sealRoute(routes) ~> check {
            status should be(OK)
            val uRelEnts = responseAs[List[GenericRelEnt]]
            uRelEnts should have length 1

            val uBAMRelEnt = uRelEnts(0)
            uBAMRelEnt.relationship.relationType shouldBe "fileType"
            uBAMRelEnt.relationship.attrs shouldBe Some(bamRelAttrs)
            uBAMRelEnt.entity.guid shouldBe (uBamGuid)
            uBAMRelEnt.entity.entityType shouldBe "unmappedBAM"
            uBAMRelEnt.entity.sysAttrs.bossID shouldBe empty
            uBAMRelEnt.entity.sysAttrs.createdBy shouldBe "TestAccount"
            uBAMRelEnt.entity.attrs shouldBe Some(uBAMAttrs)
          }
        }
      }
    }
  }

}
