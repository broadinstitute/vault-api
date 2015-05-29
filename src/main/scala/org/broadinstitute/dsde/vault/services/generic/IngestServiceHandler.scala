package org.broadinstitute.dsde.vault.services.generic

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService
import org.broadinstitute.dsde.vault.BossClientService.{BossObjectCreated, BossObjectResolved}
import org.broadinstitute.dsde.vault.GenericDmClientService.{DMGenericDescribeResponse, DMGenericDescribe, DMGenericIngestResponse, DMGenericIngest}
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.generic.IngestServiceHandler.IngestMessage
import spray.routing.RequestContext
import spray.json._
import scala.collection.immutable.SortedMap

object IngestServiceHandler {

  case class IngestMessage(ingest: GenericIngest)

  def props(requestContext: RequestContext, version: Int, bossService: ActorRef, dmService: ActorRef): Props =
    Props(new IngestServiceHandler(requestContext, version, bossService, dmService))
}

case class IngestServiceHandler(requestContext: RequestContext, dmGenericIngestVersion: Int, bossService: ActorRef, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def registerInBoss(objSpec: DataObjectSpecification, index: Int): Unit = {
    objSpec.ingestMethod match {
      case "new" =>
        val filePath = "nonsense_generic_vault_path"   // TODO: better solution for this
        val noForceLocation = Some("false")
        bossService ! BossClientService.BossCreateObject(BossDefaults.getCreationRequest(filePath, noForceLocation), index.toString)

      case other =>
        log.error("Ingest method %s not implemented".format(other))
        requestContext.reject()
        context.stop(self)
    }
  }

  def bossRegistrationCheck(ingest: GenericIngest, entities: List[GenericEntityIngest], relations: List[GenericRelationshipIngest], needsRegistration: Set[Int]) = {
    if (needsRegistration.isEmpty) {
      dmService ! DMGenericIngest(ingest, dmGenericIngestVersion)
      context become dmRegistrationState(List())
    }
    else
      context become bossRegistrationState(entities, relations, needsRegistration)
  }

  def dmResolutionCheck(entities: SortedMap[Int, GenericEntity], needsDmResolution: Set[Int]): Unit = {
    if (needsDmResolution.isEmpty) {
      var bossEntityIndex: Int = 0
      var needsBossResolution: Set[Int] = Set()

      entities foreach { case (index, ent) =>
        ent.sysAttrs.bossID foreach { id =>
          bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("PUT"), id, index.toString)
          needsBossResolution += index
        }

        bossEntityIndex += 1
      }

      bossResolutionCheck(entities, needsBossResolution)
    }
    else
      context become dmResolutionState(entities, needsDmResolution)
  }

  def bossResolutionCheck(entities: SortedMap[Int, GenericEntity], needsResolution: Set[Int]) = {
    if (needsResolution.isEmpty) {
      requestContext.complete(entities.values.toList.toJson.prettyPrint)
      context.stop(self)
    }
    else
      context become bossResolutionState(entities, needsResolution)
  }

  // Generic Ingest State Machine
  //
  // INITIAL
  //    only valid incoming message is IngestMessage
  //    parse incoming entities for BOSS requirements.  If registration needed:
  //      send BossCreateObject for those which need it
  //      add to needsRegistration
  //    if needsRegistration is not empty, transition to BOSS_REGISTRATION(newEntities, relations, newNeedsRegistration)
  //    if empty, send DMGenericIngest and transition to DM_REGISTRATION
  //
  // BOSS_REGISTRATION(entities, relations, needsRegistration)
  //    only valid incoming message is BossObjectCreated
  //    update entities at the appropriate index with the bossID
  //    remove the appropriate entity from needsRegistration
  //    if needsRegistration is not empty, transition to BOSS_REGISTRATION(newEntities, relations, newNeedsRegistration)
  //    if empty, send DMGenericIngest and transition to DM_REGISTRATION
  //
  // DM_REGISTRATION
  //    only valid incoming message is DMGenericIngestResponse
  //    send DMGenericDescribe for each guid
  //    transition to DM_RESOLUTION((), needsResolution, ())
  //
  // DM_RESOLUTION(entities, needsDmResolution, needsBossResolution)
  //    only valid incoming message is DMGenericDescribeResponse
  //    add incoming entity to entities and remove from needsDmResolution
  //    parse incoming entities for BOSS IDs.  If found:
  //      remove BOSS ID
  //      add to needsBossResolution
  //    if needsDmResolution is not empty, transition to DM_RESOLUTION(newEntities, newNeedsDmResolution, needsBossResolution)
  //    if empty, send BossResolveObject for all in needsBossResolution and transition to BOSS_RESOLUTION(entities, needsBossResolution)
  //
  // BOSS_RESOLUTION(entities, needsResolution)
  //    only valid incoming message is BossObjectResolved
  //    update entities at the appropriate index with the genericPutUrl
  //    remove the appropriate entity from needsResolution
  //    if newNeedsResolution is not empty, transition to BOSS_RESOLUTION(newEntities, newNeedsResolution)
  //    if empty, complete client request with newEntities

  // TODO: allow BOSS resolution in parallel with DM registration/resolution

  def receive = initialState

  def initialState: Actor.Receive = {
    case IngestMessage(ingest) =>
      log.debug("Received Generic Ingest message")

      var entityIndex: Int = 0
      var needsRegistration: Set[Int] = Set()

      // double foreach here because it's Option[List[GenericEntityIngest]]
      ingest.entities foreach { l => l foreach { ent =>
        ent.dataObject foreach { objectSpec =>
          needsRegistration += entityIndex
          registerInBoss(objectSpec, entityIndex)
        }

        entityIndex += 1
      }}

      bossRegistrationCheck(ingest, ingest.entities getOrElse List(), ingest.relations getOrElse List(), needsRegistration)
 
    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Ingest INITIAL")
      requestContext.reject()
      context.stop(self)
  }

  def bossRegistrationState(entities: List[GenericEntityIngest], relations: List[GenericRelationshipIngest], needsRegistration: Set[Int]): Actor.Receive = {
    case BossObjectCreated(bossObject: BossCreationObject, creationKey: String) =>
      log.debug("Received BossObjectCreated message.  Current state: %s entities (%s need registration) and %s relations".format(entities.size, needsRegistration.size, relations.size))
      val entityIndex = creationKey.toInt
      val newNeedsRegistration = needsRegistration - entityIndex
      val newEntities = entities.updated(entityIndex, entities(entityIndex).copy(bossID = bossObject.objectId))

      bossRegistrationCheck(GenericIngest(Option(newEntities), Option(relations)), newEntities, relations, newNeedsRegistration)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Ingest BOSS REGISTRATION")
      requestContext.reject()
      context.stop(self)
  }

  def dmRegistrationState(entities: List[GenericEntity]): Actor.Receive = {
    case DMGenericIngestResponse(guids: List[String]) =>
      log.debug("Received DMGenericIngestResponse message.  Current state: %s entities".format(entities.size))
      var entityIndex: Int = 0
      var needsResolution: Set[Int] = Set()

      guids foreach { id =>
        needsResolution += entityIndex
        dmService ! DMGenericDescribe(id, dmGenericIngestVersion, entityIndex)

        entityIndex += 1
      }

      context become dmResolutionState(SortedMap(), needsResolution)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Ingest DM REGISTRATION")
      requestContext.reject()
      context.stop(self)
  }

  def dmResolutionState(entities: SortedMap[Int, GenericEntity], needsResolution: Set[Int]): Actor.Receive = {
    case DMGenericDescribeResponse(entity: GenericEntity, entityIndex: Int) =>
      log.debug("Received DMGenericDescribeResponse message.  Current state: %s entities (%s need resolution)".format(entities.size, needsResolution.size))
      val newNeedsResolution = needsResolution - entityIndex
      val newEntities = entities + (entityIndex -> entity)
      
      dmResolutionCheck(newEntities, newNeedsResolution)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Ingest DM REGISTRATION")
      requestContext.reject()
      context.stop(self)
  }

  def bossResolutionState(entities: SortedMap[Int, GenericEntity], needsResolution: Set[Int]): Actor.Receive = {
    case BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String) =>
      log.debug("Received BossObjectResolved message.  Current state: %s entities (%s need resolution)".format(entities.size, needsResolution.size))
      val entityIndex = creationKey.toInt
      val newNeedsResolution = needsResolution - entityIndex

      // set signedPutUrl and clear sysAttrs.bossID
      val oldEntity = entities(entityIndex)
      val newEntity = oldEntity.copy(signedPutUrl = Option(bossObject.objectUrl), sysAttrs = oldEntity.sysAttrs.copy(bossID = None))
      val newEntities = entities + (entityIndex -> newEntity)

      bossResolutionCheck(newEntities, newNeedsResolution)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Ingest BOSS RESOLUTION")
      requestContext.reject()
      context.stop(self)
  }
}
