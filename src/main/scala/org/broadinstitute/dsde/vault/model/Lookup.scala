package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import spray.json.DefaultJsonProtocol

import scala.annotation.meta.field
object LookupJsonProtocol extends DefaultJsonProtocol {
  implicit val impEntitySearchResult = jsonFormat2(EntitySearchResult)
}

@ApiModel(value = "An entity search result")
case class EntitySearchResult
(
  @(ApiModelProperty@field)(value = "The unique id for this entity.", required = true)
  guid: String,

  @(ApiModelProperty@field)(value = "The type of entity.", required = true)
  `type`: String
)