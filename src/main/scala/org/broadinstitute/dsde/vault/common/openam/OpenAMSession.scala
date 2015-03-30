package org.broadinstitute.dsde.vault.common.openam

import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie

object OpenAMSession extends Cookie(Seq(HttpCookie(OpenAMConfig.tokenCookie, OpenAMClient.authenticate().tokenId)))
