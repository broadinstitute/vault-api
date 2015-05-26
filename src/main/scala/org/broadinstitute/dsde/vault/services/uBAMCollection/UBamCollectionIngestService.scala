package org.broadinstitute.dsde.vault.services.uBAMCollection

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model.uBAMCollectionJsonProtocol._
import org.broadinstitute.dsde.vault.model.{UBamCollectionIngest, UBamCollectionIngestResponse}
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

@Api(value = "/ubamcollections", description = "Collections Service", produces = "application/json")
trait UBamCollectionIngestService extends HttpService with VaultDirectives {

  private final val ApiPrefix = "ubamcollections"

  private final val ApiVersions = "v1"

  val ubciRoute = uBAMCollectionIngestRoute

  @ApiOperation(
    value = "Creates an UBam collection object",
    nickname = "ubam_collection_ingest",
    httpMethod = "POST",
    produces = "application/json",
    consumes = "application/json",
    response = classOf[UBamCollectionIngestResponse],
    notes = "Accepts a json packet as POST. Creates a Vault collection object with the supplied ubam ids and the supplied metadata. " +
      " Returns the Vault ID of the created object.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.UBamCollectionIngest", paramType = "body", value = "Collection to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def uBAMCollectionIngestRoute =
    path( ApiPrefix / "v" ~ IntNumber) { version =>
      post {
        respondWithJSON {
            entity(as[UBamCollectionIngest]) {
              ingest =>
                requestContext =>
                  val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
                  val ingestActor = actorRefFactory.actorOf(IngestServiceHandler.props(requestContext, version, dmService))
                  ingestActor ! IngestServiceHandler.IngestMessage(ingest)
            }
          }
        }
      }
}
