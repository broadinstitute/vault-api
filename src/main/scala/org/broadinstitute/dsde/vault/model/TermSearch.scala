package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import spray.json.DefaultJsonProtocol

import scala.annotation.meta.field

object TermSearchJsonProtocol extends DefaultJsonProtocol {
  implicit val impTermSearch = jsonFormat2(TermSearch)
}
@ApiModel(value = "Term search")
case class TermSearch
(
  @(ApiModelProperty@field)(value = "The key.", required = true)
  key: String,

  @(ApiModelProperty@field)(value = "The value.", required = true)
  value: String
  )
