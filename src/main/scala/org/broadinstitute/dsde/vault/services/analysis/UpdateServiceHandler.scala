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

  override def receive: Receive = {

    case UpdateMessage(id: String, update: AnalysisUpdate, forceLocation: Option[String]) =>
      dmId = Some(id)
      fileCount = update.files.size
      update.files.foreach {
        case (ftype, fpath) =>
          bossService ! BossClientService.BossCreateObject(BossDefaults.getCreationRequest(fpath, forceLocation), ftype)
      }

    case BossObjectCreated(bossObject: BossCreationObject, creationKey: String) =>
      bossObjects += creationKey -> bossObject.objectId.get
      bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("PUT"), bossObject.objectId.get, creationKey)

    case BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String) =>
      bossURLs += creationKey -> bossObject.objectUrl
      if (fileCount == bossObjects.size)
        dmService ! DmClientService.DMUpdateAnalysis(dmId.get, new AnalysisUpdate(bossObjects))

    case DMAnalysisUpdated(analysis) =>
      requestContext.complete(new Analysis(dmId.get, analysis.input, analysis.metadata, Option(bossURLs)))
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)

  }

}
