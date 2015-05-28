package org.broadinstitute.dsde.vault.services.generic

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model.{GenericSysAttrs, GenericEntity, GenericIngest}
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import spray.json._
import spray.http.MediaTypes._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService

@Api(value="/entities", description="generic entity service", produces="application/json")
trait GenericIngestService extends HttpService {
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
          respondWithMediaType(`application/json`) {
            complete {
              //STUB

              // for entity <- ingest.entities
              // use entity.blob to interact with BOSS, receive bossId and signedPutUrl
              // remove entity.blob, add entity.sysAttrs.bossId
              // register entity in DM, receive vault guid
              // remove entity.sysAttrs.bossId, add entity.guid and entity.signedPutUrl

              val sysAttrs = GenericSysAttrs(bossID = None, 12345, "stub user", None, None)
              List(GenericEntity("entity 1 guid", Option("entity 1 signedPutUrl"), None, "stub", sysAttrs, None),
                GenericEntity("entity 2 guid", Option("entity 2 signedPutUrl"), None, "stub", sysAttrs, None) ).toJson.prettyPrint
            }
          }
        }
      }
    }
  }
}