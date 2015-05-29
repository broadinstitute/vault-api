package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import akka.util.Timeout
import org.broadinstitute.dsde.vault.GenericDmClientService._
import org.broadinstitute.dsde.vault.model.{GenericIngest, GenericEntity}
import org.broadinstitute.dsde.vault.services.ClientFailure
import spray.client.pipelining._
import spray.http.HttpHeaders.Cookie
import spray.routing.RequestContext
import spray.httpx.SprayJsonSupport._
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._

import scala.util.{Failure, Success}

object GenericDmClientService {
  case class DMGenericIngest(ingest: GenericIngest, version: Int)
  case class DMGenericIngestResponse(guids: List[String])

  case class DMGenericDescribe(guid: String, version: Int, describeKey: Int)
  case class DMGenericDescribeResponse(entity: GenericEntity, describeKey: Int)

  def props(requestContext: RequestContext): Props = Props(new GenericDmClientService(requestContext))
}

case class GenericDmClientService(requestContext: RequestContext) extends Actor{

  import system.dispatcher

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  implicit val system = context.system
  val log = Logging(system, getClass)

  override def receive: Receive = {
    case DMGenericIngest(ingest, version) =>
      genericIngest(sender(), ingest, version)

    case DMGenericDescribe(guid, version, key) =>
      genericDescribe(sender(), guid, version, key)
  }

 def genericIngest(senderRef: ActorRef, ingest: GenericIngest, version: Int): Unit = {
   log.debug("DM Generic Ingest")
   val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[List[String]]
   val responseFuture = pipeline {
     Post(VaultConfig.DataManagement.genericIngestUrl(version), ingest)
   }
   responseFuture onComplete {
     case Success(guids) =>
       log.debug("DM Generic Ingest successful")
       senderRef ! DMGenericIngestResponse(guids)

     case Failure(error) =>
       log.error(error, "DM Generic Ingest Failure")
       senderRef ! ClientFailure(error.getMessage)
   }
 }

  def genericDescribe(senderRef: ActorRef, guid: String, version: Int, describeKey: Int): Unit = {
    log.debug("DM Generic Describe for " + guid)
    val pipeline = addHeader(Cookie(requestContext.request.cookies)) ~> sendReceive ~> unmarshal[GenericEntity]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.genericDescribeUrl(version, guid))
    }
    responseFuture onComplete {
      case Success(entity) =>
        log.debug("DM Generic Describe for %s Successful".format(guid))
        senderRef ! DMGenericDescribeResponse(entity, describeKey)

      case Failure(error) =>
        log.error(error, "DM Generic Describe for %s Failed".format(guid))
        senderRef ! ClientFailure(error.getMessage)
    }
  }

}
