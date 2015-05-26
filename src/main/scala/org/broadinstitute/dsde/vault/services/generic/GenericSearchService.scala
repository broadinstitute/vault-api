package org.broadinstitute.dsde.vault.services.generic

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpService
import spray.httpx.SprayJsonSupport._

@Api(value="/entities", description="generic entity service", produces="application/json")
trait GenericSearchService extends HttpService {
  private final val ApiPrefix = "entities"
  private final val ApiVersions = "v1"
  private final val DefaultVersion = 1

  val gsRoute = searchRoute

  @ApiOperation(
    value = "find IDs of entities of a specified type having a specified metadata attribute value",
    nickname = "findEntitiesByTypeAndAttr",
    httpMethod = "GET",
    response = classOf[GenericEntity],
    responseContainer = "List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.GenericEntityQuery", paramType = "body", value = "entities to find")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 404, message = "Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")))
  def searchRoute = {
    pathVersion(ApiPrefix, DefaultVersion) { version =>
      get {
        entity(as[GenericEntityQuery]) { query =>
          respondWithMediaType(`application/json`) {
            complete {
              //STUB

              // for entity <- search results
              // use entity.sysAttrs.bossId to interact with BOSS, receive getUrl
              // remove entity.sysAttrs.bossId, add entity.getUrl

              val sysAttrs = GenericSysAttrs(bossID = None, 12345, "stub user", None, None)
              List(GenericEntity("entity 6 guid", None, Option("entity 6 getUrl"), "stub", sysAttrs, None),
                GenericEntity("entity 7 guid", None, Option("entity 7 getUrl"), "stub", sysAttrs, None) ).toJson.prettyPrint
            }
          }
        }
      }
    }
  }
}