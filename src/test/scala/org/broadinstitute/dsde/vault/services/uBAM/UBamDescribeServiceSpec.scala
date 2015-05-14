package org.broadinstitute.dsde.vault.services.uBAM

import org.broadinstitute.dsde.vault.{VaultConfig, VaultFreeSpec}
import org.broadinstitute.dsde.vault.model.{UBamIngestResponse, UBamIngest, UBam}
import org.broadinstitute.dsde.vault.model.uBAMJsonProtocol._
import spray.http.HttpCookie
import spray.http.HttpHeaders.Cookie
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._

class UBamDescribeServiceSpec extends VaultFreeSpec with UBamDescribeService with UBamIngestService with UBamDescribeListService {

  override val routes = uBamDescribeRoute ~  uBamDescribeListRoute

  def actorRefFactory = system

  var testingId = "invalid_UUID"

  val files = Map(("bam", "vault/test/test.bam"), ("bai", "vault/test/test.bai"))
  val metadata = Map("testAttr" -> "testValue")

  val versions = Table(
    "version",
    None,
    Some(1)
  )

  val pageLimits = Table(
    "pageLimit",
    0,
    1
  )

  "UBamDescribeServiceSpec" - {
    forAll(versions) { (version: Option[Int]) =>

      s"when accessing version = '${v(version)}'" - {

        "while preparing the ubam test data" - {
          "should successfully store the data using the UBam Ingest path" in {
            val ubamIngest = new UBamIngest(files, metadata)
            Post(VaultConfig.Vault.ubamIngestPath.versioned(version), ubamIngest) ~> addOpenAmCookie ~> uBamIngestRoute ~> check {
              status should equal(OK)
              testingId = responseAs[UBamIngestResponse].id
            }
          }
        }

        "when calling GET to the UBam Describe path with a Vault ID" - {
          "should return that ID" in {
            Get(VaultConfig.Vault.ubamDescribePath(testingId).versioned(version)) ~> addOpenAmCookie ~> uBamDescribeRoute ~> check {
              status should equal(OK)
              val response = responseAs[UBam]
              response.id should be(testingId)
              response.metadata should equal(Map("testAttr" -> "testValue"))
              response.files.getOrElse("bam", "error") should (be an (URL) and not be a(UUID))
              response.files.getOrElse("bai", "error") should (be an (URL) and not be a(UUID))
              response.files.getOrElse("bam", "error") shouldNot include("ingest")
              response.files.getOrElse("bai", "error") shouldNot include("ingest")
            }
          }
        }

        "when calling GET to the UBam Describe path with an unknown Vault ID" - {
          "should return a 404 not found error" in {
            Get(VaultConfig.Vault.ubamDescribePath("12345-67890-12345").versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamDescribeRoute) ~> check {
              status should equal(NotFound)
            }
          }
        }

        "when calling PUT to the UBam Describe path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Put(VaultConfig.Vault.ubamDescribePath(testingId).versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamDescribeRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "when calling POST to the UBam Describe path with a Vault ID" - {
          "should return a MethodNotAllowed error" in {
            Post(VaultConfig.Vault.ubamDescribePath("arbitrary_id").versioned(version)) ~> addOpenAmCookie ~> sealRoute(uBamDescribeRoute) ~> check {
              status should equal(MethodNotAllowed)
              entity.toString should include("HTTP method not allowed, supported methods: GET")
            }
          }
        }

        "when calling list UBam Describe path " - {
          forAll(pageLimits) { (pageLimit: Int) =>
            s"should return a list of size $pageLimit with ?page[limit]=$pageLimit or not be handled" in {
              if (version.isDefined) {
                Get(s"${VaultConfig.Vault.ubamIngestPath + v(version)}?page[limit]=$pageLimit") ~> addOpenAmCookie ~> uBamDescribeListRoute ~> check {
                  val responses = responseAs[List[UBam]]
                  responses should have size pageLimit
                  responses.foreach{ unmappedBAM =>
                    unmappedBAM.id shouldNot be(empty)
                    /* uncomment once properties are properly passed through when using v2 endpoints */
                    /*
                    version match {
                      case Some(x) if x > 1 =>
                        unmappedBAM.properties.get.get("createdBy") shouldNot be(empty)
                        unmappedBAM.properties.get.get("createdDate") shouldNot be(empty)
                      case _ => unmappedBAM.properties should be(empty)
                    }
                    */
                  }
                }
              } else {
                Get(s"${VaultConfig.Vault.ubamIngestPath + v(version)}?page[limit]=$pageLimit") ~> addOpenAmCookie ~> sealRoute(uBamDescribeListRoute) ~> check {
                  status should be(NotFound)
                }
              }
            }
          }
        }
      }
    }
  }

}
