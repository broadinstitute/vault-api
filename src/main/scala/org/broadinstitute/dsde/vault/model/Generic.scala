package org.broadinstitute.dsde.vault.model

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}
import spray.json.DefaultJsonProtocol
import scala.annotation.meta.field

object GenericJsonProtocol extends DefaultJsonProtocol {
  implicit val impGenericSysAttrs = jsonFormat5(GenericSysAttrs)
  implicit val impGenericEntity = jsonFormat6(GenericEntity)
  implicit val impGenericRelationship = jsonFormat2(GenericRelationship)
  implicit val impGenericRelEnt = jsonFormat2(GenericRelEnt)
  implicit val impDataObjectSpecification = jsonFormat2(DataObjectSpecification)
  implicit val impGenericEntityIngest = jsonFormat4(GenericEntityIngest)
  implicit val impGenericRelationshipIngest = jsonFormat4(GenericRelationshipIngest)
  implicit val impGenericIngest = jsonFormat2(GenericIngest)
  implicit val impGenericAttributeSpec = jsonFormat2(GenericAttributeSpec)
  implicit val impGenericQuery = jsonFormat3(GenericEntityQuery)
}

@ApiModel("system-generated entity attributes")
case class GenericSysAttrs(
  @(ApiModelProperty@field)("optional BOSS ID")
  bossID: Option[String],
  @(ApiModelProperty@field)("when the entity was created (mSecs since epoch)")
  createdDate: Long,
  @(ApiModelProperty@field)("openAM commonName of entity creator")
  createdBy: String,
  @(ApiModelProperty@field)(value="when the entity was most recently modified (mSecs since epoch)",dataType="Long")
  modifiedDate: Option[Long],
  @(ApiModelProperty@field)("openAM commonName of most recent modifier")
  modifiedBy: Option[String] )

@ApiModel("something that lives in the vault")
case class GenericEntity(
  @(ApiModelProperty@field)("the globally unique vault ID for the entity")
  guid: String,
  @(ApiModelProperty@field)("a signed PUT URL for time-limited ingest of BOSS objects")
  signedPutUrl: Option[String],
  @(ApiModelProperty@field)("a signed GET URL for time-limited retrieval of BOSS objects")
  signedGetUrl: Option[String],
  @(ApiModelProperty@field)("the entity type (open vocabulary - not enumerated)")
  entityType: String,
  @(ApiModelProperty@field)("some system-generated 'when's and 'who's")
  sysAttrs: GenericSysAttrs,
  @(ApiModelProperty@field)("an open set of metadata attributes of the entity")
  attrs: Option[Map[String,String]] )

@ApiModel("a directed relationship between two entities")
case class GenericRelationship(
  @(ApiModelProperty@field)("the type of relationship (open vocabulary - not enumerated)")
  relationType: String,
  @(ApiModelProperty@field)("an open set of metadata attributes of the relationship")
  attrs: Option[Map[String,String]] )

@ApiModel("a description of a relationship and of the entity targeted by the relationship")
case class GenericRelEnt(
  @(ApiModelProperty@field)("the relationship")
  relationship: GenericRelationship,
  @(ApiModelProperty@field)("the entity so-related")
  entity: GenericEntity )

@ApiModel("how to resolve a Data Object in BOSS")
case class DataObjectSpecification(
  @(ApiModelProperty@field)("how to perform ingest (TODO, beginning with new = 'create new object')")
  ingestMethod: String,
  @(ApiModelProperty@field)("source of file for ingest (TODO)")
  source: Option[String] )

@ApiModel("a new entity to create")
case class GenericEntityIngest(
  @(ApiModelProperty@field)("the entity type (open vocabulary - not enumerated)")
  entityType: String,
  @(ApiModelProperty@field)("optional BOSS ID")
  bossID: Option[String],
  @(ApiModelProperty@field)("optional Data Object specification")
  dataObject: Option[DataObjectSpecification],
  @(ApiModelProperty@field)("an open set of metadata attributes of the entity")
  attrs: Map[String,String] )

@ApiModel("a new relationship to create")
case class GenericRelationshipIngest(
  @(ApiModelProperty@field)("the type of relationship (open vocabulary - not enumerated)")
  relationType: String,
  @(ApiModelProperty@field)("the vault ID of the upstream entity (or a reference of the form $0, $1, $2... to refer to an entity that will be created as a part of this ingest)")
  ent1: String,
  @(ApiModelProperty@field)("the vault ID of the downstream entity (or a reference of the form $0, $1, $2... to refer to an entity that will be created as a part of this ingest)")
  ent2: String,
  @(ApiModelProperty@field)("an open set of metadata attributes of the relationship")
  attrs: Map[String,String] )

@ApiModel("a small chunk of a client's object model to be created all at once")
case class GenericIngest(
  @(ApiModelProperty@field)("some entities to create")
  entities: Option[List[GenericEntityIngest]],
  @(ApiModelProperty@field)("some relationships to create among the new (or previously existing) entities")
  relations: Option[List[GenericRelationshipIngest]] )

@ApiModel("a metadata attribute name and value")
case class GenericAttributeSpec(
  @(ApiModelProperty@field)("the name")
  name: String,
  @(ApiModelProperty@field)("the value")
  value: String )

@ApiModel("a query for a type of entity with optional metadata attribute value")
case class GenericEntityQuery(
  @(ApiModelProperty@field)("the entity type")
  entityType: String,
  @(ApiModelProperty@field)("optional metadata attribute spec")
  attrSpec: Seq[GenericAttributeSpec],
  @(ApiModelProperty@field)("return metadata attributes, or skip it")
  expandAttrs: Boolean )
