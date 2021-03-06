package org.broadinstitute.dsde.vault.services.analysis

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.httpx.SprayJsonSupport._
import spray.routing._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json")
trait AnalysisIngestService extends HttpService with VaultDirectives {

  private final val ApiPrefix = "analyses"
  private final val ApiVersions = "v1"

  val aiRoute = analysisIngestRoute

  @ApiOperation(
    value = "Creates Analysis objects",
    nickname = "analysis_ingest",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json",
    response = classOf[AnalysisIngestResponse],
    notes = """Accepts a json packet as POST. Creates a Vault object with the supplied metadata and creates relationships for each input id.
      Returns the Vault ID of the created object. The values of the 'input' array must be valid Vault IDs for the ubams used as input to this analysis.
      If an invalid id is specified inside the input array, this API will fail with a 404 response code.""")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.AnalysisIngest", paramType = "body", value = "Analysis to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 404, message = "Not Found: if the 'input' array includes an invalid id"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def analysisIngestRoute =
    pathVersion( ApiPrefix ,1 ) { version =>
      post {
        respondWithJSON {
          entity(as[AnalysisIngest]) { ingest => requestContext =>
            val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
            val ingestActor = actorRefFactory.actorOf(IngestServiceHandler.props(requestContext, version, dmService))
            ingestActor ! IngestServiceHandler.IngestMessage(ingest)
          }
        }
      }
    }

}
