package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.actor.Actor
import akka.event.Logging
import org.broadinstitute.dsde.vault.OpenAmClientService.{OpenAmAuthRequest, OpenAmResponse}
import org.broadinstitute.dsde.vault.services.ClientFailure
import spray.client.pipelining._
import spray.http.{HttpEntity, MediaTypes}
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

object OpenAmClientService {
  case class OpenAmAuthRequest(username: String, password: String)
  case class OpenAmResponse(tokenId: String, successUrl: String)
}

object OpenAmResponseJsonProtocol extends DefaultJsonProtocol {
  implicit val impOpenAmResponse = jsonFormat2(OpenAmResponse)
}

class OpenAmClientService extends Actor {

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)
  val duration: Duration = new FiniteDuration(5, TimeUnit.SECONDS)

  override def receive: Receive = {
    case OpenAmAuthRequest(username, password) =>
      log.debug("Querying the OpenAM Server for access token for username: " + username)
      sender ! {
        import org.broadinstitute.dsde.vault.OpenAmResponseJsonProtocol._
        import spray.httpx.SprayJsonSupport._
        try {
          val pipeline = sendReceive ~> unmarshal[OpenAmResponse]
          val responseFuture = pipeline {
            Post(VaultConfig.OpenAm.tokenUrl, HttpEntity(MediaTypes.`application/json`, """{}""")) ~>
              addHeader("X-OpenAM-Username", username) ~>
              addHeader("X-OpenAM-Password", password)
          }
          Await.result(responseFuture, duration)
        } catch {
          case e: Exception =>
            ClientFailure(e.toString)
        }
      }
    case unknown =>
      log.error("Unable to process this request: " + unknown.toString)
      ClientFailure(unknown.toString)
  }

}
