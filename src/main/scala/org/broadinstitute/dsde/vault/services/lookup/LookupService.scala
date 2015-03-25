package org.broadinstitute.dsde.vault.services.lookup

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.routing._

@Api(value = "/query", description = "Lookup Service", produces = "application/json", position = 0)
trait LookupService extends HttpService {

  val routes = lookupRoute

  @ApiOperation(value = "Queries entities by type and attribute key/value pair",
    nickname = "lookup",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[EntitySearchResult]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "entityType", required = true, dataType = "string", paramType = "path", value = "entity type"),
    new ApiImplicitParam(name = "attributeName", required = true, dataType = "string", paramType = "path", value = "attribute name"),
    new ApiImplicitParam(name = "attributeValue", required = true, dataType = "string", paramType = "path", value = "attribute value")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault Entity Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def lookupRoute = {
    path("query" / Segment / Segment / Segment) {
      (entityType, attributeName, attributeValue) =>
        get {
          respondWithMediaType(`application/json`) {
            requestContext => {
              val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
              val describeActor = actorRefFactory.actorOf(LookupServiceHandler.props(requestContext, dmService))
              describeActor ! LookupServiceHandler.LookupMessage(entityType, attributeName, attributeValue)
            }
          }
        }
    }
  }

}


