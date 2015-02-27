package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._
import org.broadinstitute.dsde.vault.model._

case class IngestResponse(id: String, bamBossURL: String, baiBossURL: String)
object IngestJsonProtocol extends DefaultJsonProtocol {
  implicit val json = jsonFormat2(uBAMIngestResponse)
}
import IngestJsonProtocol._

@Api(value = "/ubams", description = "uBAM Service", produces = "application/json", position = 0)
trait IngestService extends HttpService {

  val routes = ingestRoute

  @ApiOperation(value = "Creates uBAM objects", nickname = "ubam_ingest", httpMethod = "POST",
    produces = "application/json", consumes = "application/json", response = classOf[uBAMIngestResponse],
    notes = "Accepts a json packet as POST. Creates a Vault object with the supplied metadata and allocates BOSS objects for each supplied file key; ignores the values for each file. " +
      " Returns the Vault ID of the object as well as presigned PUT urls - one for each key in the 'files' subobject.")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "body", required = true, dataType = "org.broadinstitute.dsde.vault.model.uBAMIngest", paramType = "body", value = "uBAM to create")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful"),
    new ApiResponse(code = 400, message = "Malformed Input"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def ingestRoute =
    path("ubams") {
      post {
        respondWithMediaType(`application/json`) {
          complete {
            uBAMIngestResponse("dummy ID", Map("bam"->"http://example.com/bam", "bai"->"http://example.com/bai", "..."->"moreFiles")).toJson.prettyPrint
          }
        }
      }
    }

}
