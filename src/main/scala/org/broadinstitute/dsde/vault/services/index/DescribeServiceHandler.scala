package org.broadinstitute.dsde.vault.services.index

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.index.DescribeServiceHandler.Index
import spray.routing.RequestContext

object DescribeServiceHandler {
  case class Index(entityType: String)

  def props(requestContext: RequestContext, version: Int, dmService: ActorRef): Props =
    Props(new DescribeServiceHandler(requestContext, version, dmService))

}

case class DescribeServiceHandler(requestContext: RequestContext, version: Int, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case Index(entityType) =>
      log.debug("Received entity type")
      dmService ! DmClientService.DMResolveIndex(entityType,version)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)
  }
}
