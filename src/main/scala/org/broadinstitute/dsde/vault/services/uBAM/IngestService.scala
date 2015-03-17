package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import spray.http.MediaTypes._
import spray.routing._
import org.broadinstitute.dsde.vault.model._

import spray.httpx.SprayJsonSupport._
import uBAMJsonProtocol._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait IngestService extends HttpService {

  val routes = ingestRoute

  @ApiOperation(value = "Creates uBAM objects", nickname = "ubam_ingest", httpMethod = "POST",
    produces = "application/json", consumes = "application/json", response = classOf[uBAMIngestResponse],
    notes = "Accepts a json packet as POST. Creates a Vault object with the supplied metadata and allocates BOSS objects for each supplied file key; ignores the values for each file. " +
      " Returns the Vault ID of the object as well as presigned PUT urls - one for each key in the 'files' subobject.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.uBAMIngest", paramType = "body", value = "uBAM to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def ingestRoute =
    path("ubams") {
      post {
        respondWithMediaType(`application/json`) {
          entity(as[uBAMIngest]) {
            ingest =>
              requestContext =>
                val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
                val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
                val ingestActor = actorRefFactory.actorOf(IngestServiceHandler.props(requestContext, bossService, dmService))
                ingestActor ! IngestServiceHandler.IngestMessage(ingest)
          }
        }
      }
    }
}




