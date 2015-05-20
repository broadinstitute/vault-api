package org.broadinstitute.dsde.vault.services.uBAMCollection

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.routing._

@Api(value = "/ubamcollections", description = "uBamCollection Service", produces = "application/json")
trait UBamCollectionDescribeService extends HttpService with VaultDirectives {

  private final val ApiPrefix = "ubamcollections"
  private final val ApiVersions = "v1"

  val ubcdRoute = ubamCollectionDescribeRoute

  @ApiOperation(value = "Describes an uBamCollection",
    nickname = "uBamCollection_describe",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[UBamCollection]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBamCollection Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def ubamCollectionDescribeRoute = {
    path( ApiPrefix / "v" ~ IntNumber / Segment) { (version, id) =>
        get {
          respondWithJSON{
            requestContext => {
              val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
              val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, dmService))
              describeActor ! DescribeServiceHandler.DescribeMessage(id)
            }
          }
        }
      }
    }

}



