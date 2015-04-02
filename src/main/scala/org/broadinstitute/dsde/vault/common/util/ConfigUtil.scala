package org.broadinstitute.dsde.vault.common.util

import com.typesafe.config.Config

object ConfigUtil {
  def getIntOrElse(config: Config, path: String, default: Int): Int = {
    if (config.hasPath(path)) config.getInt(path) else default
  }

  def getStringOrElse(config: Config, path: String, default: String): String = {
    if (config.hasPath(path)) config.getString(path) else default
  }

  def getIntOption(config: Config, path: String, default: Option[Int] = None): Option[Int] = {
    if (config.hasPath(path)) Option(config.getInt(path)) else default
  }

  def getStringOption(config: Config, path: String, default: Option[String] = None): Option[String] = {
    if (config.hasPath(path)) Option(config.getString(path)) else default
  }
}
