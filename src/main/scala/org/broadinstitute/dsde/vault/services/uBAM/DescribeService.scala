package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._
import org.broadinstitute.dsde.vault.model._

object DescribeJsonProtocol extends DefaultJsonProtocol {
  implicit val metadata = jsonFormat12(Metadata)
  implicit val json = jsonFormat3(uBAM)
}
import DescribeJsonProtocol._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait DescribeService extends HttpService {

  val routes = describeRoute

  @ApiOperation(value = "Describes a uBAM's metadata and associated files.  Does not generate presigned URLs.",
    nickname = "ubam_describe",
    httpMethod = "GET",
    produces = "application/json",
    response=classOf[uBAM],
    notes="Supports arbitrary metadata keys, but this is not represented well in Swagger (see the 'additionalMetadata' note below)"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBAM Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def describeRoute =
    path("ubams" / Segment) {
      id =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              uBAM(
                id,
                Map("bam"->"sample BOSS id 1", "bai"->"sample BOSS id 2", "..."->"more files"),
                Metadata(
                  "dummy ownerId",
                  "dummy md5",
                  "dummy project",
                  "dummy individualAlias",
                  "dummy sampleAlias",
                  "dummy readGroupAlias",
                  "dummy libraryName",
                  "dummy sequencingCenter",
                  "dummy platform",
                  "dummy platformUnit",
                  "dummy runDate",
                  "..."
                )
              ).toJson.prettyPrint
            }
          }
        }
      }
  
}



