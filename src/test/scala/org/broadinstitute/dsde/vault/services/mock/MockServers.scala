package org.broadinstitute.dsde.vault.services.mock

import org.broadinstitute.dsde.vault.VaultFreeSpec
import org.broadinstitute.dsde.vault.model.BossJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamIngestResponse, BossCreationObject, BossResolutionResponse, UBam}
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer.startClientAndServer
import org.mockserver.model.Header
import org.mockserver.model.HttpRequest.request
import org.scalatest.BeforeAndAfterAll
import spray.json._

trait MockServers extends VaultFreeSpec with BeforeAndAfterAll {

  var mockDMServer: ClientAndServer = _
  var mockBossServer: ClientAndServer = _
  val header = new Header("Content-Type", "application/json")

  override def beforeAll(): Unit = {
    startUpDM()
    startUpBoss()
  }

  override def afterAll(): Unit = {
    mockDMServer.stop()
    mockBossServer.stop()
  }

  def startUpDM(): Unit = {
    val ubam = new UBam(
      "testing_id",
      Map(("bam", "http://localhost:8080/path/to/bam"), ("bai", "http://localhost:8080/path/to/bai")),
      Map("testAttr" -> "testValue")
    )

    val ubamResponse = new UBamIngestResponse(
      "testing_id",
      Map(("bam", "http://localhost:8080/path/to/bam"), ("bai", "http://localhost:8080/path/to/bai"))
    )

    mockDMServer = startClientAndServer(8989)
    mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ubams/testing_id")
      ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(header)
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
          .withHeaders(header)
          .withStatusCode(404)
      )

    mockDMServer.when(
      request()
        .withMethod("POST")
        .withPath("/ubams")
    ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(header)
          .withBody(ubam.toJson.prettyPrint)
          .withStatusCode(200)
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

  def startUpBoss(): Unit = {

    val list = List("one","two")
    val bossObject = new BossCreationObject(
      "bossSample", "localhost", 24, "localOwner", list, list, Some("testing_id"), Some("34"), Some(true)
    )

    val bossResponse = new BossResolutionResponse(
      10, Option("application/json"), Option("md5string"), "http://localhost:8988/objects/testing_id/resolve"
    )

    mockBossServer = startClientAndServer(8988)

    mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/")
    ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(header)
          .withBody(bossObject.toJson.prettyPrint)
          .withStatusCode(200)
      )

    mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/testing_id/resolve")
    ).respond(
        org.mockserver.model.HttpResponse.response()
          .withHeaders(header)
          .withBody(bossResponse.toJson.prettyPrint)
          .withStatusCode(200)
      )

  }


}
