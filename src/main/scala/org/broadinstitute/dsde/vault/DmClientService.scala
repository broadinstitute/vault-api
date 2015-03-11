package org.broadinstitute.dsde.vault

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import org.broadinstitute.dsde.vault.model.{uBAMIngest, uBAM}
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

  def props(requestContext: RequestContext): Props = Props(new DmClientService(requestContext))
}

case class DmClientService(requestContext: RequestContext) extends Actor {

  import org.broadinstitute.dsde.vault.DmClientService._
  import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  implicit val system = context.system

  import system.dispatcher

  val log = Logging(system, getClass)

  override def receive: Receive = {
    case DMCreateUBam(ubam) =>
      createUBam(sender(), ubam)

    case DMResolveUBam(ubamId) =>
      resolveUBam(sender(), ubamId)
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

}
