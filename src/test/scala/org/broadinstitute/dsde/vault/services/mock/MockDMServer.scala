package org.broadinstitute.dsde.vault.mock

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.UBam
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.scalatest.BeforeAndAfterEach
import spray.json._

trait MockDMServer extends VaultFreeSpec with BeforeAndAfterEach {

  var mockDMServer: ClientAndServer = _
  val ubam = new UBam(
    "testing_id",
    Map(("bam", "http://localhost:8080/path/to/bam"), ("bai", "http://localhost:8080/path/to/bai")),
    Map("testAttr" -> "testValue")
  )

  override def beforeEach(): Unit = {
    mockDMServer = startClientAndServer(8989)

    mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ubams/testing_id")
      ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(
            new Header("Content-Type", "application/json")
          )
          .withBody(ubam.toJson.prettyPrint)
          .withStatusCode(200)
      )

    mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ubams/12345-67890-12345")
      ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(
            new Header("Content-Type", "application/json")
          )
          .withStatusCode(404)
      )

    mockDMServer.when(
      request()
        .withMethod("POST")
        .withPath("/ubams")
    ).respond(
        org.mockserver.model.HttpResponse.response()
          .withBody("HTTP method not allowed, supported methods: GET")
          .withStatusCode(405)
      )

    mockDMServer.when(
      request()
        .withMethod("PUT")
        .withPath("/ubams")
    ).respond(
        org.mockserver.model.HttpResponse.response()
          .withBody("HTTP method not allowed, supported methods: GET")
          .withStatusCode(405)
      )

  }

  override def afterEach(): Unit = {
    mockDMServer.stop()
  }

}

