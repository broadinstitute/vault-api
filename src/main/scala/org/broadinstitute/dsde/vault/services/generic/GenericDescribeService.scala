package org.broadinstitute.dsde.vault.services.generic

import javax.ws.rs.Path

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import org.broadinstitute.dsde.vault.{GenericDmClientService, BossClientService}
import org.broadinstitute.dsde.vault.model.GenericJsonProtocol._
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.json._
import spray.routing.HttpService

@Api(value="/entities", description="generic entity service", produces="application/json")
trait GenericDescribeService extends HttpService with VaultDirectives {
  private final val ApiPrefix = "entities"

  private final val SwaggerApiVersions = "v1"

  // up and down are more specific matches of the same route, so they must be listed first
  val gdRoutes = findUpstreamRoute ~ findDownstreamRoute ~ describeRoute

  @ApiOperation(
    value = "get data for a particular entity",
    nickname = "fetchEntity",
    httpMethod = "GET",
    response = classOf[GenericEntity])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = SwaggerApiVersions),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "vault ID")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 404, message = "Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")))
  def describeRoute = {
    path(ApiPrefix / "v" ~ IntNumber / Segment) { (version, guid) =>
      get {
        respondWithJSON { requestContext =>
          val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
          val dmService = actorRefFactory.actorOf(GenericDmClientService.props(requestContext))
          val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, bossService, dmService))
          describeActor ! DescribeServiceHandler.DescribeMessage(guid)
        }
      }
    }
  }

  @Path("/{version}/{id}?up")
  @ApiOperation(
    value = "get entities upstream of a specified entity",
    nickname = "findUpstream",
    httpMethod = "GET",
    response = classOf[GenericRelEnt],
    responseContainer = "List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = SwaggerApiVersions),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "vault ID")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 404, message = "Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")))
  def findUpstreamRoute = {
    path(ApiPrefix / "v" ~ IntNumber / Segment) { (version, guid) =>
      get {
        parameters('up) { up =>
          respondWithJSON { requestContext =>
            val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
            val dmService = actorRefFactory.actorOf(GenericDmClientService.props(requestContext))
            val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, bossService, dmService))
            describeActor ! DescribeServiceHandler.DescribeUpMessage(guid)
          }
        }
      }
    }
  }

  @Path("/{version}/{id}?down")
  @ApiOperation(
    value = "get entities downstream of a specified entity",
    nickname = "findDownstream",
    httpMethod = "GET",
    response = classOf[GenericRelEnt],
    responseContainer = "List")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = SwaggerApiVersions),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "vault ID")))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 404, message = "Not Found"),
    new ApiResponse(code = 500, message = "Internal Error")))
  def findDownstreamRoute = {
    path(ApiPrefix / "v" ~ IntNumber / Segment) { (version, guid) =>
      get {
        parameters('down) { down =>
          respondWithJSON { requestContext =>
            val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
            val dmService = actorRefFactory.actorOf(GenericDmClientService.props(requestContext))
            val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, bossService, dmService))
            describeActor ! DescribeServiceHandler.DescribeDownMessage(guid)
          }
        }
      }
    }
  }

}