package org.broadinstitute.dsde.vault

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService._
import org.broadinstitute.dsde.vault.model.BossJsonProtocol._
import org.broadinstitute.dsde.vault.model.{BossCreationObject, BossResolutionRequest, BossResolutionResponse}
import org.broadinstitute.dsde.vault.services.ClientFailure
import spray.client.pipelining._
import spray.http.BasicHttpCredentials
import spray.http.HttpHeaders.Cookie
import spray.httpx.SprayJsonSupport._
import spray.routing.RequestContext

import scala.util.{Failure, Success, Try}

object BossClientService {
  case class BossCreateObject(obj: BossCreationObject, creationKey: String)
  case class BossObjectCreated(bossId: Try[String], creationKey: String)
  case class BossResolveObject(obj: BossResolutionRequest, id: String, creationKey: String)
  case class BossObjectResolved(bossURL: Try[String], creationKey: String)
  case class BossDeleteObject(bossId: String, creationKey: String)
  case class BossObjectDeleted(bossId: Try[String], creationKey: String)

  def props(requestContext: RequestContext): Props = Props(new BossClientService(requestContext))
}

case class BossClientService(requestContext: RequestContext) extends Actor {

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)
  val bossCredentials = BasicHttpCredentials(VaultConfig.BOSS.bossUser, VaultConfig.BOSS.bossUserPassword)

  override def receive: Receive = {
    case BossCreateObject(obj, creationKey) =>
      create(sender(), obj, creationKey)

    case BossResolveObject(obj, id, creationKey) =>
      resolve(sender(), obj, id, creationKey)

    case BossDeleteObject(id, creationKey) =>
      delete(sender(), id, creationKey)
  }

  def create(senderRef: ActorRef, obj: BossCreationObject, creationKey: String): Unit = {
    log.debug("Creating an object in the BOSS API")
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[BossCreationObject]
    val responseFuture = pipeline {
      Post(VaultConfig.BOSS.objectsUrl, obj) ~> addCredentials(bossCredentials)
    }
    responseFuture onComplete {
      case Success(createdObject) =>
        val id = createdObject.objectId.get
        log.debug("BOSS object created with id: " + id)
        senderRef ! BossObjectCreated(Success(id), creationKey)

      case Failure(error) =>
        log.error(error, "Failure creating BOSS object")
        senderRef ! BossObjectCreated(Failure(error), creationKey)
    }
  }

  def resolve(senderRef: ActorRef, obj: BossResolutionRequest, id: String, creationKey: String): Unit = {
    log.debug("Resolving an object in the BOSS API with id: " + id)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[BossResolutionResponse]
    val responseFuture = pipeline {
      Post(VaultConfig.BOSS.objectResolveUrl(id), obj) ~> addCredentials(bossCredentials)
    }
    responseFuture onComplete {
      case Success(resolvedObject) =>
        val bossURL = resolvedObject.objectUrl
        log.debug("BOSS object resolved with presigned URL: " + bossURL)
        senderRef ! BossObjectResolved(Success(bossURL), creationKey)

      case Failure(error) =>
        log.error(error, "Failure resolving BOSS object")
        senderRef ! BossObjectResolved(Failure(error), creationKey)
    }
  }

  def delete(senderRef: ActorRef, id: String, creationKey: String): Unit = {
    log.debug("Deleting an object in the BOSS API with id: " + id)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive
    val responseFuture = pipeline {
      Delete(VaultConfig.BOSS.objectResolveUrl(id)) ~> addCredentials(bossCredentials)
    }
    responseFuture onComplete {
      case Success(createdObject) =>
        log.debug("BOSS object deleted with id: " + id)
        senderRef ! BossObjectDeleted(Success(id), creationKey)

      case Failure(error) =>
        log.error(error, "Failure deleting BOSS object")
        senderRef ! BossObjectDeleted(Failure(error), creationKey)
    }
  }
}
