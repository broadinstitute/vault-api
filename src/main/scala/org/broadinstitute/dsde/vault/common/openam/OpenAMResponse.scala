package org.broadinstitute.dsde.vault.common.openam

import spray.json.DefaultJsonProtocol

object OpenAMResponse extends DefaultJsonProtocol {

  case class AuthenticateResponse(tokenId: String, successUrl: String)

  case class IdFromSessionResponse(id: String, realm: Option[String], dn: String, successURL: String, fullLoginURL: String)

  /**
   * Response with the username and common names (CN) by using the token.
   */
  case class UsernameCNResponse(username: String, cn: Seq[String])

  implicit val impAuthenticateResponse = jsonFormat2(AuthenticateResponse)
  implicit val impIdFromSessionResponse = jsonFormat5(IdFromSessionResponse)
  implicit val impUserNameCNResponse = jsonFormat2(UsernameCNResponse)
}
