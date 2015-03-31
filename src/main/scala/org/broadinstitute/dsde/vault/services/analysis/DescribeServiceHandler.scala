package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.model.Analysis
import org.broadinstitute.dsde.vault.{VaultConfig, DmClientService}
import org.broadinstitute.dsde.vault.DmClientService.DMAnalysisResolved
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.analysis.DescribeServiceHandler.DescribeMessage
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

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
      log.debug("Received Analysis describe message")
      dmService ! DmClientService.DMResolveAnalysis(dmId)

    case DMAnalysisResolved(resolvedAnalysis) =>
      val redirects = resolvedAnalysis.files.getOrElse(Map.empty) map {
        case (fileType, _) => (fileType, VaultConfig.Vault.analysisRedirectUrl(resolvedAnalysis.id, fileType))
      }

      requestContext.complete(resolvedAnalysis.copy(files = Option(redirects)))
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client failure: " + message)
      requestContext.reject()
      context.stop(self)

  }

}
