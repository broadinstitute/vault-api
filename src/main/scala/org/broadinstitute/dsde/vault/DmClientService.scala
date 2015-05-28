package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.DmClientService._
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
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

  case class DMLookupEntity(entityType: String, attributeName: String, attributeValue: String, version: Int)
  case class DMLookupResolved(result: EntitySearchResult)
  case class DMCreateUBamCollection(ubamCollectionIngest: UBamCollectionIngest, version: Int)
  case class DMUBamCollectionCreated(uBAMCollection: UBamCollection)
  case class DMResolveUBamCollection(uBamCollectionId: String, version: Int)
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

    case DMCreateUBamCollection(ubamCollectionIngest, version) =>
      createUBamCollection(sender(), ubamCollectionIngest, version)

    case DMResolveUBamCollection(uBamCollectionId, version) =>
      resolveUBamCollection(sender(),uBamCollectionId, version)

    case DMCreateAnalysis(analysisIngest) =>
      createAnalysis(sender(), analysisIngest)

    case DMResolveAnalysis(analysisId) =>
      resolveAnalysis(sender(), analysisId)

    case DMUpdateAnalysis(analysisId, update) =>
      updateAnalysis(sender(), analysisId, update)

    case DMLookupEntity(entityType, attributeName, attributeValue, version) =>
      lookup(sender(), entityType, attributeName, attributeValue, version)
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

  def createUBamCollection(senderRef: ActorRef, ubamCollectionIngest: UBamCollectionIngest, version: Int): Unit = {
    log.debug("Creating a uBAM Collection object in the DM")
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[UBamCollection]
    val responseFuture = pipeline {
      Post(VaultConfig.DataManagement.collectionsUrl(version), ubamCollectionIngest)
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

  def resolveUBamCollection(senderRef: ActorRef, uBamCollectionId: String, version: Int): Unit = {
    log.debug("Querying the DM API for an UbamCollection  id: " + uBamCollectionId)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[UBamCollection]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.uBamCollectionResolveUrl(uBamCollectionId, version))
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

  def lookup(senderRef: ActorRef, entityType: String, attributeName: String, attributeValue: String, version: Int): Unit = {
    log.debug("Querying the DM API for a lookup on %s/%s/%s. ".format(entityType, attributeName, attributeValue))

    /*
      The old Lookup Service in DM performed a mapping between the physical entity type we store in the DB (e.g.
      "unmappedBAM") and the endpoint name (e.g. "ubam"). The new generic service does not perform this mapping,
      and I don't think we want the mapping. For backwards compatibility, we therefore need to perform the
      translation here, inside this deprecated API, and leave the new DM generic service alone.
     */
    val legacyMappedEntityType = EntityType.TYPES.find(_.endpoint == entityType) match {
      case Some(endpointType) => endpointType.databaseKey
      case None => entityType
    }

    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[List[GenericEntity]]
    val responseFuture = pipeline {

      val spec = GenericAttributeSpec(attributeName, attributeValue)
      val entityQuery = GenericEntityQuery(legacyMappedEntityType, Seq(spec), false)

      Post(VaultConfig.DataManagement.queryLookupUrl(version), entityQuery)
    }
    responseFuture onComplete {
      case Success(queryResult) =>
        // reshape/parse response for compatibility
        val firstResultEntity = queryResult.headOption match {
          case Some(ent) => {
            // I don't like that this uses the type passed in by the user instead of the type from the result.
            // But, that's the way DM worked, and this API is deprecated, so it's not worth changing
            val result = EntitySearchResult(ent.guid, entityType)
            log.debug("Found entity matching %s/%s/%s: ID %s".format(entityType, attributeName, attributeValue, ent.guid))
            senderRef ! DMLookupResolved(result)
          }
          case None => {
            // query succeeded, but no results found
            val msg = "No entities found for %s/%s/%s".format(entityType, attributeName, attributeValue)
            log.debug(msg)
            senderRef ! ClientFailure(msg)
          }
        }
      case Failure(error) =>
        log.error(error, "Failure finding entity matching %s/%s/%s".format(entityType, attributeName, attributeValue))
        senderRef ! ClientFailure(error.getMessage)
    }
  }
}

case class EntityType(databaseKey: String, endpoint: String)

object EntityType {
  val UNMAPPED_BAM = EntityType("unmappedBAM", "ubam")
  val ANALYSIS = EntityType("analysis", "analyses")
  val UBAM_COLLECTION = EntityType("uBAMCollection", "ubamcollection")
  val TYPES = Seq(UNMAPPED_BAM, ANALYSIS, UBAM_COLLECTION)
}
