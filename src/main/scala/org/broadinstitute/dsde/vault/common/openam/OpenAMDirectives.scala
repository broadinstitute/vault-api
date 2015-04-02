package org.broadinstitute.dsde.vault.common.openam

import org.slf4j.LoggerFactory
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

  // Internet says hit slf4j directly if outside an actor...
  // https://groups.google.com/forum/#!topic/akka-user/_bIiPKoGJXY
  lazy val logOpenAMRequestLogger = LoggerFactory.getLogger("org.broadinstitute.dsde.vault.common.openam.OpenAMRequest")

  // Partially based off DebuggingDirectives.logRequest, sans magnets, with requestInstance instead of mapRequest
  val logOpenAMRequest = commonNameFromCookie flatMap { commonName =>
    requestInstance flatMap { request =>
      // Quoting based off ELF string format: http://en.wikipedia.org/wiki/Extended_Log_Format
      val commonNameQuoted = """"%s"""".format(commonName.replaceAll("\"", "\"\""))
      val method = request.method.name
      val uri = request.uri
      var message = s"$commonNameQuoted $method $uri"
      if (logOpenAMRequestLogger.isDebugEnabled) {
        if (request.entity.nonEmpty) {
          message += "%n".format() + request.entity.asString
        }
      }
      logOpenAMRequestLogger.info(message)
      pass
    }
  }

  private def commonNameFromToken(token: String) = {
    val id = OpenAMClient.lookupIdFromSession(OpenAMConfig.deploymentUri, token)
    val usernameCN = OpenAMClient.lookupUsernameCN(OpenAMConfig.deploymentUri, token, id.id, id.realm)
    usernameCN.cn.head
  }
}
