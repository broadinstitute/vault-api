package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations._

import scala.annotation.meta.field

@ApiModel(value = "An Analysis")
case class Analysis(
  @(ApiModelProperty@field)(required = true, value = "The Vault ID of this Analysis")
  id: String,
  @(ApiModelProperty@field)(required = true, value = "The Vault IDs of the input uBAMs used by this Analysis")
  input: List[String],
  @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this Analysis.")
  metadata: Map[String, String],
  @(ApiModelProperty@field)(required = true, value = "The output files associated with this Analysis, each with a unique user-supplied string key.")
  files: Map[String, String])

case class AnalysisIngest(
  @(ApiModelProperty@field)(required = true, value = "The Vault IDs of the input uBAMs used by this Analysis")
  input: List[String],
  @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this Analysis.")
  metadata: Map[String, String])

case class AnalysisIngestResponse(
  @(ApiModelProperty@field)(required = true, value = "The Vault ID of this Analysis")
  id: String)

case class AnalysisUpdate(
  @(ApiModelProperty@field)(required = true, value = "The files associated with this Analysis, each with a unique user-supplied string key.")
  files: Map[String, String])

