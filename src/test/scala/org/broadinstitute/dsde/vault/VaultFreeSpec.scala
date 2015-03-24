package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.testkit.TestActorRef
import akka.util.Timeout
import akka.pattern.ask
import org.broadinstitute.dsde.vault.OpenAmClientService.{OpenAmResponse, OpenAmAuthRequest}
import org.scalatest._
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, Duration}

abstract class VaultFreeSpec extends FreeSpec with Matchers with OptionValues with Inside with Inspectors with ScalatestRouteTest {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  val duration: Duration = new FiniteDuration(5, TimeUnit.SECONDS)
  implicit val routeTestTimeout = RouteTestTimeout(new FiniteDuration(10, TimeUnit.SECONDS))

  def getOpenAmToken: Option[OpenAmResponse] = {
    val actor = TestActorRef[OpenAmClientService]
    val future = actor ? OpenAmAuthRequest(VaultConfig.OpenAm.testUser, VaultConfig.OpenAm.testUserPassword)
    Some(Await.result(future, duration).asInstanceOf[OpenAmResponse])
  }

}

