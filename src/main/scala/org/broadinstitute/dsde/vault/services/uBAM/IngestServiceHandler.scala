package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.{Props, Actor, ActorRef}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService.{BossObjectResolved, BossObjectCreated}
import org.broadinstitute.dsde.vault.DmClientService.DMUBamCreated
import org.broadinstitute.dsde.vault.{DmClientService, BossClientService}
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.uBAM.IngestServiceHandler._
import spray.routing.RequestContext

import spray.json._
import uBAMJsonProtocol._

object IngestServiceHandler {
  case class IngestMessage(ingest: uBAMIngest)

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
  // consumed by uBAMIngestResponse after [fileCount] have been added and the DM ID has been set
  var bossURLs: Map[String, String] = Map.empty

  // the DM ID for this uBAM
  // set by DMUBamCreated
  // consumed by uBAMIngestResponse after this has been set and [fileCount] bossURLs have been added
  var dmId: Option[String] = None

  def receive = {
    case IngestMessage(ingest: uBAMIngest) =>
      log.debug("Received uBAM ingest message")
      fileCount = ingest.files.size
      metadata = ingest.metadata
      ingest.files.foreach {
        case (ftype, fpath) =>
          bossService ! BossClientService.BossCreateObject(BossDefaults.getCreationRequest(fpath), ftype)
      }

    case BossObjectCreated(bossObject: BossCreationObject, creationKey: String) =>
      bossObjects += creationKey -> bossObject.objectId.get
      bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("PUT"), bossObject.objectId.get, creationKey)
      if (fileCount == bossObjects.size)
        dmService ! DmClientService.DMCreateUBam(new uBAMIngest(bossObjects, metadata))

    case BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String) =>
      bossURLs += creationKey -> bossObject.objectUrl
      completeIfDone

    case DMUBamCreated(createdUBam: uBAM) =>
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
        if (fileCount == bossURLs.size) {
          log.debug("uBAM ingest complete")
          requestContext.complete(uBAMIngestResponse(id, bossURLs).toJson.prettyPrint)
          context.stop(self)
        }
      case _ => None
    }
}
