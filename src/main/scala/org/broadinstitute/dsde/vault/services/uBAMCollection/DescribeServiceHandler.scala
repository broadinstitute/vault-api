package org.broadinstitute.dsde.vault.services.uBAMCollection

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService.DMUBamCollectionResolved
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.uBAMCollection.DescribeServiceHandler.DescribeMessage
import org.broadinstitute.dsde.vault.{DmClientService, VaultConfig}
import spray.routing.RequestContext

import spray.json._
import uBAMCollectionJsonProtocol._

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
      log.debug("Received Collection describe message")
      dmService ! DmClientService.DMResolveUBamCollection(dmId)

    case DMUBamCollectionResolved(resolveduBamCollection) =>
      log.debug("Received Collection resolved message")
      requestContext.complete(resolveduBamCollection.toJson.prettyPrint)
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)

  }

}
