package org.broadinstitute.dsde.vault

import akka.actor.{ActorSystem, Props}
import org.broadinstitute.dsde.vault.common.util.ServerInitializer
import org.slf4j.LoggerFactory

object VaultApp {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("vault-api")
  lazy val log = LoggerFactory.getLogger(getClass)
  def main(args: Array[String]) {
    log.info("Vault instance starting.")
    ServerInitializer.startWebServiceActors(Props[VaultServiceActor],VaultConfig.HttpConfig.interface,VaultConfig.HttpConfig.port,VaultConfig.HttpConfig.timeout,system)
  }
}
