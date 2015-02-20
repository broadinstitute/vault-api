package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.StatusCodes
import spray.routing._

@Api(value = "/ubam_redirect", description = "uBAM Redirection Service", position = 0)
trait RedirectService extends HttpService {

  val baseURI = "http://example.com/"
  val routes = redirectRoute

  @ApiOperation(value = "Redirects to uBAM Files", nickname = "ubam_redirect", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "dm_id", required = true, dataType = "string", paramType = "path", value = "uBAM DM ID"),
    new ApiImplicitParam(name = "filetype", required = true, dataType = "string", paramType = "path", value = "File Type (BAM or BAI)")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 301, message = "Redirected"),
    new ApiResponse(code = 404, message = "DM ID not found")
  ))
  def redirectRoute =
    path("ubam_redirect" / Segment / Segment) {
      (dm_id, filetype) =>
        // ignore values in stub
        redirect(baseURI, StatusCodes.MovedPermanently)
    }

}
