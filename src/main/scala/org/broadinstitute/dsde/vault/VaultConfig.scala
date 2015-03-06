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

  object SwaggerConfig {
    private val swagger = config.getConfig("swagger")
    lazy val apiVersion = swagger.getString("apiVersion")
    lazy val swaggerVersion = swagger.getString("swaggerVersion")
    lazy val info = swagger.getString("info")
    lazy val description = swagger.getString("description")
    lazy val termsOfServiceUrl = swagger.getString("termsOfServiceUrl")
    lazy val contact = swagger.getString("contact")
    lazy val license = swagger.getString("license")
    lazy val licenseUrl = swagger.getString("licenseUrl")
    lazy val baseUrl = swagger.getString("baseUrl")
    lazy val apiDocs = swagger.getString("apiDocs")
  }

  object DataManagement {
    private val dm = config.getConfig("dm")
    lazy val ubamsUrl = dm.getString("ubamsUrl")
    def uBamResolveUrl(id: String) = ubamsUrl + "/" + id
    def uBamRedirectUrl(id: String, fileType: String) = ubamsUrl + "/" + id + "/" + fileType
  }

  object BOSS {
    private val boss = config.getConfig("boss")
    lazy val objectsUrl = boss.getString("objectsUrl")
    lazy val objectResolvePath = boss.getString("objectResolvePath")
    def objectResolveUrl(id: String) = objectsUrl + id + objectResolvePath
    lazy val defaultUser = boss.getString("defaultUser")
    lazy val defaultStoragePlatform = boss.getString("defaultStoragePlatform")
    lazy val defaultValidityPeriodSeconds = boss.getInt("defaultValidityPeriodSeconds")
  }

}
