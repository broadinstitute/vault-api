package org.broadinstitute.dsde.vault

import org.scalatest._
import spray.testkit.ScalatestRouteTest

abstract class VaultFreeSpec extends FreeSpec with Matchers with OptionValues with Inside with Inspectors with ScalatestRouteTest