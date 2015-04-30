package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import spray.http.MediaTypes._
import spray.routing._
import org.broadinstitute.dsde.vault.model._

import spray.httpx.SprayJsonSupport._
import uBAMJsonProtocol._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait UBamIngestService extends HttpService {

  val ubiRoute = uBamIngestRoute

  @ApiOperation(
    value = "Creates uBAM objects",
    nickname = "ubam_ingest",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json",
    response = classOf[UBamIngestResponse],
    notes = "Accepts a json packet as POST. Creates a Vault object with the supplied metadata and allocates BOSS objects for each supplied file key; ignores the values for each file. Returns the Vault ID of the object as well as" +
      " presigned PUT urls - one for each key in the 'files' subobject. If a custom header of type 'X-Force-Location' has a case-insensitive value of 'true', then the file path locations will not be ignored and instead will be used as the location of the file object.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.UBamIngest", paramType = "body", value = "uBAM to create"),
    new ApiImplicitParam(name = "X-Force-Location", required = false, dataType = "boolean", paramType = "header", value = "When true, honors file path locations")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def uBamIngestRoute =
    path("ubams") {
      post {
        optionalHeaderValueByName("X-Force-Location") {
          forceLocation =>
          respondWithMediaType(`application/json`) {
            entity(as[UBamIngest]) {
              ingest =>
                requestContext =>
                  val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
                  val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
                  val ingestActor = actorRefFactory.actorOf(IngestServiceHandler.props(requestContext, bossService, dmService))
                  ingestActor ! IngestServiceHandler.IngestMessage(ingest, forceLocation)
            }
          }
        }
      }
    }
}




