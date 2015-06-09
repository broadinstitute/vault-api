package org.broadinstitute.dsde.vault.services.lookup

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService.DMLookupResolved
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.lookup.LookupServiceHandler.LookupMessage
import org.broadinstitute.dsde.vault.DmClientService
import spray.json._
import spray.routing.RequestContext

object LookupServiceHandler {
  case class LookupMessage(entityType: String, attributeName: String, attributeValue: String)

  def props(requestContext: RequestContext, version: Int, dmService: ActorRef): Props =
    Props(new LookupServiceHandler(requestContext, version, dmService))
}

case class LookupServiceHandler(requestContext: RequestContext, version: Int, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case LookupMessage(entityType, attributeName, attributeValue) =>
      log.debug("Received lookup message")
      dmService ! DmClientService.DMLookupEntity(entityType, attributeName, attributeValue, version)

    case DMLookupResolved(result: EntitySearchResult) =>
      requestContext.complete(result.toJson.prettyPrint)
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)
  }
}
