package org.broadinstitute.dsde.vault

import akka.actor.Actor
import akka.event.Logging
import org.broadinstitute.dsde.vault.model.uBAM
import spray.client.pipelining._
import spray.json._
import spray.routing.RequestContext

import scala.util.{Failure, Success}

object DmClientService {
  case class QueryUBam(ubamId: String)
}

class DmClientService(requestContext: RequestContext) extends Actor {

  import org.broadinstitute.dsde.vault.DmClientService._
  import org.broadinstitute.dsde.vault.services.uBAM.DescribeJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  implicit val system = context.system
  import system.dispatcher
  val log = Logging(system, getClass)

  override def receive: Receive = {
    case QueryUBam(ubamId) =>
      queryForUBam(ubamId)
      context.stop(self)
  }

  def queryForUBam(ubamId: String): Unit = {
    log.info("Querying the DM API for a uBAM id: " + ubamId)
    val pipeline = sendReceive ~> unmarshal[uBAM]
    val responseFuture = pipeline {
      Get(VaultConfig.DataManagement.ubamsUrl + ubamId)
    }
    responseFuture onComplete {
      case Success(uBAM(id, files, meta)) =>
        log.info("uBAM found: " + ubamId)
        requestContext.complete(uBAM(id, files, meta).toJson.prettyPrint)

      case Failure(error) =>
        log.error(error, "Couldn't find uBAM with id: " + ubamId)
        requestContext.complete(error)
    }
  }

}
