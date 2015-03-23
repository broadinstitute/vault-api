package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.{Props, Actor, ActorRef}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.BossClientService.BossObjectResolved
import org.broadinstitute.dsde.vault.DmClientService.DMUBamResolved
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.uBAM.RedirectServiceHandler.RedirectMessage
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import spray.http.StatusCodes
import spray.routing.RequestContext

object RedirectServiceHandler {
  case class RedirectMessage(bossId: String, fileType: String)

  def props(requestContext: RequestContext, bossService: ActorRef, dmService: ActorRef): Props =
    Props(new RedirectServiceHandler(requestContext, bossService, dmService))
}

case class RedirectServiceHandler(requestContext: RequestContext, bossService: ActorRef, dmService: ActorRef) extends Actor {

  import scala.concurrent.duration._
  implicit val timeout = Timeout(5.seconds)
  implicit val system = context.system
  val log = Logging(system, getClass)

  var fileType: String = _

  def receive = {
    case RedirectMessage(dmId: String, fileType: String) =>
      log.debug("Received uBAM redirect message")
      this.fileType = fileType
      dmService ! DmClientService.DMResolveUBam(dmId)

    case DMUBamResolved(resolvedUBam: uBAM) =>
      log.debug("Resolved uBAM with DM ID " + resolvedUBam.id)
      resolvedUBam.files get fileType match {
        case Some(bossId) =>
          log.debug("Resolved file with Boss ID " + bossId)
          bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("GET"), bossId, fileType)

        case None =>
          log.error("File type not found: " + fileType)
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
