package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.StatusCodes
import spray.routing._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait RedirectService extends HttpService {

  val baseURI = "http://example.com/"
  val routes = redirectRoute

  @ApiOperation(value = "Redirects to uBAM Files", nickname = "ubam_redirect", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBAM Vault ID"),
    new ApiImplicitParam(name = "filetype", required = true, dataType = "string", paramType = "path", value = "Filetype")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 307, message = "Redirected"),
    new ApiResponse(code = 400, message = "Filetype Not Found"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def redirectRoute =
    path("ubams" / Segment / Segment) {
      (id, filetype) =>
        // ignore values in stub
        redirect(baseURI, StatusCodes.TemporaryRedirect)
    }

}
