package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.{Props, Actor, ActorRef}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService.DMUBamResolved
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.uBAM.DescribeServiceHandler.DescribeMessage
import org.broadinstitute.dsde.vault.{VaultConfig, DmClientService}
import spray.routing.RequestContext

import spray.json._
import uBAMJsonProtocol._

object DescribeServiceHandler {
  case class DescribeMessage(dmId: String)

  def props(requestContext: RequestContext, dmService: ActorRef): Props =
    Props(new DescribeServiceHandler(requestContext, dmService))
}

case class DescribeServiceHandler(requestContext: RequestContext, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case DescribeMessage(dmId) =>
      log.debug("Received uBAM describe message")
      dmService ! DmClientService.DMResolveUBam(dmId)

    case DMUBamResolved(resolvedUBam: uBAM) =>
      val redirects = resolvedUBam.files.map {
        case (fileType, _) => (fileType, VaultConfig.Vault.uBamRedirectUrl(resolvedUBam.id, fileType))
      }
      requestContext.complete(uBAM(resolvedUBam.id, redirects, resolvedUBam.metadata).toJson.prettyPrint)
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)
  }
}
