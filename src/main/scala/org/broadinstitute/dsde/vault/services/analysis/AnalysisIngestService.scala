package org.broadinstitute.dsde.vault.services.analysis

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.routing._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json")
trait AnalysisIngestService extends HttpService {

  val routes = analysisIngestRoute

  @ApiOperation(
    value = "Creates Analysis objects",
    nickname = "analysis_ingest",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json",
    response = classOf[AnalysisIngestResponse],
    notes = "Accepts a json packet as POST. Creates a Vault object with the supplied metadata and creates relationships for each input id. " +
      " Returns the Vault ID of the created object.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.AnalysisIngest", paramType = "body", value = "Analysis to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def analysisIngestRoute =
    path("analyses") {
      post {
        respondWithMediaType(`application/json`) {
          entity(as[AnalysisIngest]) {
            ingest =>
              requestContext =>
                val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
                val ingestActor = actorRefFactory.actorOf(IngestServiceHandler.props(requestContext, dmService))
                ingestActor ! IngestServiceHandler.IngestMessage(ingest)
          }
        }
      }
    }

}
