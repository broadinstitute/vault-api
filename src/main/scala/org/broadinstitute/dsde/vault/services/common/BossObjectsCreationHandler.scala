package org.broadinstitute.dsde.vault.services.common

import akka.actor.{Props, Actor, ActorRef}
import akka.event.Logging
import org.broadinstitute.dsde.vault.BossClientService.{BossObjectResolved, BossObjectCreated, BossObjectDeleted}
import org.broadinstitute.dsde.vault.BossClientService
import org.broadinstitute.dsde.vault.model._
import org.broadinstitute.dsde.vault.services.ClientFailure
import org.broadinstitute.dsde.vault.services.common.BossObjectsCreationHandler._
import scala.util.{Try,Success}

object BossObjectsCreationHandler {
  case class CreateObjectsMessage(objectsToCreate: Map[String,String], forceLocs: Boolean)
  case class ObjectsCreatedMessage(objectIDs: Map[String,String], objectURLs: Map[String,String])
  case class ObjectsCreationFailedMessage(message: String)
  case class DeleteObjectsMessage(objectsToDelete: Map[String,String])

  def props(bossService: ActorRef): Props =
    Props(new BossObjectsCreationHandler(bossService))
}

case class BossObjectsCreationHandler(bossService: ActorRef) extends Actor {

  implicit val system = context.system
  val mLog = Logging(system, getClass)

  // these first three vars are set from the initial message

  var mSender: ActorRef = null

  // the objects from the initial message
  var mObjs: Map[String, String] = Map.empty

  // the value of the optional "X-Force-Location" header from the initial message
  // if true, no need to resolve Boss objects -- just pass back the original locations
  var mForceLocs: Boolean = false

  // the next few vars are collected along the way, and tell us the status of the message processing

  // a map from file types to Boss IDs
  // one mapping is added by each BossObjectCreated message
  // consumed by DmClientService.CreateUBam when all mappings have been added
  var mBossIDs: Map[String, Try[String]] = Map.empty

  // true if any of the values in mBossIDs is a Failure
  var mBossIDsFailure: Boolean = false
  
  // a map from file types to presigned Boss PUT URLs
  // one mapping is added by each BossObjectResolved message
  // consumed by UBamIngestResponse after all mappings have been added and the DM ID has been set
  var mBossURLs: Map[String, Try[String]] = Map.empty

  def receive = {
    // State 0: process the initial message
    case CreateObjectsMessage(objectsToCreate: Map[String,String], forceLocs: Boolean) =>
      mLog.debug("Received create BOSS objects message")
      mSender = sender()
      mObjs = objectsToCreate
      mForceLocs = forceLocs
      mObjs.foreach {
        case (ftype, fpath) =>
          bossService ! BossClientService.BossCreateObject(BossDefaults.getCreationRequest(fpath,mForceLocs), ftype)
      }

    // State 1: gather the object-created messages from BOSS
    case BossObjectCreated(bossID: Try[String], creationKey: String) =>
      // add ID to map
      mBossIDs += creationKey -> bossID
      if (bossID.isFailure)
        mBossIDsFailure = true

      // Unless the client is using the "X-Force-Location" header, resolve object to a PUT URL.
      if (!mForceLocs && !mBossIDsFailure)
        bossService ! BossClientService.BossResolveObject(BossDefaults.getResolutionRequest("PUT"), bossID.get, creationKey)

      // If all Boss objects have been created
      if (mObjs.size == mBossIDs.size) {
        val failedCreateCount = mBossIDs.count { _._2.isFailure }
        // If there are errors
        if (failedCreateCount > 0) {
          nevermind("Failed to create "+failedCreateCount+" boss objects.")
        }
        // All good.  If we're not resolving objects, we're ready to tell the original requester about our successes.
        else if (mForceLocs) {
          allDone
        }
      }

    // State 2: gather the resolve URLS from BOSS (Optional: omitted when forcing locations)
    case BossObjectResolved(bossURL: Try[String], creationKey: String) =>
      mBossURLs += creationKey -> bossURL
      // If all resolves are done.
      if (mObjs.size == mBossURLs.size) {

        // This can never happen if there were errors in producing the boss IDs.
        assert(!mBossIDsFailure)
        // If will also not happen if we're forcing locations
        assert(!mForceLocs)

        // If any of the resolves failed.
        val failedResolveCount = mBossURLs.count { _._2.isFailure }
        if (failedResolveCount > 0) {
          nevermind("Failed to resolve "+failedResolveCount+" boss objects.")
        }
        else {
          allDone
        }
      }

    // State X: just fire a bunch of deletion requests
    case DeleteObjectsMessage(objectsToDelete: Map[String,String]) =>
      deleteAll(objectsToDelete)

    // Ignore these clean-up reports
    case BossObjectDeleted(bossId: Try[String], creationKey: String) =>
      None
  }

  // flatten ids from Map[String,Try[String]] to Map[String,String]
  def validIDs( map: Map[String,Try[String]] ): Map[String,String] = {
    map.collect { case (key,Success(id)) => (key,id) }
  }

  def allDone(): Unit = {
    mSender ! ObjectsCreatedMessage(validIDs(mBossIDs),validIDs(mBossURLs))
  }

  def deleteAll(ids: Map[String,String]) = {
    ids.foreach { entry => bossService ! BossClientService.BossDeleteObject(entry._2,entry._1) }
  }

  // delete whatever Boss objects you can, then fail
  def nevermind(message: String) = {
    mLog.error("Client failure: " + message)
    deleteAll(validIDs(mBossIDs))
    mSender ! ObjectsCreationFailedMessage(message)
  }
}
