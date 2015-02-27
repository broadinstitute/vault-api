package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations._
import scala.annotation.meta.field

@ApiModel(value = "An unmapped BAM")
case class uBAM(
  @(ApiModelProperty @field)(required=true, value="The Vault ID of this uBAM")
  id: String,
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value="The metadata key-value pairs associated with this uBAM.")
  metadata: Metadata)

case class uBAMIngest(
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String],
  @(ApiModelProperty @field)(required=true, value="The metadata key-value pairs associated with this uBAM.")
  metadata: Metadata)

case class uBAMIngestResponse(
  @(ApiModelProperty @field)(required=true, value="The Vault ID of this uBAM")
  id: String,
  @(ApiModelProperty @field)(required=true, value="The files associated with this uBAM, each with a unique user-supplied string key.")
  files: Map[String,String])

@ApiModel(value = "Metadata for a BAM (mapped or aggregated).")
case class Metadata(
  @(ApiModelProperty @field)(required=true)
  ownerId: String,
  md5: String,
  project: String,
  individualAlias: String,
  sampleAlias: String,
  readGroupAlias: String,
  libraryName: String,
  sequencingCenter: String,
  platform: String,
  platformUnit: String,
  runDate: String,
  @(ApiModelProperty @field)(value = "indicates that this object supports arbitrary key-value pairs beyond the keys listed here. This is a hack right now" +
    " because I don't know the best way to represent varargs in Swagger.")
  additionalMetadata: String
)

