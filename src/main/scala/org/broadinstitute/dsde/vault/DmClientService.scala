package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.DmClientService._
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.{Analysis, AnalysisIngest, EntitySearchResult, UBam, UBamIngest, _}
import org.broadinstitute.dsde.vault.services.ClientFailure
import spray.client.pipelining._
import spray.http.HttpHeaders.Cookie
import spray.http.Uri
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing.RequestContext

import scala.util.{Failure, Success}


object DmClientService {
  case class DMCreateUBam(ubam: UBamIngest)
  case class DMUBamCreated(createdUBam: UBam)
  case class DMResolveUBam(ubamId: String)
  case class DMUBamResolved(dmObject: UBam)
  case class DMResolveUBamList(version: Int, pageLimit: Option[Int])
  case class DMCreateAnalysis(analysisIngest: AnalysisIngest)
  case class DMAnalysisCreated(analysis: Analysis)
  case class DMResolveAnalysis(analysisId: String)
  case class DMAnalysisResolved(analysis: Analysis)
  case class DMUpdateAnalysis(analysisId: String, update: AnalysisUpdate)
  case class DMAnalysisUpdated(analysis: Analysis)

  case class DMLookupEntity(entityType: String, attributeName: String, attributeValue: String)
  case class DMLookupResolved(result: EntitySearchResult)
  case class DMCreateUBamCollection(ubamCollectionIngest: UBamCollectionIngest)
  case class DMUBamCollectionCreated(uBAMCollection: UBamCollection)
  case class DMResolveUBamCollection(uBamCollectionId: String)
  case class DMUBamCollectionResolved(uBamCollection: UBamCollection)


  def props(requestContext: RequestContext): Props = Props(new DmClientService(requestContext))
}

case class DmClientService(requestContext: RequestContext) extends Actor{

  import system.dispatcher
  import DefaultJsonProtocol._

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  implicit val system = context.system
  val log = Logging(system, getClass)

  override def receive: Receive = {
    case DMCreateUBam(ubam) =>
      createUBam(sender(), ubam)

    case DMResolveUBam(ubamId) =>
      resolveUBam(sender(), ubamId)

    case DMResolveUBamList(version: Int, pageLimit: Option[Int]) =>
      resolveUBamList(sender(), version, pageLimit)

    case DMCreateUBamCollection(ubamCollectionIngest) =>
      createUBamCollection(sender(), ubamCollectionIngest)

    case DMResolveUBamCollection(uBamCollectionId) =>
      resolveUBamCollection(sender(),uBamCollectionId)

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

  def resolveUBamList(senderRef: ActorRef, version: Int, pageLimit: Option[Int]): Unit = {
    log.debug("Querying the DM API for a uBAM List")
    val pipeline = {
      addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[List[UBam]]
    }
    val responseFuture = pipeline {
      var uri = Uri(VaultConfig.DataManagement.ubamsUrl + "/v" + version)
      pageLimit.foreach(limit => uri = uri.withQuery("page[limit]" -> limit.toString))
      Get(uri)
    }
    responseFuture onComplete {

      case Success(resolvedUBams) =>
        requestContext.complete(resolvedUBams)

      case Failure(error) =>
        log.error(error, "Couldn't find uBAMs")
    }
  }

  def createUBamCollection(senderRef: ActorRef, ubamCollectionIngest: UBamCollectionIngest): Unit = {
    log.debug("Creating a uBAM Collection object in the DM")
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[UBamCollection]
    val responseFuture = pipeline {
      Post(VaultConfig.DataManagement.collectionsUrl, ubamCollectionIngest)
    }
    responseFuture onComplete {
      case Success(createdUBamCollection) =>
        log.debug("uBAM Collection created with id: " + createdUBamCollection.id)
        senderRef ! DMUBamCollectionCreated(createdUBamCollection)

      case Failure(error) =>
        log.error(error, "Failure creating uBAM Collection object")
        senderRef ! ClientFailure(error.getMessage)
    }
  }

  def resolveUBamCollection(senderRef: ActorRef, uBamCollectionId: String): Unit = {
    log.debug("Querying the DM API for an UbamCollection  id: " + uBamCollectionId)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[UBamCollection]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.uBamCollectionResolveUrl(uBamCollectionId))
    }
    responseFuture onComplete {
      case Success(uBamCollection) =>
        log.debug("uBamCollection found: " + uBamCollection.id)
        senderRef ! DMUBamCollectionResolved(uBamCollection)
      case Failure(error) =>
        log.error(error, "Couldn't find uBamCollection with id: " + uBamCollectionId)
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

  // We explicitly ignore any metadata passed in from the client.
  def updateAnalysis(senderRef: ActorRef, analysisId: String, update: AnalysisUpdate): Unit = {
    log.debug("Updating an Analysis through the DM API for Analysis id: " + analysisId)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[Analysis]
    val analysisDmUpdate = new AnalysisDMUpdate(update.files, Map.empty[String,String])
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
