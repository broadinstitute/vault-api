package org.broadinstitute.dsde.vault

import java.util.concurrent.TimeUnit

import akka.testkit.TestActorRef
import akka.util.Timeout
import akka.pattern.ask
import org.broadinstitute.dsde.vault.OpenAmClientService.{OpenAmResponse, OpenAmAuthRequest}
import org.scalatest._
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.testkit.ScalatestRouteTest

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.util.Try

abstract class VaultFreeSpec extends FreeSpec with Matchers with VaultCustomMatchers with OptionValues with Inside with Inspectors with ScalatestRouteTest {

  implicit val timeout = Timeout(5, TimeUnit.SECONDS)
  val duration: Duration = new FiniteDuration(5, TimeUnit.SECONDS)
  implicit val routeTestTimeout = RouteTestTimeout(new FiniteDuration(10, TimeUnit.SECONDS))

  def getOpenAmToken: Option[OpenAmResponse] = {
    val actor = TestActorRef[OpenAmClientService]
    val future = actor ? OpenAmAuthRequest(VaultConfig.OpenAm.testUser, VaultConfig.OpenAm.testUserPassword)
    Some(Await.result(future, duration).asInstanceOf[OpenAmResponse])
  }

  lazy val openAmResponse = getOpenAmToken.get
  def addOpenAmCookie: RequestTransformer = {
    Cookie(HttpCookie("iPlanetDirectoryPro", openAmResponse.tokenId))
  }

}

// enables tests for "should be a UUID" etc
// see org.scalatest.matchers.BePropertyMatcher for more information

trait VaultCustomMatchers {
  class UUIDBePropertyMatcher extends BePropertyMatcher[String] {
    def apply(left: String) = BePropertyMatchResult(Try(java.util.UUID.fromString(left)).isSuccess, "UUID")
  }

  class URLBePropertyMatcher extends BePropertyMatcher[String] {
    def apply(left: String) = BePropertyMatchResult(Try(new java.net.URL(left)).isSuccess, "URL")
  }

  val UUID = new UUIDBePropertyMatcher
  val URL = new URLBePropertyMatcher
}
