package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.routing._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait UBamDescribeListService extends HttpService {

  val routes = uBamDescribeListRoute

  private final val ApiVersions = "v1"




  @ApiOperation(value = "Describes a list of uBAM's metadata and associated files.  Does not generate presigned URLs.",
    nickname = "ubam_describe_list",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[UBam],
    responseContainer = "List"
  )
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "page[limit]", required = false, dataType = "integer", paramType = "query", value = "uBAM limit", allowableValues = "range[0, 2147483647]")
  ))
  def uBamDescribeListRoute = {
    path("ubams" / "v" ~ IntNumber) { version =>
      get {
        parameter("page[limit]".as[Int].?) { pageLimit =>
          respondWithMediaType(`application/json`) {
            requestContext => {
              val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
              val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, dmService))
              describeActor ! DescribeServiceHandler.DescribeListMessage(version, pageLimit)
            }
          }
        }
      }
    }
  }

}
