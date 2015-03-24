package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.model.{Analysis, AnalysisIngest, uBAMIngest, uBAM}
import org.broadinstitute.dsde.vault.services.uBAM.ClientFailure
import spray.client.pipelining._
import spray.http.HttpHeaders.Cookie
import spray.routing.RequestContext

import scala.util.{Failure, Success}

object DmClientService {
  case class DMCreateUBam(ubam: uBAMIngest)
  case class DMUBamCreated(createdUBam: uBAM)
  case class DMResolveUBam(ubamId: String)
  case class DMUBamResolved(dmObject: uBAM)

  case class DMCreateAnalysis(analysisIngest: AnalysisIngest)
  case class DMAnalysisCreated(analysis: Analysis)
  case class DMResolveAnalysis(analysisId: String)
  case class DMAnalysisResolved(analysis: Analysis)

  def props(requestContext: RequestContext): Props = Props(new DmClientService(requestContext))
}

case class DmClientService(requestContext: RequestContext) extends Actor {

  import org.broadinstitute.dsde.vault.DmClientService._
  import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
  import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
  import spray.httpx.SprayJsonSupport._
  import system.dispatcher

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  implicit val system = context.system
  val log = Logging(system, getClass)

  override def receive: Receive = {
    case DMCreateUBam(ubam) =>
      val requestor = sender()
      createUBam(requestor, ubam)

    case DMResolveUBam(ubamId) =>
      val requestor = sender()
      resolveUBam(requestor, ubamId)

    case DMCreateAnalysis(analysisIngest) =>
      val requestor = sender()
      createAnalysis(requestor, analysisIngest)

    case DMResolveAnalysis(analysisId) =>
      val requestor = sender()
      resolveAnalysis(requestor, analysisId)
  }

  def createUBam(senderRef: ActorRef, ubam: uBAMIngest): Unit = {
    log.debug("Creating a uBAM object in the DM")
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[uBAM]
    val responseFuture = pipeline {
      Post(VaultConfig.DataManagement.ubamsUrl, ubam)
    }
    responseFuture onComplete {
      case Success(createdUBam) =>
        log.debug("uBAM created with id: " + createdUBam.id)
        senderRef ! DMUBamCreated(createdUBam)

      case Failure(error) =>
        log.error(error, "Failure creating uBAM object")
        senderRef ! ClientFailure(error.getMessage)
    }
  }

  def resolveUBam(senderRef: ActorRef, ubamId: String): Unit = {
    log.debug("Querying the DM API for a uBAM id: " + ubamId)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[uBAM]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.uBamResolveUrl(ubamId))
    }
    responseFuture onComplete {
      case Success(resolvedUBam) =>
        log.debug("uBAM found: " + resolvedUBam.id)
        senderRef ! DMUBamResolved(resolvedUBam)

      case Failure(error) =>
        log.error(error, "Couldn't find uBAM with id: " + ubamId)
        senderRef ! ClientFailure(error.getMessage)
    }
  }

  def createAnalysis(senderRef: ActorRef, analysisIngest: AnalysisIngest): Unit = {
    log.debug("Creating an Analysis object in the DM")
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[Analysis]
    val responseFuture = pipeline {
      Post(VaultConfig.DataManagement.analysesUrl, analysisIngest)
    }
    responseFuture onComplete {
      case Success(createdAnalysis) =>
        log.debug("Analysis created with id: " + createdAnalysis.id)
        senderRef ! DMAnalysisCreated(createdAnalysis)

      case Failure(error) =>
        log.error(error, "Failure creating Analysis object")
        senderRef ! ClientFailure(error.getMessage)
    }
  }

  def resolveAnalysis(senderRef: ActorRef, analysisId: String): Unit = {
    log.debug("Querying the DM API for an Analysis id: " + analysisId)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[Analysis]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.analysesResolveUrl(analysisId))
    }
    responseFuture onComplete {
      case Success(analysis) =>
        log.debug("Analysis found: " + analysis.id)
        senderRef ! DMAnalysisResolved(analysis)

      case Failure(error) =>
        log.error(error, "Couldn't find Analysis with id: " + analysisId)
        senderRef ! ClientFailure(error.getMessage)
    }
  }

}