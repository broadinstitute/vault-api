package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._

case class IngestResponse(id: String, bamBossURL: String, baiBossURL: String)
object IngestJsonProtocol extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(IngestResponse)
}
import IngestJsonProtocol._

@Api(value = "/ubam_ingest", description = "uBAM Ingest Service", produces = "application/json", position = 0)
trait IngestService extends HttpService {

  val routes = ingestRoute

  @ApiOperation(value = "Ingests uBAM Files", nickname = "ubam_ingest", httpMethod = "POST", produces = "application/json")
  @ApiResponses(Array(
    new ApiResponse(code = 200, message = "Successful Ingest"),
    new ApiResponse(code = 400, message = "Malformed Ingest Command"),
    new ApiResponse(code = 500, message = "Vault Internal Error")
  ))
  def ingestRoute =
    path("ubam_ingest") {
      post {
        respondWithMediaType(`application/json`) {
          complete {
            IngestResponse("dummy ID", "http://example.com/bam", "http://example.com/bai").toJson.prettyPrint
          }
        }
      }
    }

}
