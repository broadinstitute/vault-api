package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.{BossClientService, DmClientService}
import spray.routing._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait UBamRedirectService extends HttpService {

  val ubrRoute = uBamRedirectRoute

  @ApiOperation(value = "Redirects to presigned GET URLs for uBAM files", nickname = "ubam_redirect", httpMethod = "GET",
    notes = "Returns an HTTP 307 redirect to a presigned GET URL for the specified uBAM file. If the caller would like presigned URLs to all files within an object, " +
      "it is the caller's responsibility to make multiple requests to this API - one for each file.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBAM Vault ID"),
    new ApiImplicitParam(name = "filetype", required = true, dataType = "string", paramType = "path", value = "The user-specified unique key for this file, e.g. 'bam' or 'bai'")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 307, message = "Redirected"),
    new ApiResponse(code = 400, message = "Filetype Not Found: the Vault ID exists, but the specified filetype argument does not exist in that object"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def uBamRedirectRoute =
    path("ubams" / Segment / Segment) { (id, filetype) =>
      get { requestContext =>
        val bossService = actorRefFactory.actorOf(BossClientService.props(requestContext))
        val dmService = actorRefFactory.actorOf(DmClientService.props(requestContext))
        val redirectActor = actorRefFactory.actorOf(RedirectServiceHandler.props(requestContext, bossService, dmService))
        redirectActor ! RedirectServiceHandler.RedirectMessage(id, filetype)
      }
    }

}
