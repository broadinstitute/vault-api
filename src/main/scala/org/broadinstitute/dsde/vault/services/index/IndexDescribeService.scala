package org.broadinstitute.dsde.vault.services.index

import javax.ws.rs.Path

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.http.MediaTypes._
import spray.routing._

@Api(value = "/admin", description = "Admin Service", produces = "application/json")
trait IndexService extends HttpService with VaultDirectives {

  private final val ApiVersions = "v1"

  val adminRoutes = indexRoute


  @Path("/index/{version}/{entityType}")
  @ApiOperation(value = "Index all existing collection metadata.", nickname = "index", httpMethod = "POST",
    produces = "application/json", consumes = "application/json", notes = "Accepts an Entity Type as POST (unmappedBAM, analysis, uBAMCollection). Index the selected Entity and its metadata. ")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "entityType", required = true, dataType = "string", paramType = "path", value = "Entity Type")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def indexRoute = {
    path("admin" / "index" /"v" ~ IntNumber / Segment) { (version,entityType) => {
      post {
        respondWithJSON {
          respondWithMediaType(`application/json`) {
              requestContext =>
                val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
                val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, dmService))
                describeActor ! DescribeServiceHandler.Index(entityType)
            }
          }
        }
      }
    }
  }


}



