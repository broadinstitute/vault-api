package org.broadinstitute.dsde.vault.common.openam

import spray.routing.Directives._

object OpenAMDirectives {
  val tokenFromCookie = cookie(OpenAMConfig.tokenCookie) map {
    // return the content of the cookie
    _.content
  }

  val tokenFromOptionalCookie = optionalCookie(OpenAMConfig.tokenCookie) map {
    // when there is a cookie
    _ map {
      // return the content of the cookie
      _.content
    }
  }

  val commonNameFromCookie = tokenFromCookie map {
    // get the common name from the token
    case token => commonNameFromToken(token)
  }

  val commonNameFromOptionalCookie = tokenFromOptionalCookie map {
    // when there is a cookie
    _ map {
      // get the common name from the token
      case token => commonNameFromToken(token)
    }
  }

  private def commonNameFromToken(token: String) = {
    val id = OpenAMClient.lookupIdFromSession(OpenAMConfig.deploymentUri, token)
    val usernameCN = OpenAMClient.lookupUsernameCN(OpenAMConfig.deploymentUri, token, id.id, id.realm)
    usernameCN.cn.head
  }
}
