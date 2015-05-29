package org.broadinstitute.dsde.vault.services.generic

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import org.broadinstitute.dsde.vault.{GenericDmClientService, BossClientService}
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model.{GenericEntity, GenericIngest}
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

@Api(value="/entities", description="generic entity service", produces="application/json")
trait GenericIngestService extends HttpService with VaultDirectives {
  private final val ApiPrefix = "entities"
  private final val ApiVersions = "v1"
  private final val DefaultVersion = 1

  val giRoute = ingestRoute

  @ApiOperation(
    value = "store some entities and relationships",
    nickname = "genericIngest",
    httpMethod = "POST",
    response = classOf[GenericEntity],
    responseContainer = "List",
    notes = "response is a list of the new entities")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.GenericIngest", paramType = "body", value = "entities and relations to create")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 500, message = "Internal Error")))
  def ingestRoute = {
    pathVersion(ApiPrefix, DefaultVersion) { version =>
      post {
        entity(as[GenericIngest]) { ingest =>
          respondWithJSON { requestContext =>
            val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
            val dmService = actorRefFactory.actorOf(GenericDmClientService.props(requestContext))
            val ingestActor = actorRefFactory.actorOf(IngestServiceHandler.props(requestContext, version, bossService, dmService))
            ingestActor ! IngestServiceHandler.IngestMessage(ingest)
          }
        }
      }
    }
  }
}