package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.DmClientService.DMAnalysisUpdated
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model.AnalysisUpdate
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.analysis.UpdateServiceHandler.UpdateMessage
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

object UpdateServiceHandler {

  case class UpdateMessage(dmId: String, update: AnalysisUpdate)

  def props(requestContext: RequestContext, dmService: ActorRef): Props =
    Props(new UpdateServiceHandler(requestContext, dmService))

}

case class UpdateServiceHandler(requestContext: RequestContext, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  override def receive: Receive = {

    case UpdateMessage(dmId: String, update: AnalysisUpdate) =>
      dmService ! DmClientService.DMUpdateAnalysis(dmId, update)

    case DMAnalysisUpdated(analysis) =>
      requestContext.complete(analysis)
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)

  }

}
