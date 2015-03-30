package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.{Props, Actor, ActorRef}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService.{BossObjectResolved, BossObjectCreated}
import org.broadinstitute.dsde.vault.DmClientService.DMUBamCreated
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.{DmClientService, BossClientService}
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.uBAM.IngestServiceHandler._
import spray.routing.RequestContext

import spray.json._
import uBAMJsonProtocol._

object IngestServiceHandler {
  case class IngestMessage(ingest: UBamIngest, forceLocation: Option[String] = None)

  def props(requestContext: RequestContext, bossService: ActorRef, dmService: ActorRef): Props =
    Props(new IngestServiceHandler(requestContext, bossService, dmService))
}

case class IngestServiceHandler(requestContext: RequestContext, bossService: ActorRef, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  // the number of files associated with this uBAM
  // set by IngestMessage
  var fileCount: Integer = _

  // the metadata associated with this uBAM
  // set by IngestMessage
  var metadata: Map[String,String] = Map.empty

  // a map from file types to Boss IDs
  // added by BossObjectCreated messages (1 per message)
  // consumed by DmClientService.CreateUBam after [fileCount] have been added
  var bossObjects: Map[String, String] = Map.empty

  // a map from file types to Presigned Boss PUT URLs
  // added by BossObjectResolved messages (1 per message)
  // consumed by UBamIngestResponse after [fileCount] have been added and the DM ID has been set
  var bossURLs: Map[String, String] = Map.empty

  // the DM ID for this uBAM
  // set by DMUBamCreated
  // consumed by UBamIngestResponse after this has been set and [fileCount] bossURLs have been added
  var dmId: Option[String] = None

  // Capture the update files in the case of "X-Force-Location" header.
  var providedFiles: Map[String, String] = Map.empty

  // Capture the case where the client is using "X-Force-Location" headers.
  // No need to populate bossURLs and we return the Analysis using the provided files.
  var forceLocationHeader: Boolean = false

  def receive = {
    case IngestMessage(ingest: UBamIngest, forceLocation: Option[String]) =>
      log.debug("Received uBAM ingest message")
      fileCount = ingest.files.size
      metadata = ingest.metadata
      forceLocationHeader = forceLocation.getOrElse("false").toBoolean
      providedFiles = ingest.files
      ingest.files.foreach {
        case (ftype, fpath) =>
          bossService ! BossClientService.BossCreateObject(BossDefaults.getCreationRequest(fpath, forceLocation), ftype)
      }

    case BossObjectCreated(bossObject: BossCreationObject, creationKey: String) =>
      bossObjects += creationKey -> bossObject.objectId.get
      // If the client is using the "X-Force-Location" header, no need to populate PUT urls.
      // Instead, forward to the DM service if all BOSS objects have been created.
      if (forceLocationHeader && fileCount == bossObjects.size) {
        dmService ! DmClientService.DMCreateUBam(new UBamIngest(bossObjects, metadata))
      }
      else {
        bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("PUT"), bossObject.objectId.get, creationKey)
        if (fileCount == bossObjects.size)
          dmService ! DmClientService.DMCreateUBam(new UBamIngest(bossObjects, metadata))
      }

    case BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String) =>
      bossURLs += creationKey -> bossObject.objectUrl
      completeIfDone

    case DMUBamCreated(createdUBam: UBam) =>
      this.dmId = Option(createdUBam.id)
      completeIfDone

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)
  }

  def completeIfDone =
    dmId match {
      case Some(id) =>
        forceLocationHeader match {
          case true =>
            log.debug("'X-Force-Location' uBAM ingest complete")
            requestContext.complete(UBamIngestResponse(id, providedFiles).toJson.prettyPrint)
            context.stop(self)
          case false =>
            if (fileCount == bossURLs.size) {
              log.debug("uBAM ingest complete")
              requestContext.complete(UBamIngestResponse(id, bossURLs).toJson.prettyPrint)
              context.stop(self)
            }
        }
      case _ => None
    }
}
