package org.broadinstitute.dsde.vault.services

import spray.http.MediaTypes._

trait VaultDirectives extends spray.routing.Directives {
  def respondWithJSON = respondWithMediaType(`application/json`)
  def forceLocationHeader = optionalHeaderValueByName("X-Force-Location")
}
