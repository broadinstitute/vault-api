package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations.ApiModelProperty
import spray.json

import scala.annotation.meta.field

object uBAMCollectionJsonProtocol extends json.DefaultJsonProtocol {
  implicit val impUBamCollection = jsonFormat4(UBamCollection)
  implicit val impUBamCollectionIngest = jsonFormat2(UBamCollectionIngest)
  implicit val impUBamCollectionIngestResponse = jsonFormat4(UBamCollectionIngestResponse)
}

case class UBamCollection
(
  @(ApiModelProperty@field)(value = "The Vault ID of this uBAM collection", required = true)
  id: Option[String] = None,
  @(ApiModelProperty@field)(value = "The Vault IDs of the uBAMs included in this collection.", required = true)
  members: Option[Seq[String]] = None,
  @(ApiModelProperty@field)(value = "The metadata key-value pairs associated with this uBAM collection.", required = true)
  metadata: Map[String, String],
  @(ApiModelProperty@field)(value = "The properties associated with this uBAM collection.", required = true)
  properties: Map[String, String]
)

case class UBamCollectionIngest
(
  @(ApiModelProperty@field)(value = "The Vault IDs of the uBAMs included in this collection.", required = true)
  members: Option[Seq[String]] = None,
  @(ApiModelProperty@field)(value = "The metadata key-value pairs associated with this uBAM collection.", required = true)
  metadata: Map[String, String]
)

case class UBamCollectionIngestResponse
(
  @(ApiModelProperty@field)(value = "The Vault ID of this uBAM collection", required = true)
  id: String,
  @(ApiModelProperty@field)(value = "The Vault IDs of the uBAMs included in this collection.", required = true)
  members: Option[Seq[String]] = None,
  @(ApiModelProperty@field)(value = "The metadata key-value pairs associated with this uBAM collection.", required = true)
  metadata: Map[String, String],
  @(ApiModelProperty@field)(value = "The properties associated with this uBAM collection.", required = true)
  properties: Map[String, String]
)