package org.broadinstitute.dsde.vault.services.mock

import org.broadinstitute.dsde.vault.model.Analysis
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.mockserver.mock.action.ExpectationCallback
import org.mockserver.model.Header.header
import org.mockserver.model.HttpResponse.{notFoundResponse, response}
import org.mockserver.model.{HttpRequest, HttpResponse, HttpStatusCode}
import spray.json._

class AnalysisUpdateSwitchCallback extends ExpectationCallback {

  def createResponse(analysis: Analysis): HttpResponse = {
    response()
      .withStatusCode(HttpStatusCode.OK_200.code())
      .withHeaders(
        header("Content-Type", "application/json")
      )
      .withBody(analysis.toJson.prettyPrint)
  }

  override def handle(httpRequest: HttpRequest): HttpResponse = {
    httpRequest match {
      case request if request.getPath.endsWith("/analyses") =>
        AnalysesResponseContainer.setEmptyResponse()
        createResponse(AnalysesResponseContainer.emptyAnalysis)
      case request if request.getPath.endsWith("/analyses/9b66665c-f41a-11e4-b9b2-1697f925ec7b") =>
        createResponse(AnalysesResponseContainer.responseAnalysis)
      case request if request.getPath.endsWith("9b66665c-f41a-11e4-b9b2-1697f925ec7b/outputs") =>
        AnalysesResponseContainer.setUpdatedResponse()
        createResponse(AnalysesResponseContainer.responseAnalysis)
      case _ => notFoundResponse()
    }
  }
}



