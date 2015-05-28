package org.broadinstitute.dsde.vault.services.lookup

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.http.MediaTypes._
import spray.routing._

@Api(value = "/query", description = "Lookup Service", produces = "application/json", position = 0)
trait LookupService extends HttpService with VaultDirectives {

  private final val ApiPrefix = "query"
  private final val ApiVersions = "v1"

  val lRoute = lookupRoute

  @Deprecated
  @ApiOperation(value = "Queries entities by type and attribute key/value pair",
    nickname = "lookup",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[EntitySearchResult]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
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
    pathVersion( ApiPrefix , 1 , Segment / Segment / Segment) { (version, entityType, attributeName, attributeValue) =>
      get {
        respondWithJSON { requestContext =>
          val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
          val describeActor = actorRefFactory.actorOf(LookupServiceHandler.props(requestContext, version, dmService))
          describeActor ! LookupServiceHandler.LookupMessage(entityType, attributeName, attributeValue)
        }
      }
    }
  }

}





