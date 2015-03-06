package org.broadinstitute.dsde.vault.model

import org.broadinstitute.dsde.vault.VaultConfig
import spray.json.DefaultJsonProtocol

object BossJsonProtocol extends DefaultJsonProtocol {
  implicit val creationRequest = jsonFormat7(BossCreationRequest)
  implicit val creation = jsonFormat8(BossCreationObject)
  implicit val resolutionRequest = jsonFormat4(BossResolutionRequest)
  implicit val resolution = jsonFormat4(BossResolutionResponse)
}

// TODO? use ObjectResource in the BOSS repo directly
case class BossCreationRequest(
  objectName: String,
  storagePlatform: String,
  directoryPath: Option[String] = None,
  sizeEstimateBytes: Int,
  ownerId: String,
  readers: List[String],
  writers: List[String]
)

case class BossCreationObject(
  objectId: String,
  objectName: String,
  storagePlatform: String,
  directoryPath: Option[String] = None,
  sizeEstimateBytes: Int,
  ownerId: String,
  readers: List[String],
  writers: List[String]
)

// TODO? use ResolutionRequest / ResolutionResource in the BOSS repo directly
case class BossResolutionRequest(
  validityPeriodSeconds: Int,
  httpMethod: String,
  contentType: Option[String] = None,
  contentMD5Hex: Option[String] = None
                                 )
case class BossResolutionResponse(
  validityPeriodSeconds: Int,
  contentType: Option[String] = None,
  contentMD5Hex: Option[String] = None,
  objectUrl: String
)

object BossDefaults {
  val ownerId = VaultConfig.BOSS.defaultUser
  val readers = List(ownerId)
  val writers = List(ownerId)
  val storagePlatform = VaultConfig.BOSS.defaultStoragePlatform
  val validityPeriodSeconds = VaultConfig.BOSS.defaultValidityPeriodSeconds

  def getCreationRequest(name: String): BossCreationRequest =
    new BossCreationRequest(name, storagePlatform, None, 0, ownerId, readers, writers)

  def getResolutionRequest(httpMethod: String): BossResolutionRequest =
    new BossResolutionRequest(validityPeriodSeconds, httpMethod, None, None)
}
