package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.VaultFreeSpec
import spray.http.StatusCodes._

case class DescribeServiceSpec extends VaultFreeSpec with DescribeService {

  def actorRefFactory = system
  val path = "/ubams"

  "DescribeuBAMService" - {
    "when calling GET to the " + path + " path with a Vault ID" - {
      "should return that ID" in {
        Get(path + "/arbitrary_id") ~> describeRoute ~> check {
          status should equal(OK)
          entity.toString should include("arbitrary_id")
        }
      }
    }

    "when calling PUT to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Put(path + "/arbitrary_id") ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

    "when calling POST to the " + path + " path with a Vault ID" - {
      "should return a MethodNotAllowed error" in {
        Post(path + "/arbitrary_id") ~> sealRoute(describeRoute) ~> check {
          status should equal(MethodNotAllowed)
          entity.toString should include("HTTP method not allowed, supported methods: GET")
        }
      }
    }

  }

}

