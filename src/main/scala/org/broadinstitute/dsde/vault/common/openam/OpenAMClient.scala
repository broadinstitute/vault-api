package org.broadinstitute.dsde.vault.common.openam

import akka.actor.ActorSystem
import org.broadinstitute.dsde.vault.common.openam.OpenAMResponse._
import spray.client.pipelining._
import spray.http.Uri
import spray.httpx.SprayJsonSupport._

import scala.concurrent.{Awaitable, Await}
import scala.concurrent.duration._

object OpenAMClient {
  implicit val system = ActorSystem()

  import system.dispatcher

  def authenticate(): AuthenticateResponse =
    authenticate(
      OpenAMConfig.deploymentUri,
      OpenAMConfig.username,
      OpenAMConfig.password,
      OpenAMConfig.realm,
      OpenAMConfig.authIndexType,
      OpenAMConfig.authIndexValue)

  def authenticate(deploymentUri: String, username: String, password: String, realm: Option[String],
                   authIndexType: Option[String], authIndexValue: Option[String]): AuthenticateResponse = {

    // Add optional information about how we are authenticating
    var queryValues = Seq.empty[(String, String)]
    authIndexType foreach { x => queryValues :+= ("authIndexType" -> x) }
    authIndexValue foreach { x => queryValues :+= ("authIndexValue" -> x) }

    val uri = Uri(s"$deploymentUri/json${realm.getOrElse("")}/authenticate").
      withQuery(queryValues: _*)
    val pipeline =
      addHeader("X-OpenAM-Username", username) ~>
        addHeader("X-OpenAM-Password", password) ~>
        sendReceive ~>
        unmarshal[AuthenticateResponse]
    waitResponse(pipeline(Post(uri)))
  }

  def lookupIdFromSession(deploymentUri: String, token: String) = {
    val uri = Uri(s"$deploymentUri/json/users").
      withQuery("_action" -> "idFromSession")
    val pipeline =
      addToken(token) ~>
        sendReceive ~>
        unmarshal[IdFromSessionResponse]
    // NOTE: Using the empty map to set the required json content-type header
    waitResponse(pipeline(Post(uri, Map.empty[String, String])))
  }

  /**
   * Retrieves the username and common names (CN) by using the token.
   */
  def lookupUsernameCN(deploymentUri: String, token: String, id: String, realm: Option[String]) = {
    val uri = Uri(s"$deploymentUri/json${realm.getOrElse("")}/users/$id").
      withQuery("_fields" -> "username,cn")
    val pipeline =
      addToken(token) ~>
        sendReceive ~>
        unmarshal[UsernameCNResponse]
    waitResponse(pipeline(Get(uri)))
  }

  private def addToken(token: String) =
    addHeader(OpenAMConfig.tokenCookie, token)

  private def waitResponse[Response](awaitable: Awaitable[Response]) =
    Await.result(awaitable, OpenAMConfig.timeoutSeconds.seconds)
}
