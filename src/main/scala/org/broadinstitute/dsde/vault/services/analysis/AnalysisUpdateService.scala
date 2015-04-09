package org.broadinstitute.dsde.vault.services.analysis

import javax.ws.rs.Path

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import org.broadinstitute.dsde.vault.model.AnalysisJsonProtocol.impAnalysisUpdate
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.routing._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json", position = 1)
trait AnalysisUpdateService extends HttpService {

  val routes = analysisUpdateRoute

  @Path("/{id}/outputs")
  @ApiOperation(
    value = "Updates Analysis objects with output files",
    nickname = "analysis_update",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json",
    notes = "Accepts a json packet as POST. Updates the Vault analysis object with the supplied output files. Returns the Vault ID of the created object." +
      " If a custom header of type 'X-Force-Location' has a case-insensitive value of 'true', then the file path locations will be used as the location of the file object.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Analysis Vault ID"),
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.AnalysisUpdate", paramType = "body", value = "Analysis outputs to add"),
    new ApiImplicitParam(name = "X-Force-Location", required = false, dataType = "boolean", paramType = "header", value = "When true, honors file path locations")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "OK"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def analysisUpdateRoute =
    path("analyses" / Segment / "outputs") {
      id => {
        post {
          optionalHeaderValueByName("X-Force-Location") {
            forceLocation =>
            respondWithMediaType(`application/json`) {
              entity(as[AnalysisUpdate]) {
                update =>
                  requestContext => {
                    val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
                    val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
                    val updateActor = actorRefFactory.actorOf(UpdateServiceHandler.props(requestContext, dmService, bossService))
                    updateActor ! UpdateServiceHandler.UpdateMessage(id, update, forceLocation)
                  }
              }
            }
          }
        }
      }
    }

}
