package org.broadinstitute.dsde.vault.services.uBAMCollection

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.routing._

@Api(value = "/collections", description = "uBamCollection Service", produces = "application/json")
trait UBamCollectionDescribeService extends HttpService {

  val ubcdRoute = ubamCollectionDescribeRoute

  @ApiOperation(value = "Describes an uBamCollection",
    nickname = "uBamCollection_describe",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[UBamCollection]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBamCollection Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def ubamCollectionDescribeRoute = {
    path("collections" / Segment) {
      id => {
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

}



