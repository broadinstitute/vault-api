package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.DmClientService.DMAnalysisResolved
import org.broadinstitute.dsde.vault.services.analysis.DescribeServiceHandler.DescribeMessage
import spray.routing.RequestContext

object DescribeServiceHandler {
  case class DescribeMessage(dmId: String)

  def props(requestContext: RequestContext, dmService: ActorRef): Props =
    Props(new DescribeServiceHandler(requestContext, dmService))
}

case class DescribeServiceHandler(requestContext: RequestContext, dmService: ActorRef) extends Actor {

  import org.broadinstitute.dsde.vault.services.analysis.DescribeJsonProtocol._
  import spray.httpx.SprayJsonSupport._
  implicit val system = context.system
  val log = Logging(system, getClass)

  def receive = {
    case DescribeMessage(dmId) =>
      log.debug("Received Analysis describe message")
      dmService ! DmClientService.DMResolveAnalysis(dmId)

    case DMAnalysisResolved(analysis) =>
      requestContext.complete(analysis)
      context.stop(self)

    case unknown =>
      log.error("Client failure: " + unknown)
      requestContext.reject()
      context.stop(self)
  }

}
