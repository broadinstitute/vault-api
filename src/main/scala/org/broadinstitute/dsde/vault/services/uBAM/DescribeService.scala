package org.broadinstitute.dsde.vault.services.uBAM

import akka.actor.Props
import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model._
import spray.routing._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait DescribeService extends HttpService {

  val routes = describeRoute

  @ApiOperation(value = "Describes a uBAM's metadata and associated files.  Does not generate presigned URLs.",
    nickname = "ubam_describe",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[UBam]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBAM Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def describeRoute = {
    path("ubams" / Segment) {
      id =>
        get {
          respondWithMediaType(`application/json`) {
            requestContext => {
              val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
              val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, dmService))
              describeActor ! DescribeServiceHandler.DescribeMessage(id)
            }
          }
        }
    }
  }

}



