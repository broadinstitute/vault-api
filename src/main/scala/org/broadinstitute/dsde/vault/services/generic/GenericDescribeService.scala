package org.broadinstitute.dsde.vault.services.generic

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import org.broadinstitute.dsde.vault.model.{GenericRelationship, GenericRelEnt, GenericEntity, GenericSysAttrs}
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpService

@Api(value="/entities", description="generic entity service", produces="application/json")
trait GenericDescribeService extends HttpService {
  private final val ApiPrefix = "entities"
  private final val ApiVersions = "v1"
  private final val DefaultVersion = 1

  // up and down are more specific matches of the same route, so they must be listed first
  val gdRoutes = findUpstreamRoute ~ findDownstreamRoute ~ describeRoute

  @ApiOperation(
    value="get data for a particular entity",
    nickname="fetchEntity",
    httpMethod="GET",
    response=classOf[GenericEntity])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name="version", required=true, dataType="string", paramType="path", value="API version", allowableValues=ApiVersions),
    new ApiImplicitParam(name="id", required=true, dataType="string", paramType="path", value="vault ID")))
  @ApiResponses(Array(
    new ApiResponse(code=200, message="Successful"),
    new ApiResponse(code=404, message="Not Found"),
    new ApiResponse(code=500, message="Internal Error")))
  def describeRoute = {
    pathVersion(ApiPrefix,DefaultVersion,Segment) { (version, guid) =>
      get {
        respondWithMediaType(`application/json`) {
          complete {
            //STUB

            // interact with BOSS, receive getUrl
            // remove entity.sysAttrs.bossId, add entity.getUrl

            val sysAttrs = GenericSysAttrs(bossID = None, 12345, "stub user", None, None)
            GenericEntity("entity 3 guid", None, Option("entity 3 getUrl"), "stub", sysAttrs, None).toJson.prettyPrint
          }
        }
      }
    }
  }

  @ApiOperation(
    value="get entities upstream of a specified entity",
    nickname="findUpstream",
    httpMethod="GET",
    response=classOf[GenericRelEnt],
    responseContainer="List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name="version", required=true, dataType="string", paramType="path", value="API version", allowableValues=ApiVersions),
    new ApiImplicitParam(name="id", required=true, dataType="string", paramType="path", value="vault ID"),
    new ApiImplicitParam(name="up", required=true, dataType="string", defaultValue="up", paramType="query", value="marker for upstream query")))
  @ApiResponses(Array(
    new ApiResponse(code=200, message="Successful"),
    new ApiResponse(code=404, message="Not Found"),
    new ApiResponse(code=500, message="Internal Error")))
  def findUpstreamRoute = {
    pathVersion(ApiPrefix,DefaultVersion,Segment) { (version, guid) =>
      get {
        parameters('up) { up =>
          respondWithMediaType(`application/json`) {
            complete {
              //STUB

              // foreach entity upstream
              // interact with BOSS, receive getUrl
              // remove entity.sysAttrs.bossId, add entity.getUrl

              val rel = GenericRelationship("stub upward relation type", None)
              val sysAttrs = GenericSysAttrs(bossID = None, 12345, "stub user", None, None)
              val ent = GenericEntity("entity 4 guid", None, Option("entity 4 getUrl"), "stub", sysAttrs, None)
              GenericRelEnt(rel, ent).toJson.prettyPrint
            }
          }
        }
      }
    }
  }

  @ApiOperation(
    value="get entities downstream of a specified entity",
    nickname="findDownstream",
    httpMethod="GET",
    response=classOf[GenericRelEnt],
    responseContainer="List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name="version", required=true, dataType="string", paramType="path", value="API version", allowableValues=ApiVersions),
    new ApiImplicitParam(name="id", required=true, dataType="string", paramType="path", value="vault ID"),
    new ApiImplicitParam(name="down", required=true, dataType="string", defaultValue="down", paramType="query", value="marker for downstream query")))
  @ApiResponses(Array(
    new ApiResponse(code=200, message="Successful"),
    new ApiResponse(code=404, message="Not Found"),
    new ApiResponse(code=500, message="Internal Error")))
  def findDownstreamRoute = {
    pathVersion(ApiPrefix,DefaultVersion,Segment) { (version, guid) =>
      get {
        parameters('down) { down =>
          respondWithMediaType(`application/json`) {
            complete {
              //STUB

              // foreach entity downstream
              // interact with BOSS, receive getUrl
              // remove entity.sysAttrs.bossId, add entity.getUrl

              val rel = GenericRelationship("stub downward relation type", None)
              val sysAttrs = GenericSysAttrs(bossID = None, 12345, "stub user", None, None)
              val ent = GenericEntity("entity 5 guid", None, Option("entity 5 getUrl"), "stub", sysAttrs, None)
              GenericRelEnt(rel, ent).toJson.prettyPrint
            }
          }
        }
      }
    }
  }

}