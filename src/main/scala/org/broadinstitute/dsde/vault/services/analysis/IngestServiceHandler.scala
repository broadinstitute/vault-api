package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.DmClientService.DMAnalysisCreated
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.analysis.IngestServiceHandler.IngestMessage
import spray.routing.RequestContext

object IngestServiceHandler {

  case class IngestMessage(ingest: AnalysisIngest)

  def props(requestContext: RequestContext, dmService: ActorRef): Props =
    Props(new IngestServiceHandler(requestContext, dmService))

}

case class IngestServiceHandler(requestContext: RequestContext, dmService: ActorRef) extends Actor {

  import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
  import spray.httpx.SprayJsonSupport._
  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case IngestMessage(ingest: AnalysisIngest) =>
      log.debug("Received Analysis ingest message")
      dmService ! DmClientService.DMCreateAnalysis(ingest)

    case DMAnalysisCreated(analysis: Analysis) =>
      log.debug("Analysis ingest complete")
      requestContext.complete(analysis)
      context.stop(self)

    case unknown =>
      log.error("Client failure: " + unknown)
      requestContext.reject()
      context.stop(self)

  }

}
