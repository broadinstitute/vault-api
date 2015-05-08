package org.broadinstitute.dsde.vault.services.analysis

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import org.broadinstitute.dsde.vault.common.directives.VersioningDirectives._
import spray.routing._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json", position = 0)
trait AnalysisRedirectService extends HttpService {

  private final val ApiVersions = "v1"

  val arRoute = analysisRedirectRoute

  @ApiOperation(value = "Redirects to presigned GET URLs for analyses", nickname = "analysis_redirect", httpMethod = "GET",
    notes = "Returns an HTTP 307 redirect to a presigned GET URL for the specified analysis output file. If the caller would like presigned URLs to all files within an analysis, " +
      "it is the caller's responsibility to make multiple requests to this API - one for each file.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "version", required = true, dataType = "string", paramType = "path", value = "API version", allowableValues = ApiVersions),
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Analysis Vault ID"),
    new ApiImplicitParam(name = "filetype", required = true, dataType = "string", paramType = "path", value = "The user-specified unique key for this file, e.g. 'bam' or 'vcf'")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 307, message = "Redirected"),
    new ApiResponse(code = 400, message = "Filetype Not Found: the Vault ID exists, but the specified filetype argument does not exist in that object"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def analysisRedirectRoute =
    pathVersion("analyses", Segment / Segment) { (versionOpt, id, filetype) =>
      get { requestContext =>
        val version = versionOpt.getOrElse(1)
        val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
        val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
        val redirectActor = actorRefFactory.actorOf(RedirectServiceHandler.props(requestContext, version, bossService, dmService))
        redirectActor ! RedirectServiceHandler.RedirectMessage(id, filetype)
      }
    }

}
