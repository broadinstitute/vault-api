package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._

case class DescribeResponse(id: String,
                            ownerId: String,
                            bam: String,
                            bai: String,
                            md5: String,
                            project: String,
                            individualAlias: String,
                            sampleAlias: String,
                            readGroupAlias: String,
                            libraryName: String,
                            sequencingCenter: String,
                            platform: String,
                            platformUnit: String,
                            runDate: String)
object DescribeJsonProtocol extends DefaultJsonProtocol {
  implicit val json = jsonFormat14(DescribeResponse)
}
import DescribeJsonProtocol._

@Api(value = "/ubam_describe", description = "uBAM Describe Service", produces = "application/json", position = 0)
trait DescribeService extends HttpService {

  val routes = describeRoute

  @ApiOperation(value = "Returns uBAM Metadata", nickname = "ubam_describe", httpMethod = "GET", produces = "application/json")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "uBAM Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID not found")
  ))
  def describeRoute =
    path("ubam_describe" / Segment) {
      id =>
        get {
          respondWithMediaType(`application/json`) {
            complete {
              DescribeResponse(
                id,
                "dummy ownerId",
                "dummy bam",
                "dummy bai",
                "dummy md5",
                "dummy project",
                "dummy individualAlias",
                "dummy sampleAlias",
                "dummy readGroupAlias",
                "dummy libraryName",
                "dummy sequencingCenter",
                "dummy platform",
                "dummy platformUnit",
                "dummy runDate"
              ).toJson.prettyPrint
            }
          }
        }
      }

}
