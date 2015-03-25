package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations._
import spray.json.DefaultJsonProtocol
import scala.annotation.meta.field

object uBAMJsonProtocol extends DefaultJsonProtocol {
  implicit val impuBAM = jsonFormat3(uBAM)
  implicit val impuBAMIngest = jsonFormat2(uBAMIngest)
  implicit val impuBAMIngestResponse = jsonFormat2(uBAMIngestResponse)
}

@ApiModel(value = "An unmapped BAM")
case class uBAM(
  @(ApiModelProperty @field)(required=true, value="The Vault ID of this uBAM")
  id: String,
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value="The metadata key-value pairs associated with this uBAM.")
  metadata: Map[String,String])

case class uBAMIngest(
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value="The metadata key-value pairs associated with this uBAM.")
  metadata: Map[String,String])

case class uBAMIngestResponse(
  @(ApiModelProperty @field)(required=true, value="The Vault ID of this uBAM")
  id: String,
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String])
