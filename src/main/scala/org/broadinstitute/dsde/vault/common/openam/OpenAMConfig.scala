package org.broadinstitute.dsde.vault.common.openam

import com.typesafe.config.ConfigFactory
import org.broadinstitute.dsde.vault.common.util.ConfigUtil

object OpenAMConfig {
  private val openAMConfig = ConfigFactory.load().getConfig("openam")
  lazy val deploymentUri = openAMConfig.getString("deploymentUri")
  lazy val username = openAMConfig.getString("username")
  lazy val password = openAMConfig.getString("password")
  lazy val realm = ConfigUtil.getStringOption(openAMConfig, "realm")
  lazy val authIndexType = ConfigUtil.getStringOption(openAMConfig, "authIndex.type")
  lazy val authIndexValue =  ConfigUtil.getStringOption(openAMConfig, "authIndex.value")
  lazy val tokenCookie = ConfigUtil.getStringOrElse(openAMConfig, "tokenCookie", "iPlanetDirectoryPro")
  lazy val timeoutSeconds = ConfigUtil.getIntOrElse(openAMConfig, "timeoutSeconds", 5)
}
