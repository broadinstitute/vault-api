package org.broadinstitute.dsde.vault.services.analysis

import akka.actor.Props
import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.DmClientService
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.VaultDirectives
import spray.routing._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json")
trait AnalysisDescribeService extends HttpService with VaultDirectives {

  private final val ApiPrefix = "analyses"
  private final val ApiVersions = "v1"

  val adRoute = analysisDescribeRoute

  @ApiOperation(value = "Describes an Analysis' metadata, inputs, and output files.  Does not generate presigned URLs.",
    nickname = "analysis_describe",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[Analysis],
    notes = "The files key will be empty if the analysis is still running."
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Analysis Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def analysisDescribeRoute = {
    pathVersion( ApiPrefix ,1, Segment) { (version, id) =>
      get {
        respondWithJSON { requestContext =>
          val dmService = actorRefFactory.actorOf(Props(new DmClientService(requestContext)))
          val describeActor = actorRefFactory.actorOf(DescribeServiceHandler.props(requestContext, version, dmService))
          describeActor ! DescribeServiceHandler.DescribeMessage(id)
        }
      }
    }
  }

}


