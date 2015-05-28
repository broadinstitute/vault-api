package org.broadinstitute.dsde.vault

import com.typesafe.config.ConfigFactory

object VaultConfig {
  private val config = ConfigFactory.load()

  object HttpConfig {
    private val httpConfig = config.getConfig("http")
    lazy val interface = httpConfig.getString("interface")
    lazy val port = httpConfig.getInt("port")
    lazy val timeoutSeconds = httpConfig.getLong("timeoutSeconds")

  }

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

  object Vault {
    private val vault = config.getConfig("vault")
    lazy val server = vault.getString("server")

    lazy val analysisIngestPath = vault.getString("analysesIngestPath")
    lazy val analysisIngestUrl = server + analysisIngestPath

    def analysisDescribePath(id: String) = vault.getString("analysesDescribePath").format(id)
    def analysisDescribeUrl(id: String) = server + analysisDescribePath(id)

    def analysisUpdatePath(id: String) = vault.getString("analysesUpdatePath").format(id)
    def analysisUpdateUrl(id: String) = server + analysisUpdatePath(id)

    def analysisRedirectPath(id: String, fileType: String) = vault.getString("analysesRedirectPath").format(id, fileType)
    def analysisRedirectUrl(id: String, fileType: String) = server + analysisRedirectPath(id, fileType)

    def lookupPath(entityType: String, attributeName: String, attributeValue: String) = vault.getString("lookupPath").format(entityType, attributeName, attributeValue)
    def lookupUrl(entityType: String, attributeName: String, attributeValue: String) = server + lookupPath(entityType, attributeName, attributeValue)

    lazy val ubamIngestPath = vault.getString("ubamsIngestPath")
    lazy val ubamIngestUrl = server + ubamIngestPath

    def ubamDescribePath(id: String) = vault.getString("ubamsDescribePath").format(id)
    def ubamDescribeUrl(id: String) = server + ubamDescribePath(id)

    def ubamRedirectPath(id: String, fileType: String) = vault.getString("ubamsRedirectPath").format(id, fileType)
    def ubamRedirectUrl(id: String, fileType: String) = server + ubamRedirectPath(id, fileType)

    def ubamCollectionDescribePath(id: String, version: Int) = vault.getString("ubamCollectionsDescribePath").format(version, id)
    def ubamCollectionDescribeUrl(id: String, version: Int) = server + ubamCollectionDescribePath(id, version)

    def ubamCollectionIngestPath(version: Int) = vault.getString("ubamCollectionsIngestPath").format(version)
    def ubamCollectionIngestUrl(version: Int) = server + ubamIngestPath(version)

    def genericIngestPath(version: Int) = vault.getString("genericIngestPath").format(version)
    def genericIngestUrl(version: Int) = server + genericIngestPath(version)

    def genericDescribePath(version: Int, id: String) = vault.getString("genericDescribePath").format(version, id)
    def genericDescribeUrl(version: Int, id: String) = server + genericDescribePath(version, id)

    def genericDescribeUpPath(version: Int, id: String) = vault.getString("genericDescribeUpPath").format(version, id)
    def genericDescribeUpUrl(version: Int, id: String) = server + genericDescribeUpPath(version, id)

    def genericDescribeDownPath(version: Int, id: String) = vault.getString("genericDescribeDownPath").format(version, id)
    def genericDescribeDownUrl(version: Int, id: String) = server + genericDescribeDownPath(version, id)

    def genericSearchPath(version: Int) = vault.getString("genericSearchPath").format(version)
    def genericSearchUrl(version: Int) = server + genericSearchPath(version)
  }

  object DataManagement {
    private val dm = config.getConfig("dm")
    lazy val server = dm.getString("server")

    lazy val ubamsPath = dm.getString("ubamsPath")
    lazy val ubamsUrl = server + ubamsPath

    def uBamResolvePath(id: String) = dm.getString("ubamsResolvePath").format(id)
    def uBamResolveUrl(id: String) = server + uBamResolvePath(id)
    lazy val analysesPath = dm.getString("analysesPath")
    lazy val analysesUrl = server + analysesPath

    def analysesResolvePath(id: String) = dm.getString("analysesResolvePath").format(id)
    def analysesResolveUrl(id: String) = server + analysesResolvePath(id)

    def analysesUpdatePath(id: String) = dm.getString("analysesUpdatePath").format(id)
    def analysesUpdateUrl(id: String) = server + analysesUpdatePath(id)

    def uBamCollectionResolvePath(id: String, version: Int) = dm.getString("uBamCollectionsResolvePath").format(version, id)
    def uBamCollectionResolveUrl(id: String, version: Int) = server + uBamCollectionResolvePath(id, version)

    def collectionsPath(version: Int) = dm.getString("ubamCollectionsPath").format(version)
    def collectionsUrl(version: Int) = server + collectionsPath(version)

    def queryLookupPath(entityType: String, attributeName: String, attributeValue: String) = dm.getString("queryLookupPath").format(entityType, attributeName, attributeValue)
    def queryLookupUrl(entityType: String, attributeName: String, attributeValue: String) = server + queryLookupPath(entityType, attributeName, attributeValue)

    def genericIngestPath(version: Int) = dm.getString("genericIngestPath").format(version)
    def genericIngestUrl(version: Int) = server + genericIngestPath(version)

    def genericDescribePath(version: Int, id: String) = dm.getString("genericDescribePath").format(version, id)
    def genericDescribeUrl(version: Int, id: String) = server + genericDescribePath(version, id)

    def genericDescribeUpPath(version: Int, id: String) = dm.getString("genericDescribeUpPath").format(version, id)
    def genericDescribeUpUrl(version: Int, id: String) = server + genericDescribeUpPath(version, id)

    def genericDescribeDownPath(version: Int, id: String) = dm.getString("genericDescribeDownPath").format(version, id)
    def genericDescribeDownUrl(version: Int, id: String) = server + genericDescribeDownPath(version, id)

    def genericSearchPath(version: Int) = dm.getString("genericSearchPath").format(version)
    def genericSearchUrl(version: Int) = server + genericSearchPath(version)
}

  object BOSS {
    private val boss = config.getConfig("boss")
    lazy val server = boss.getString("server")

    lazy val objectsPath = boss.getString("objectsPath")
    lazy val objectsUrl = server + objectsPath

    def objectResolvePath(id: String) = boss.getString("objectResolvePath").format(id)
    def objectResolveUrl(id: String) = server + objectResolvePath(id)

    lazy val defaultStoragePlatform = boss.getString("defaultStoragePlatform")
    lazy val defaultValidityPeriodSeconds = boss.getInt("defaultValidityPeriodSeconds")
    lazy val bossUser = boss.getString("bossUser")
    lazy val bossUserPassword = boss.getString("bossUserPassword")
  }

  object OpenAm {
    private val openam = config.getConfig("openam")
    lazy val testUser = openam.getString("testUser")
    lazy val testUserPassword = openam.getString("testUserPassword")
    lazy val tokenUrl = openam.getString("tokenUrl")
  }

}
