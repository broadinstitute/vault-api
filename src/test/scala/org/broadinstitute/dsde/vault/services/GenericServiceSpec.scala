package org.broadinstitute.dsde.vault.services

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.generic.GenericIngestService

import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import spray.httpx.SprayJsonSupport._

class GenericServiceSpec extends VaultFreeSpec with GenericIngestService {

  def actorRefFactory = system

  "GenericService" - {

    val routes = giRoute

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
            Some(List(GenericRelationshipIngest("fileType","$0","$1",bamRelAttrs),
                      GenericRelationshipIngest("fileType","$0","$2",baiRelAttrs))))

        "POST should store a new uBAM generically" in {
          Post(ingestPath, ingest) ~> addOpenAmCookie ~> sealRoute(routes) ~> check {
            val entities = responseAs[List[GenericEntity]]

            entities should have length 3

            entities(0).entityType should be("unmappedBAM")
            entities(0).attrs.nonEmpty should be(true)
            entities(0).attrs.get should be(uBAMAttrs)

            entities(1).entityType should be("bamFile")
            entities(1).attrs.nonEmpty should be(true)
            entities(1).attrs.get should be(bamAttrs)

            entities(2).entityType should be("baiFile")
            entities(2).attrs.nonEmpty should be(true)
            entities(2).attrs.get should be(baiAttrs)

            entities.tail foreach { ent =>
              ent.guid should be a UUID
              ent.signedPutUrl.nonEmpty should be(true)
              ent.signedPutUrl.get should be a URL
              ent.signedGetUrl.isEmpty should be(true)
              ent.sysAttrs.bossID.isEmpty should be(true)
            }
          }
        }
      }
    }
  }

}
