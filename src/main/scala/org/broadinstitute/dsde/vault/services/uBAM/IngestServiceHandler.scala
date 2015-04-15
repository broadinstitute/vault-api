package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.{Props, Actor, ActorRef}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService.{BossObjectResolved, BossObjectCreated, BossObjectDeleted}
import org.broadinstitute.dsde.vault.DmClientService.DMUBamCreated
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.common.BossObjectsCreationHandler._
import org.broadinstitute.dsde.vault.{DmClientService, BossClientService}
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.uBAM.IngestServiceHandler._
import scala.util.{Try,Success}
import spray.routing.RequestContext
import spray.json._
import uBAMJsonProtocol._

object IngestServiceHandler {
  case class IngestMessage(ingest: UBamIngest, forceLocation: Option[String] = None)

  def props(requestContext: RequestContext, bossService: ActorRef, dmService: ActorRef): Props =
    Props(new IngestServiceHandler(requestContext, bossService, dmService))
}

case class IngestServiceHandler(requestContext: RequestContext, bossService: ActorRef, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  // these first three vars are set from the IngestMessage

  // the metadata from the ingest message
  var metadata: Map[String,String] = Map.empty

  // the files from the ingest message
  var files: Map[String, String] = Map.empty

  // the value of the optional "X-Force-Location" header from the initial message
  // if true, no need to resolve Boss objects -- just pass back the original locations
  var forceLocation: Boolean = false

  // the Boss IDs for each file
  var ids: Map[String, String] = Map.empty

  // the resolved URLs for each file
  var urls: Map[String, String] = Map.empty

  def receive = {
    // Process the ingest message
    case IngestMessage(ingest: UBamIngest, forceLocationHdr: Option[String]) =>
      log.debug("Received uBAM ingest message")
      metadata = ingest.metadata
      files = ingest.files
      forceLocation = forceLocationHdr.getOrElse("false").toBoolean
      bossService ! CreateObjectsMessage(ingest.files,forceLocation)

    // Tell DM to do its thing
    case ObjectsCreatedMessage(objectIDs: Map[String,String], objectURLs: Map[String,String]) =>
      ids = objectIDs
      urls = objectURLs
      dmService ! DmClientService.DMCreateUBam(new UBamIngest(objectIDs, metadata))

    case ObjectsCreationFailedMessage(message: String) =>
      fail(message)

    // Success case: got the vault id from DM
    case DMUBamCreated(uBam: UBam) =>
      if (forceLocation) {
        log.debug("'X-Force-Location' uBAM ingest complete")
        requestContext.complete(UBamIngestResponse(uBam.id, files).toJson.prettyPrint)
      }
      else {
        log.debug("uBAM ingest complete")
        requestContext.complete(UBamIngestResponse(uBam.id, urls).toJson.prettyPrint)
      }
      context.stop(self)

    // Failure case: the DM work didn't go well
    case ClientFailure(message: String) =>
      bossService ! DeleteObjectsMessage(ids)
      fail(message)
  }

  def fail(message: String) = {
    log.error("Client failure: " + message)
    requestContext.reject()
    context.stop(self)
  }
}
