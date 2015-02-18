package org.broadinstitute.dsde.vault

import com.typesafe.config.ConfigFactory

object VaultConfig {
  private val config = ConfigFactory.load()

  object HttpConfig {
    private val httpConfig = config.getConfig("http")
    lazy val interface = httpConfig.getString("interface")
    lazy val port = httpConfig.getInt("port")
  }
  //Config Settings

}
