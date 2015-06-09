package org.broadinstitute.dsde.vault.services.mock

import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model.BossJsonProtocol._
import org.broadinstitute.dsde.vault.model.LookupJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.mockserver.integration.ClientAndServer
import org.mockserver.integration.ClientAndServer._
import org.mockserver.model.Header
import org.mockserver.model.HttpCallback._
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse._
import spray.json._

object Servers{

  val header = new Header("Content-Type", "application/json")
  var mockDMServer: ClientAndServer = _
  var mockBossServer: ClientAndServer = _

  if(mockDMServer == null){
    mockDMServer = startClientAndServer(8989)
  }

  if(mockBossServer == null){
    mockBossServer = startClientAndServer(8988)
  }

  def stopServers(): Unit = {
    mockDMServer.stop()
    mockBossServer.stop()
  }

  def startUpDM(): Unit = {

    /* ----------------- Generic API ----------------------*/

    val lookupResponse = new EntitySearchResult(
      "testing_id",
      "ubam"
    )

    Servers.mockDMServer.when(
      request()
        .withMethod("GET")
        .withPath("/entities/search/ubam")
    ).respond(
        response()
          .withHeader(header)
          .withBody(lookupResponse.toJson.prettyPrint)
          .withStatusCode(200)
      )


    /* ----------------- UBams ----------------------*/
    val ubam = new UBam(
      "testing_id",
      Map(("bam", "http://localhost:8080/path/to/bam"), ("bai", "pathToBai")),
      Map("testAttr" -> "testValue")
    )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ubams/testing_id")
      ).respond(
        response()
          .withHeaders(header)
          .withBody(ubam.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ubams/12345-67890-12345")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(404)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/ubams/v*/12345-67890-12345")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(404)
      )

    Servers.mockDMServer.when(
      request()
        .withMethod("POST")
        .withPath("/ubams")
    ).respond(
        response()
          .withHeaders(header)
          .withBody(ubam.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer.when(
      request()
        .withMethod("GET")
        .withPath("/ubams/v1*1")
    ).callback(
        callback()
          .withCallbackClass("org.broadinstitute.dsde.vault.services.mock.UbamListCallback")
      )

    Servers.mockDMServer.when(
      request()
        .withMethod("PUT")
        .withPath("/ubams")
    ).respond(
        response()
          .withBody("HTTP method not allowed, supported methods: GET")
          .withStatusCode(405)
      )

    /* ----------- UBam Collections ----------------- */
    val members = Some(List("testing_id"))

    val ubamCollectionResponse = new UBamCollectionIngestResponse(
      "9b66665c-f41a-11e4-b9b2-1697f925ec7",
      members,
      Map("testAttr" -> "testValue", "randomData" -> "7"),
      Map("testAttr" -> "testValue", "randomData" -> "7")
    )

    Servers.mockDMServer.when(
      request()
        .withMethod("POST")
        .withPath("/ubamcollections/v1")
    ).respond(
        response()
          .withHeader(header)
          .withBody(ubamCollectionResponse.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer.when(
      request()
        .withMethod("GET")
        .withPath("/ubamcollections/v1/9b66665c-f41a-11e4-b9b2-1697f925ec7")
    ).respond(
        response()
          .withHeader(header)
          .withBody(ubamCollectionResponse.toJson.prettyPrint)
          .withStatusCode(200)
      )

    /* ----------------- Analyses ----------------------*/
    val metadataMap = Map("testAttr" -> "testValue", "randomData" -> "7")
    val analysisIngest = new AnalysisIngest(List(), Map("testAttr" -> "testValue"))

    val badAnalysisIngest = new AnalysisIngest(
      input = List("testing_id", "intentionallyBadForeignKey"),
      metadata = metadataMap
    )
    val badAnalysisIngest2 = new AnalysisIngest(
      input = List("testing_id", "testing_id", "testing_id", "intentionallyBadForeignKey"),
      metadata = metadataMap
    )

    val responseAnalysis = new Analysis (
      "0a3b5d30-f41b-11e4-b9b2-1697f925ec7b",
      input = List(),
      files = Some(Map("vcf" -> "vault/test/test.vcf", "bai" -> "pathToBai", "bam" -> "pathToBam")),
      metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
    )

    val analysisTesting_id = new Analysis (
      "testing_id",
      input = List(),
      metadata = metadataMap,
      files = Some(Map("vcf" -> "vault/test/test.vcf", "bai" -> "pathToBai", "bam" -> "pathToBam"))
    )

    Servers.mockDMServer.when(
      request()
        .withMethod("POST")
        .withPath("/analyses")
        .withBody(analysisIngest.toJson.prettyPrint)
    ).respond(
        response()
          .withHeader(header)
          .withBody(responseAnalysis.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/analyses/*/0a3b5d30-f41b-11e4-b9b2-1697f925ec7b")
      ).respond(
        response()
          .withHeader(header)
          .withBody(responseAnalysis.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/*/0a3b5d30-f41b-11e4-b9b2-1697f925ec7b/outputs")
      ).respond(
        response()
          .withHeader(header)
          .withBody(responseAnalysis.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses")
          .withBody(badAnalysisIngest.toJson.prettyPrint)
      ).respond(
        response()
          .withHeader(header)
          .withBody(AnalysesResponseContainer.emptyAnalysis.toJson.prettyPrint)
          .withStatusCode(404)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses")
          .withBody(badAnalysisIngest2.toJson.prettyPrint)
      ).respond(
        response()
          .withHeader(header)
          .withBody(AnalysesResponseContainer.emptyAnalysis.toJson.prettyPrint)
          .withStatusCode(404)
      )

    Servers.mockDMServer.when(
      request()
        .withMethod("POST")
        .withPath("/analyses")
    ).callback(
        callback()
          .withCallbackClass("org.broadinstitute.dsde.vault.services.mock.AnalysisUpdateSwitchCallback")
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("GET")
          .withPath("/analyses/*/9b66665c-f41a-11e4-b9b2-1697f925ec7b")
      ).callback(
        callback()
          .withCallbackClass("org.broadinstitute.dsde.vault.services.mock.AnalysisUpdateSwitchCallback")
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/9b66665c-f41a-11e4-b9b2-1697f925ec7b/outputs")
      ).callback(
        callback()
          .withCallbackClass("org.broadinstitute.dsde.vault.services.mock.AnalysisUpdateSwitchCallback")
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/unknown-not-found-id")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(404)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/0a3b5d30-f41b-11e4-b9b2-1697f925ec7b/outputs")
      ).respond(
        response()
          .withHeader(header)
          .withBody(AnalysesResponseContainer.emptyAnalysis.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/testing_id/outputs")
      ).respond(
        response()
          .withHeader(header)
          .withBody(analysisTesting_id.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/1234-1234/outputs")
      ).respond(
        response()
          .withHeader(header)
          .withBody(analysisTesting_id.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("PUT")
          .withPath("/analyses/9b66665c-f41a-11e4-b9b2-1697f925ec7b")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(405)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("PUT")
          .withPath("/analyses/0a3b5d30-f41b-11e4-b9b2-1697f925ec7b")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(405)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/9b66665c-f41a-11e4-b9b2-1697f925ec7b")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(405)
      )

    Servers.mockDMServer
      .when(
        request()
          .withMethod("POST")
          .withPath("/analyses/0a3b5d30-f41b-11e4-b9b2-1697f925ec7b")
      ).respond(
        response()
          .withHeaders(header)
          .withStatusCode(405)
      )
  }

  def startUpBoss(): Unit = {

    val list = List("one","two")
    val bossObject = new BossCreationObject(
      "bossSample", "localhost", 24, "localOwner", list, list, Some("testing_id"), Some("34"), Some(true)
    )

    val bossResponseBam = new BossResolutionResponse(
      10, Option("application/json"), Option("md5string"), "vault/test/test.bam?GoogleAccessId="
    )

    val bossResponseBai = new BossResolutionResponse(
      10, Option("application/json"), Option("md5string"), "vault/test/test.bai?GoogleAccessId="
    )

    Servers.mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/")
    ).respond(
        response()
          .withHeaders(header)
          .withBody(bossObject.toJson.prettyPrint)
          .withStatusCode(200)
      )

    Servers.mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/testing_id/resolve")
    ).respond(
        response()
          .withHeaders(header)
          .withBody(bossResponseBam.toJson.prettyPrint)
          .withStatusCode(200)
      )


    Servers.mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/pathToBam/resolve")
    ).respond(
        response()
          .withHeaders(header)
          .withBody(bossResponseBam.toJson.prettyPrint)
          .withStatusCode(307)
      )

    Servers.mockBossServer.when(
      request()
        .withMethod("POST")
        .withPath("/objects/pathToBai/resolve")
    ).respond(
        response()
          .withHeaders(header)
          .withBody(bossResponseBai.toJson.prettyPrint)
          .withStatusCode(307)
      )



  }
}
