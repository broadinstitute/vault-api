package org.broadinstitute.dsde.vault.services.mock

import org.broadinstitute.dsde.vault.services.analysis.{AnalysisDescribeServiceSpec, AnalysisIngestServiceSpec, AnalysisRedirectServiceSpec, AnalysisUpdateServiceSpec}
import org.broadinstitute.dsde.vault.services.lookup.LookupServiceSpec
import org.broadinstitute.dsde.vault.services.uBAM.{UBamDescribeServiceSpec, UBamIngestServiceSpec, UBamRedirectServiceSpec}
import org.broadinstitute.dsde.vault.services.uBAMCollection.{UBamCollectionDescribeServiceSpec, UBamCollectionIngestServiceSpec}
import org.scalatest.{BeforeAndAfterAll, Suites}

class ServiceSuite extends Suites(
  new AnalysisDescribeServiceSpec,
  new AnalysisIngestServiceSpec,
  new AnalysisRedirectServiceSpec,
  new AnalysisUpdateServiceSpec,
  new LookupServiceSpec,
  new UBamDescribeServiceSpec,
  new UBamIngestServiceSpec,
  new UBamRedirectServiceSpec,
  new UBamCollectionDescribeServiceSpec,
  new UBamCollectionIngestServiceSpec
) with BeforeAndAfterAll{

  override def beforeAll(): Unit = {
    Servers.startUpDM()
    Servers.startUpBoss()
  }

  override def afterAll(): Unit = {
    Servers.stopServers()
  }
}