package org.broadinstitute.dsde.vault.services.generic

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService
import org.broadinstitute.dsde.vault.BossClientService.BossObjectResolved
import org.broadinstitute.dsde.vault.GenericDmClientService._
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.generic.DescribeServiceHandler.{DescribeDownMessage, DescribeUpMessage, DescribeMessage}
import spray.routing.RequestContext
import spray.json._

object DescribeServiceHandler {

  case class DescribeMessage(vaultId: String)
  case class DescribeUpMessage(vaultId: String)
  case class DescribeDownMessage(vaultId: String)

  def props(requestContext: RequestContext, version: Int, bossService: ActorRef, dmService: ActorRef): Props =
    Props(new DescribeServiceHandler(requestContext, version, bossService, dmService))
}

case class DescribeServiceHandler(requestContext: RequestContext, dmGenericDescribeVersion: Int, bossService: ActorRef, dmService: ActorRef) extends Actor {

  implicit val system = context.system
  val log = Logging(system, getClass)

  def multipleResolutionCheck(entities: List[GenericRelEnt], needsResolution: Map[String, GenericRelEnt]) = {
    if (needsResolution.isEmpty) {
      requestContext.complete(entities.toList.toJson.prettyPrint)
      context.stop(self)
    }
    else
      context become awaitMultipleBossResponse(entities, needsResolution)
  }

  // GENERIC DESCRIBE STATE MACHINE
  //
  //
  // Single Entity Describe / Find Entity
  //
  // INITIAL: receive DescribeMessage
  //    send DMGenericDescribe
  //    transition to awaitSingleDmResponse
  //
  // awaitSingleDmResponse: receive DMGenericDescribeResponse
  //    if requires BOSS
  //      send BossResolveObject
  //      transition to awaitSingleBossResponse
  //    else complete with entity
  //
  // awaitSingleBossResponse: receive BossObjectResolved
  //    complete with entity
  //
  //
  // Multiple Entity Describe / findUpstream/findDownstream
  //
  // INITIAL: receive DescribeUpMessage/DescribeDownMessage
  //    send DMGenericDescribeUp/DMGenericDescribeDown
  //    transition to awaitMultipleDmResponse
  //
  // awaitMultipleDmResponse: receive DMGenericDescribeMultipleResponse
  //    for each entity requiring BOSS
  //      send BossResolveObject
  //    if no BOSS resolution is necessary, complete with entities
  //
  // awaitMultipleBossResponse: receive BossObjectResolved
  //    if no further BOSS resolution is necessary, complete with entities
  //    else transition to awaitMultipleBossResponse with one fewer required resolution



  def receive = initialState

  def initialState: Actor.Receive = {
    case DescribeMessage(dmId: String) =>
      dmService ! DMGenericDescribe(dmId, dmGenericDescribeVersion, 0)
      context become awaitSingleDmResponse

    case DescribeUpMessage(dmId: String) =>
      dmService ! DMGenericDescribeUp(dmId, dmGenericDescribeVersion, 0)
      context become awaitMultipleDmResponse

    case DescribeDownMessage(dmId: String) =>
      dmService ! DMGenericDescribeDown(dmId, dmGenericDescribeVersion, 0)
      context become awaitMultipleDmResponse

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Describe INITIAL")
      requestContext.reject()
      context.stop(self)
  }

  def awaitSingleDmResponse: Actor.Receive = {
    case DMGenericDescribeResponse(entity: GenericEntity, describeKey: Int) =>
      log.debug("Received SINGLE DMGenericDescribeResponse message.  describeKey = " + describeKey)
      entity.sysAttrs.bossID match {
        case Some(id) =>
          bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("GET"), id, "")
          context become awaitSingleBossResponse(entity)

        case None =>
          requestContext.complete(entity.toJson.prettyPrint)
          context.stop(self)
      }

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Describe AWAIT SINGLE DM RESPONSE")
      requestContext.reject()
      context.stop(self)
  }

  def awaitSingleBossResponse(entity: GenericEntity): Actor.Receive = {
    case BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String) =>
      log.debug("Received SINGLE BossObjectResolved message.  creationKey = " + creationKey)
      val newEntity = entity.withGetUrl(bossObject.objectUrl)
      requestContext.complete(newEntity.toJson.prettyPrint)
      context.stop(self)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Describe AWAIT SINGLE BOSS RESPONSE")
      requestContext.reject()
      context.stop(self)
  }

  def awaitMultipleDmResponse: Actor.Receive = {
    case DMGenericDescribeMultipleResponse(entities: List[GenericRelEnt], describeKey: Int) =>
      log.debug("Received MULTIPLE DMGenericDescribeMultipleResponse message.  describeKey = " + describeKey)

      // no Boss ID = "resolved" already
      val (resolved, needsBossResolution) = entities partition { relEnt => relEnt.entity.sysAttrs.bossID.isEmpty }

      val indexedNeedsResponse = for {
        relEnt <- needsBossResolution
        id <- relEnt.entity.sysAttrs.bossID
      } yield {
        bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("GET"), id, id)
        (id, relEnt)
      }

      multipleResolutionCheck(resolved, indexedNeedsResponse.toMap)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Describe AWAIT MULTIPLE DM RESPONSE")
      requestContext.reject()
      context.stop(self)
  }

  def awaitMultipleBossResponse(entities: List[GenericRelEnt], needsBossResolution: Map[String, GenericRelEnt]): Actor.Receive = {
    case BossObjectResolved(bossObject: BossResolutionResponse, bossId: String) =>
      log.debug("Received MULTIPLE BossObjectResolved message.  bossId = " + bossId)

      // Doesn't handle the cases when bossId is not in needsBossResolution or when the resolved entity's bossId is None
      // These cases should never happen, but let's make a note of it

      val newEntity = needsBossResolution get bossId map { relEnt =>
        relEnt.copy(entity = relEnt.entity.withGetUrl(bossObject.objectUrl))
      }

      multipleResolutionCheck(entities ++ newEntity, needsBossResolution - bossId)

    case ClientFailure(message: String) =>
      log.error("Client Failure: " + message)
      requestContext.reject()
      context.stop(self)

    case _ =>
      log.error("Illegal State Exception: Generic Describe AWAIT MULTIPLE BOSS RESPONSE")
      requestContext.reject()
      context.stop(self)
  }

}

