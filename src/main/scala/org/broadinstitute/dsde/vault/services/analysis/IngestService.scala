package org.broadinstitute.dsde.vault.services.analysis

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._
import org.broadinstitute.dsde.vault.model._

object IngestJsonProtocol extends DefaultJsonProtocol {
  implicit val json = jsonFormat1(AnalysisIngestResponse)
}
import IngestJsonProtocol._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json")
trait IngestService extends HttpService {

  val routes = ingestRoute

  @ApiOperation(value = "Creates Analysis objects", nickname = "analysis_ingest", httpMethod = "POST",
    produces = "application/json", consumes = "application/json", response = classOf[AnalysisIngestResponse],
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
  def ingestRoute =
    path("analyses") {
      post {
        respondWithMediaType(`application/json`) {
          complete {
            AnalysisIngestResponse("58c61109-ced8-46bd-a50a-0fb04b94908e").toJson.prettyPrint
          }
        }
      }
    }

}
