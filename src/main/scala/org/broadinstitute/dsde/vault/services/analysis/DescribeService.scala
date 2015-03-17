package org.broadinstitute.dsde.vault.services.analysis

import com.wordnik.swagger.annotations._
import org.broadinstitute.dsde.vault.model._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._

object DescribeJsonProtocol extends DefaultJsonProtocol {
  implicit val impAnalysis = jsonFormat4(Analysis)
}

import org.broadinstitute.dsde.vault.services.analysis.DescribeJsonProtocol._

@Api(value = "/analyses", description = "Analysis Service", produces = "application/json")
trait DescribeService extends HttpService {

  val routes = describeRoute

  @ApiOperation(value = "Describes an Analysis' metadata, inputs, and output files.  Does not generate presigned URLs.",
    nickname = "analysis_describe",
    httpMethod = "GET",
    produces = "application/json",
    response = classOf[Analysis],
    notes = "The files key will be empty if the analysis is still running."
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", required = true, dataType = "string", paramType = "path", value = "Analysis Vault ID")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Request"),
    new ApiResponse(code = 404, message = "Vault ID Not Found"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def describeRoute = {
    path("analyses" / Segment) {
      id => {
        get {
          respondWithMediaType(`application/json`) {
            complete {
              Analysis(
                id,
                List("123", "456", "789"),
                Map(
                  "vcf" -> "http://vault/redirect/url/to/get/file",
                  "bam" -> "http://vault/redirect/url/to/get/file",
                  "bai" -> "http://vault/redirect/url/to/get/file",
                  "adapter_metrics" -> "http://vault/redirect/url/to/get/file"
                ),
                Map(
                  "analysisId" -> "CES_id",
                  "key1" -> "value1",
                  "key2" -> "value2",
                  "key3" -> "value3",
                  "key4" -> "value4",
                  "key5" -> "value5"
                )
              ).toJson.prettyPrint

            }
          }
        }
      }
    }
  }

}


