package org.broadinstitute.dsde.vault.services.uBAM

import com.wordnik.swagger.annotations._
import spray.http.MediaTypes._
import spray.json._
import spray.routing._

case class FilesResponse(dm_id: String, bamBossID: String, baiBossID: String)
object FilesJsonProtocol extends DefaultJsonProtocol {
  implicit val json = jsonFormat3(FilesResponse)
}
import FilesJsonProtocol._

@Api(value = "/ubam_files", description = "uBAM Files Service", produces = "application/json", position = 0)
trait FilesService extends HttpService {

   val routes = filesRoute

   @ApiOperation(value = "Returns uBAM Files", nickname = "ubam_files", httpMethod = "GET", produces = "application/json")
   @ApiImplicitParams(Array(
     new ApiImplicitParam(name = "dm_id", required = true, dataType = "string", paramType = "path", value = "uBAM DM ID")
   ))
   @ApiResponses(Array(
     new ApiResponse(code = 200, message = "Successful Request"),
     new ApiResponse(code = 404, message = "DM ID not found")
   ))
   def filesRoute =
     path("ubam_files" / Segment) {
       dm_id =>
         get {
           respondWithMediaType(`application/json`) {
             complete {
               FilesResponse(
                 dm_id,
                 "dummy bam",
                 "dummy bai"
               ).toJson.prettyPrint
             }
           }
         }
     }

 }
