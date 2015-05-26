package org.broadinstitute.dsde.vault.services.uBAMCollection

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.DmClientService.DMUBamCollectionResolved
import org.broadinstitute.dsde.vault.model.TermSearch
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.uBAMCollection.DescribeServiceHandler.{DescribeMessageFilterByTerm, DescribeMessage}
import spray.json._
import spray.routing.RequestContext

object DescribeServiceHandler {
  case class DescribeMessage(dmId: String)
  case class DescribeMessageFilterByTerm(termSearch: List[TermSearch])

  def props(requestContext: RequestContext, version: Int, dmService: ActorRef): Props =
    Props(new DescribeServiceHandler(requestContext, version, dmService))
}

case class DescribeServiceHandler(requestContext: RequestContext, version: Int, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case DescribeMessage(dmId) =>
      log.debug("Received Collection describe message")
      dmService ! DmClientService.DMResolveUBamCollection(dmId, version)

    case DescribeMessageFilterByTerm(termSearch) =>
      log.debug("Received Collection describe message filter by term search")
      dmService ! DmClientService.DMResolveUBamCollectionFilterByTermSearch(termSearch, version)

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
