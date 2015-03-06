package org.broadinstitute.dsde.vault

import akka.actor.{Props, ActorRef, Actor}
import akka.event.Logging
import org.broadinstitute.dsde.vault.model.{BossCreationRequest, BossResolutionRequest, BossResolutionResponse, BossCreationObject}
import org.broadinstitute.dsde.vault.services.uBAM.ClientFailure
import spray.client.pipelining
import spray.client.pipelining._
import spray.http.HttpHeaders.{RawHeader, Cookie}
import spray.routing.RequestContext

import scala.util.{Failure, Success}

object BossClientService {
  case class BossCreateObject(obj: BossCreationRequest, creationKey: String)
  case class BossObjectCreated(bossObject: BossCreationObject, creationKey: String)
  case class BossResolveObject(obj: BossResolutionRequest, id: String, creationKey: String)
  case class BossObjectResolved(bossObject: BossResolutionResponse, creationKey: String)

  def props(requestContext: RequestContext): Props = Props(new BossClientService(requestContext))
}

case class BossClientService(requestContext: RequestContext) extends Actor {

  import org.broadinstitute.dsde.vault.BossClientService._
  import org.broadinstitute.dsde.vault.model.BossJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)

  override def receive: Receive = {
    case BossCreateObject(obj, creationKey) =>
      create(sender(), obj, creationKey)

    case BossResolveObject(obj, id, creationKey) =>
      resolve(sender(), obj, id, creationKey)
  }

  def initHeaders: pipelining.RequestTransformer = {
    addHeaders(
      Cookie(requestContext.request.cookies),
      RawHeader("REMOTE_USER", VaultConfig.BOSS.defaultUser)  // TODO: extract from cookie?
    )
  }

  def create(senderRef: ActorRef, obj: BossCreationRequest, creationKey: String): Unit = {
    log.info("Creating an object in the BOSS API")
    val pipeline = initHeaders ~> sendReceive ~> unmarshal[BossCreationObject]
    val responseFuture = pipeline {
      Post(VaultConfig.BOSS.objectsUrl, obj)
    }
    responseFuture onComplete {
      case Success(createdObject) =>
        log.info("BOSS object created with id: " + createdObject.objectId)
        senderRef ! BossObjectCreated(createdObject, creationKey)

      case Failure(error) =>
        log.error(error, "Failure creating BOSS object")
        senderRef ! ClientFailure(error.getMessage)
    }
  }

  def resolve(senderRef: ActorRef, obj: BossResolutionRequest, id: String, creationKey: String): Unit = {
    log.info("Resolving an object in the BOSS API with id: " + id)
    val pipeline = initHeaders ~> sendReceive ~> unmarshal[BossResolutionResponse]
    val responseFuture = pipeline {
      Post(VaultConfig.BOSS.objectResolveUrl(id), obj)
    }
    responseFuture onComplete {
      case Success(resolvedObject) =>
        log.info("BOSS object resolved with presigned URL: " + resolvedObject.objectUrl)
        senderRef ! BossObjectResolved(resolvedObject, creationKey)

      case Failure(error) =>
        log.error(error, "Failure resolving BOSS object")
        senderRef ! ClientFailure(error.getMessage)
    }
  }
}
