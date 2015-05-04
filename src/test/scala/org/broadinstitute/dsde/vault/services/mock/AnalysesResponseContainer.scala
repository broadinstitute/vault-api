package org.broadinstitute.dsde.vault.services.mock

import org.broadinstitute.dsde.vault.model.Analysis

object AnalysesResponseContainer{

  val emptyAnalysis = new Analysis (
    "9b66665c-f41a-11e4-b9b2-1697f925ec7b",
    input = List(),
    files = Some(Map()),
    metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
  )

  val updatedAnalysis = new Analysis (
    "9b66665c-f41a-11e4-b9b2-1697f925ec7b",
    input = List(),
    files = Some(Map("vcf" -> "vault/test/test.vcf", "bai" -> "vault/test/test.bai", "bam" -> "vault/test/test.bam")),
    metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
  )

  var responseAnalysis = new Analysis(
    "9b66665c-f41a-11e4-b9b2-1697f925ec7b",
    input = List(),
    files = Some(Map()),
    metadata = Map("testAttr" -> "testValue", "randomData" -> "7")
  );

  responseAnalysis = emptyAnalysis

  def setEmptyResponse(): Unit = {
    responseAnalysis = emptyAnalysis
  }

  def setUpdatedResponse(): Unit = {
    responseAnalysis = updatedAnalysis
  }
}
