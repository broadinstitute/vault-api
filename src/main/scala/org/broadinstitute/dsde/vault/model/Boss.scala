package org.broadinstitute.dsde.vault.model

import org.broadinstitute.dsde.vault.VaultConfig
import spray.json.DefaultJsonProtocol

object BossJsonProtocol extends DefaultJsonProtocol {
  implicit val impBossCreationObject = jsonFormat9(BossCreationObject)
  implicit val impBossResolutionRequest = jsonFormat4(BossResolutionRequest)
  implicit val impBossResolutionResponse = jsonFormat4(BossResolutionResponse)
}

case class BossCreationObject(
  objectName: String,
  storagePlatform: String,
  sizeEstimateBytes: Int,
  ownerId: String,
  readers: List[String],
  writers: List[String],
  objectId: Option[String] = None,
  directoryPath: Option[String] = None,
  forceLocation: Option[Boolean] = None
)

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
  val ownerId = VaultConfig.BOSS.bossUser
  val readers = List(ownerId)
  val writers = List(ownerId)
  val storagePlatform = VaultConfig.BOSS.defaultStoragePlatform
  val validityPeriodSeconds = VaultConfig.BOSS.defaultValidityPeriodSeconds

  def getCreationRequest(fpath: String, forceLocation: Option[String]): BossCreationObject = {
    forceLocation.getOrElse("false").toBoolean match {
      case true =>
        new BossCreationObject(fpath, storagePlatform, 0, ownerId, readers, writers, Option.empty, Option(fpath), Option(true))
      case false =>
        new BossCreationObject(fpath, storagePlatform, 0, ownerId, readers, writers)
    }
  }

  def getResolutionRequest(httpMethod: String): BossResolutionRequest =
    new BossResolutionRequest(validityPeriodSeconds, httpMethod, None, None)
}
