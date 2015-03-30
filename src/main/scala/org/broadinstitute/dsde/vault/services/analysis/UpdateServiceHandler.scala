package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService.{BossObjectResolved, BossObjectCreated}
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import org.broadinstitute.dsde.vault.DmClientService.DMAnalysisUpdated
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.analysis.UpdateServiceHandler.UpdateMessage
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

object UpdateServiceHandler {

  case class UpdateMessage(dmId: String, update: AnalysisUpdate, forceLocation: Option[String] = None)

  def props(requestContext: RequestContext, dmService: ActorRef, bossService: ActorRef): Props =
    Props(new UpdateServiceHandler(requestContext, dmService, bossService))

}

case class UpdateServiceHandler(requestContext: RequestContext, dmService: ActorRef, bossService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  // the number of files associated with this analysis
  // set by AnalysisUpdate
  var fileCount: Integer = _

  // a map from file types to Boss IDs
  // added by BossObjectCreated messages (1 per message)
  // consumed by DmClientService.DMUpdateAnalysis after [fileCount] have been added
  var bossObjects: Map[String, String] = Map.empty

  // a map from file types to Presigned Boss PUT URLs
  // added by BossObjectResolved messages (1 per message)
  // consumed by DMAnalysisUpdated after [fileCount] have been added and the DM ID has been set
  var bossURLs: Map[String, String] = Map.empty

  // the DM ID for this analysis
  // set by UpdateMessage
  var dmId: Option[String] = None

  // Capture the update files in the case of "X-Force-Location" header.
  var providedFiles: Map[String, String] = Map.empty

  // Capture the case where the client is using "X-Force-Location" headers.
  // No need to populate bossURLs and we return the Analysis using the provided files.
  var forceLocationHeader: Boolean = false

  override def receive: Receive = {

    case UpdateMessage(id: String, update: AnalysisUpdate, forceLocation: Option[String]) =>
      dmId = Some(id)
      fileCount = update.files.size
      forceLocationHeader = forceLocation.getOrElse("false").toBoolean
      providedFiles = update.files
      update.files.foreach {
        case (ftype, fpath) =>
          bossService ! BossClientService.BossCreateObject(BossDefaults.getCreationRequest(fpath, forceLocation), ftype)
      }

    case BossObjectCreated(bossObject: BossCreationObject, creationKey: String) =>
      bossObjects += creationKey -> bossObject.objectId.get
      // If the client is using the "X-Force-Location" header, no need to populate PUT urls.
      // Instead, forward to the DM service if all BOSS objects have been created.
      if (forceLocationHeader && fileCount == bossObjects.size) {
        dmService ! DmClientService.DMUpdateAnalysis(dmId.get, new AnalysisUpdate(providedFiles))
      }
      else {
        bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("PUT"), bossObject.objectId.get, creationKey)
      }

    case BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String) =>
      bossURLs += creationKey -> bossObject.objectUrl
      if (fileCount == bossURLs.size)
        dmService ! DmClientService.DMUpdateAnalysis(dmId.get, new AnalysisUpdate(bossObjects))

    case DMAnalysisUpdated(analysis) =>
      completeIfDone(analysis)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)

  }

  def completeIfDone(analysis: Analysis): Unit =
    forceLocationHeader match {
        case true =>
          if (fileCount == bossObjects.size) {
            log.debug("'X-Force-Location' Analysis update complete")
            requestContext.complete(new Analysis(dmId.get, analysis.input, analysis.metadata, analysis.files))
            context.stop(self)
          }
        case false =>
          if (fileCount == bossURLs.size) {
            log.debug("Analysis update complete")
            requestContext.complete(new Analysis(dmId.get, analysis.input, analysis.metadata, Option(bossURLs)))
            context.stop(self)
          }
      }

}
