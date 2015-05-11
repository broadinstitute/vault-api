package org.broadinstitute.dsde.vault.services.uBAMCollection

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.DmClientService._
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamCollection, UBamCollectionIngest}
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.uBAMCollection.IngestServiceHandler.IngestMessage
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

object IngestServiceHandler {
  case class IngestMessage(ingest: UBamCollectionIngest)

  def props(requestContext: RequestContext, version: Int, dmService: ActorRef): Props =
    Props(new IngestServiceHandler(requestContext, version, dmService))
}

case class IngestServiceHandler(requestContext: RequestContext, version: Int, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case IngestMessage(ingest: UBamCollectionIngest) =>
      log.debug("Received UBamCollection ingest message")
      dmService ! DmClientService.DMCreateUBamCollection(ingest, version)

    case DMUBamCollectionCreated(ubamCollection: UBamCollection) =>
      log.debug("UBamCollection ingest complete")
      requestContext.complete(ubamCollection)
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)
  }
}