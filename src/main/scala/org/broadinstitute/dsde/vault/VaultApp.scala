package org.broadinstitute.dsde.vault

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.slf4j.LoggerFactory
import spray.can.Http

import scala.concurrent.duration._

object VaultApp {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("vault-api")

  // create and start our service actor
  val service = system.actorOf(Props[VaultServiceActor], "vault-api-service")

  implicit val timeout = Timeout(5.seconds)

  lazy val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    log.info("Vault instance starting.")
    start()
  }

  def start(): Unit = {
    // start a new HTTP server on configuration port with our service actor as the handler
    IO(Http) ? Http.Bind(service, VaultConfig.HttpConfig.interface, VaultConfig.HttpConfig.port)
  }

}
