package org.broadinstitute.dsde.vault.client

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import org.broadinstitute.dsde.vault.OpenAmClientService.{OpenAmAuthRequest, OpenAmResponse}
import org.broadinstitute.dsde.vault.services.uBAM.ClientFailure
import org.broadinstitute.dsde.vault.{OpenAmClientService, VaultConfig}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

class OpenAmClientSpec extends TestKit(ActorSystem()) with WordSpecLike with Matchers {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS) // Required for actor sendReceive
  val duration: Duration = new FiniteDuration(5, TimeUnit.SECONDS)

  "The OpenAmClientService Actor" should {
    "return a valid token id" in {
      val actor = TestActorRef[OpenAmClientService]
      val future = actor ? OpenAmAuthRequest(VaultConfig.OpenAm.testUser, VaultConfig.OpenAm.testUserPassword)
      val result = Await.result(future, duration)
      result shouldNot be (None)
      result.asInstanceOf[OpenAmResponse].tokenId shouldNot be (null)
      result.asInstanceOf[OpenAmResponse].successUrl shouldNot be (null)
    }

    "return a failure message when passed invalid credentials in" in {
      val actor = TestActorRef[OpenAmClientService]
      val future = actor ? OpenAmAuthRequest("fakeUser", "fakePassword")
      val result = Await.result(future, duration)
      result shouldBe a [ClientFailure]
      result.toString should include ("Unauthorized")
    }
  }

}
