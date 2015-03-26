package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.model.{Analysis, AnalysisIngest, EntitySearchResult, UBam, UBamIngest, _}
import org.broadinstitute.dsde.vault.services.ClientFailure
import spray.client.pipelining._
import spray.http.HttpHeaders.Cookie
import spray.routing.RequestContext

import scala.util.{Failure, Success}

object DmClientService {
  case class DMCreateUBam(ubam: UBamIngest)
  case class DMUBamCreated(createdUBam: UBam)
  case class DMResolveUBam(ubamId: String)
  case class DMUBamResolved(dmObject: UBam)

  case class DMCreateAnalysis(analysisIngest: AnalysisIngest)
  case class DMAnalysisCreated(analysis: Analysis)
  case class DMResolveAnalysis(analysisId: String)
  case class DMAnalysisResolved(analysis: Analysis)
  case class DMUpdateAnalysis(analysisId: String, update: AnalysisUpdate)
  case class DMAnalysisUpdated(analysis: Analysis)

  case class DMLookupEntity(entityType: String, attributeName: String, attributeValue: String)
  case class DMLookupResolved(result: EntitySearchResult)

  def props(requestContext: RequestContext): Props = Props(new DmClientService(requestContext))
}

case class DmClientService(requestContext: RequestContext) extends Actor {

  import org.broadinstitute.dsde.vault.DmClientService._
  import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
  import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
  import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
  import spray.httpx.SprayJsonSupport._
  import system.dispatcher

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  implicit val system = context.system
  val log = Logging(system, getClass)

  override def receive: Receive = {
    case DMCreateUBam(ubam) =>
      createUBam(sender(), ubam)

    case DMResolveUBam(ubamId) =>
      resolveUBam(sender(), ubamId)

    case DMCreateAnalysis(analysisIngest) =>
      createAnalysis(sender(), analysisIngest)

    case DMResolveAnalysis(analysisId) =>
      resolveAnalysis(sender(), analysisId)

    case DMUpdateAnalysis(analysisId, update) =>
      updateAnalysis(sender(), analysisId, update)

    case DMLookupEntity(entityType, attributeName, attributeValue) =>
      lookup(sender(), entityType, attributeName, attributeValue)
  }

  def createUBam(senderRef: ActorRef, ubam: UBamIngest): Unit = {
    log.debug("Creating a uBAM object in the DM")
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[UBam]
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
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[UBam]
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

  // TODO: Fix the following hack when reverse-lookup authentication service is implemented
  // Any metadata passed in needs to be ignored. Metadata passed to DM needs to include a temporary ownerId key/value.
  // This needs to change when authorship is appropriately determined through the authentication service.
  def updateAnalysis(senderRef: ActorRef, analysisId: String, update: AnalysisUpdate): Unit = {
    log.debug("Updating an Analysis through the DM API for Analysis id: " + analysisId)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[Analysis]
    val analysisDmUpdate = new AnalysisDMUpdate(update.files, Map("ownerId" -> "foo"))
    val responseFuture = pipeline {
      Post(VaultConfig.DataManagement.analysesUpdateUrl(analysisId), analysisDmUpdate)
    }
    responseFuture onComplete {
      case Success(analysis) =>
        log.debug("Analysis updated: " + analysis.id)
        senderRef ! DMAnalysisUpdated(analysis)

      case Failure(error) =>
        log.error(error, "Couldn't update Analysis with id: " + analysisId)
        senderRef ! ClientFailure(error.getMessage)
    }
  }

  def lookup(senderRef: ActorRef, entityType: String, attributeName: String, attributeValue: String): Unit = {
    log.debug("Querying the DM API for a lookup on %s/%s/%s. ".format(entityType, attributeName, attributeValue))
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[EntitySearchResult]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.queryLookupUrl(entityType, attributeName, attributeValue))
    }
    responseFuture onComplete {
      case Success(queryResult) =>
        log.debug("Found entity matching %s/%s/%s: ID %s".format(entityType, attributeName, attributeValue, queryResult.guid))
        senderRef ! DMLookupResolved(queryResult)

      case Failure(error) =>
        log.error(error, "Couldn't find entity matching %s/%s/%s".format(entityType, attributeName, attributeValue))
        senderRef ! ClientFailure(error.getMessage)
    }
  }
}
