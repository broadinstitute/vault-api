package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.BossClientService.BossObjectResolved
import org.broadinstitute.dsde.vault.DmClientService.DMAnalysisResolved
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.analysis.RedirectServiceHandler.RedirectMessage
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import spray.http.StatusCodes
import spray.routing.RequestContext
import scala.concurrent.duration._

object RedirectServiceHandler {
  case class RedirectMessage(bossId: String, fileType: String)

  def props(requestContext: RequestContext, version: Int, bossService: ActorRef, dmService: ActorRef): Props =
    Props(new RedirectServiceHandler(requestContext, version, bossService, dmService))
}

case class RedirectServiceHandler(requestContext: RequestContext, version: Int, bossService: ActorRef, dmService: ActorRef) extends Actor {

  implicit val timeout = Timeout(5.seconds)
  implicit val system = context.system
  val log = Logging(system, getClass)

  var fileType: String = _

  def receive = {
    case RedirectMessage(dmId: String, fileType: String) =>
      log.debug("Received Analysis redirect message")
      this.fileType = fileType
      dmService ! DmClientService.DMResolveAnalysis(dmId)

    case DMAnalysisResolved(resolvedAnalysis: Analysis) =>
      log.debug("Resolved Analysis with DM ID " + resolvedAnalysis.id)
      resolvedAnalysis.files match {
        case Some(bossFiles) =>
          bossFiles get fileType match {
          case Some(bossId) =>
            log.debug("Resolved file with Boss ID " + bossId)
            bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("GET"), bossId, fileType)

          case None =>
            log.error("File type not found: " + fileType)
            requestContext.complete(StatusCodes.BadRequest)
            context.stop(self)
        }
        case None =>
          log.error("No files found")
          requestContext.complete(StatusCodes.BadRequest)
          context.stop(self)
      }

    case BossObjectResolved(bossObject: BossResolutionResponse, fileType: String) =>
      log.debug("Resolved Boss object, redirecting to " + bossObject.objectUrl)
      requestContext.redirect(bossObject.objectUrl, StatusCodes.TemporaryRedirect)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)
  }
}
