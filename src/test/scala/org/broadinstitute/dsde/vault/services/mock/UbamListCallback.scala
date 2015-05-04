package org.broadinstitute.dsde.vault.services.mock

import org.broadinstitute.dsde.vault.model.UBam
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import org.mockserver.mock.action.ExpectationCallback
import org.mockserver.model.Header.header
import org.mockserver.model.HttpResponse.{notFoundResponse, response}
import org.mockserver.model.{HttpRequest, HttpResponse, HttpStatusCode}
import spray.json.{DefaultJsonProtocol, _}

class UbamListCallback extends ExpectationCallback {

  val ubam = new UBam(
    "testing_id",
    Map(("bam", "http://localhost:8080/path/to/bam"), ("bai", "pathToBai")),
    Map("testAttr" -> "testValue")
  )

  val ubamList = List(ubam)
  val emptyUbamList = List()


  import DefaultJsonProtocol._

  def createResponse( list: List[UBam]): HttpResponse = {
    response()
      .withStatusCode(HttpStatusCode.OK_200.code())
      .withHeaders(
        header("Content-Type", "application/json")
      )
      .withBody(list.toJson.prettyPrint)
  }

  override def handle(httpRequest: HttpRequest): HttpResponse = {
    if(httpRequest.hasQueryStringParameter("page[limit]", "0")){
      return createResponse(emptyUbamList)
    } else {
      if(httpRequest.hasQueryStringParameter("page[limit]", "1")){
        return createResponse(ubamList)
      } else {
        notFoundResponse()
      }
    }
  }
}