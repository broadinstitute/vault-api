package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations._
import spray.json.DefaultJsonProtocol
import scala.annotation.meta.field

object uBAMJsonProtocol extends DefaultJsonProtocol {
  implicit val impUBam = jsonFormat4(UBam)
  implicit val impUBamIngest = jsonFormat2(UBamIngest)
  implicit val impUBamIngestResponse = jsonFormat3(UBamIngestResponse)
}

@ApiModel(value = "An unmapped BAM")
case class UBam(
  @(ApiModelProperty @field)(required=true, value="The Vault ID of this uBAM")
  id: String,
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value="The metadata key-value pairs associated with this uBAM.")
  metadata: Map[String,String],
  @(ApiModelProperty @field)(required=true, value = "The properties of this uBAM")
  properties: Option[Map[String, String]] = None)

case class UBamIngest(
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value="The metadata key-value pairs associated with this uBAM.")
  metadata: Map[String,String])

case class UBamIngestResponse(
  @(ApiModelProperty @field)(required=true, value="The Vault ID of this uBAM")
  id: String,
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value = "The properties of this uBAM")
  properties: Option[Map[String, String]] = None)
