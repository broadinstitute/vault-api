package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import spray.json.DefaultJsonProtocol

import scala.annotation.meta.field


object IndexResponseJsonProtocol extends DefaultJsonProtocol {
  implicit val impIndexResponse = jsonFormat1(IndexResponse)
}

@ApiModel(value = "Index Response.")
case class IndexResponse
(
  @(ApiModelProperty@field)(value = "The indexing result message.")
  messageResult: String


)
