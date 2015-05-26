package org.broadinstitute.dsde.vault.services.uBAMCollection

import javax.ws.rs.{POST, Path}

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.http.MediaTypes._
import spray.routing._
import spray.httpx.SprayJsonSupport._
import org.broadinstitute.dsde.vault.model.TermSearchJsonProtocol._

@Api(value = "/ubamcollections", description = "uBamCollection Service", produces = "application/json")
trait UBamCollectionDescribeService extends HttpService with VaultDirectives {

  private final val ApiPrefix = "ubamcollections"
  private final val ApiVersions = "v1"

  val ubcdRoute = ubamCollectionDescribeRoute  ~ searchRoute

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

  @Path("/{version}/search")
  @POST
  @ApiOperation(value = "Search",
    nickname = "search",
    httpMethod = "POST",
    produces = "application/json",
    response = classOf[UBamCollection],
    responseContainer = "List",
    notes = "Accepts a json packet as POST. Performs a search based on the supplied key/value List."
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "body", required = true, dataType = "List[org.broadinstitute.dsde.vault.model.TermSearch]", paramType = "body", value = " List of Key/value to search")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Bad Request"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def searchRoute = {
    path("ubamcollections" /"v" ~ IntNumber / "search") { version =>
      post {
        respondWithJSON {
          entity(as[List[TermSearch]]) { termsToSearch =>
            respondWithMediaType(`application/json`) {
              requestContext =>
                val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
                val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, dmService))
                describeActor ! DescribeServiceHandler.DescribeMessageFilterByTerm(termsToSearch)
            }
          }
        }
      }
    }
  }


}



