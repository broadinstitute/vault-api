package org.broadinstitute.dsde.vault.mock

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.BossCreationObject
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.scalatest.BeforeAndAfterEach
import org.broadinstitute.dsde.vault.model.BossJsonProtocol._
import spray.json._

trait MockBossServer extends VaultFreeSpec with BeforeAndAfterEach {

  var mockBossServer: ClientAndServer = _

  val list = List("one","two")
  val bossObject = new BossCreationObject(
   "bossSample", "localhost", 24, "localOwner", list, list, Some("testing_id"), Some("34"), Some(true)
  )

  override def beforeEach(): Unit = {
    mockBossServer = startClientAndServer(8988)

    mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/")
    ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(
            new Header("Content-Type", "application/json")
          )
          .withBody(bossObject.toJson.prettyPrint)
          .withStatusCode(200)
      )
  }

  override def afterEach(): Unit = {
    mockBossServer.stop()
  }

}


