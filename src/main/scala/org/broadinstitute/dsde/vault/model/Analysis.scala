package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations._
import spray.json.DefaultJsonProtocol

import scala.annotation.meta.field

object AnalysisJsonProtocol extends DefaultJsonProtocol {
  implicit val impAnalysisIngestResponse = jsonFormat2(AnalysisIngestResponse)
  implicit val impAnalysisIngest = jsonFormat2(AnalysisIngest)
  implicit val impAnalysis = jsonFormat5(Analysis)
  implicit val impAnalysisUpdate = jsonFormat1(AnalysisUpdate)
  implicit val impAnalysisDMUpdate = jsonFormat2(AnalysisDMUpdate)
}

@ApiModel(value = "An Analysis")
case class Analysis(
  @(ApiModelProperty@field)(required = true, value = "The Vault ID of this Analysis")
  id: String,
  @(ApiModelProperty@field)(required = true, value = "The Vault IDs of the input uBAMs used by this Analysis")
  input: List[String],
  @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this Analysis.")
  metadata: Map[String, String],
  @(ApiModelProperty@field)(required = true, value = "The properties of this Analysis")
  properties: Option[Map[String, String]] = None,
  @(ApiModelProperty@field)(required = true, value = "The output files associated with this Analysis, each with a unique user-supplied string key.")
  files: Option[Map[String, String]] = None)

case class AnalysisIngest(
  @(ApiModelProperty@field)(required = true, value = "The Vault IDs of the input uBAMs used by this Analysis")
  input: List[String],
  @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this Analysis.")
  metadata: Map[String, String])

case class AnalysisIngestResponse(
  @(ApiModelProperty@field)(required = true, value = "The Vault ID of this Analysis")
  id: String,
  @(ApiModelProperty@field)(required = true, value = "The properties of this Analysis")
  properties: Option[Map[String, String]] = None)

case class AnalysisUpdate(
  @(ApiModelProperty@field)(required = true, value = "The files associated with this Analysis, each with a unique user-supplied string key.")
  files: Map[String, String])

case class AnalysisDMUpdate(
  @(ApiModelProperty@field)(required = true, value = "The files associated with this Analysis, each with a unique user-supplied string key.")
  files: Map[String, String],
  @(ApiModelProperty@field)(required = true, value = "The metadata key-value pairs associated with this Analysis.")
  metadata: Map[String, String])
