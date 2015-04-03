package org.broadinstitute.dsde.vault.common.openam

import java.io.ByteArrayOutputStream

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, LoggerContext}
import ch.qos.logback.core.OutputStreamAppender
import ch.qos.logback.core.encoder.EchoEncoder
import org.broadinstitute.dsde.vault.common.openam.OpenAMDirectives._
import org.scalatest.{FreeSpec, Matchers}
import org.slf4j.LoggerFactory
import spray.http.HttpHeaders.`User-Agent`
import spray.http.StatusCodes._
import spray.http.Uri.{Path, Query}
import spray.http._
import spray.routing.Directives._
import spray.routing.MissingCookieRejection
import spray.testkit.ScalatestRouteTest
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._

class OpenAMDirectivesSpec extends FreeSpec with Matchers with ScalatestRouteTest {

  val OkResponse = HttpResponse()
  val completeOk = complete(OkResponse)

  "OpenAMClient" - {

    "when accessing OpenAM logOpenAMRequest directive" - {

      val commonNameQuoted = """"%s"""".format(OpenAMConfig.commonName.replaceAll("\"", "\"\""))

      // A fixture for loaning: http://doc.scalatest.org/2.0/org/scalatest/FreeSpec.html#loanFixtureMethods
      // Based on http://stackoverflow.com/questions/5448673/slf4j-logback-how-to-configure-loggers-in-runtime/5715581#5715581
      class TestLogger {
        // may fail due to race condition in SLF4J.  See build.sbt for details/workaround
        private val context = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
        private val encoder = new EchoEncoder[ILoggingEvent]
        private val logByteStream = new ByteArrayOutputStream(512)
        private val logger = OpenAMDirectives.logOpenAMRequestLogger.asInstanceOf[ch.qos.logback.classic.Logger]

        private val streamAppender = new OutputStreamAppender[ILoggingEvent] {
          override def start() {
            logByteStream.reset()
            setOutputStream(logByteStream)
            super.start()
          }
        }

        // Initialize the logger
        private val originalLevel = logger.getLevel
        encoder.setContext(context)
        encoder.start()
        streamAppender.setEncoder(encoder)
        streamAppender.setContext(context)
        streamAppender.start()
        logger.addAppender(streamAppender)
        logger.setLevel(Level.INFO)

        def setLevel(level: Level) = logger.setLevel(level)

        def messages = logByteStream.toString

        def cleanup() {
          logger.detachAppender(streamAppender)
          logger.setLevel(originalLevel)
        }
      }

      def withTestLogger[T](testCode: (TestLogger) => T): T = {
        val testLogger = new TestLogger
        val result = testCode(testLogger)
        testLogger.cleanup()
        result
      }

      "should log the request" in withTestLogger { logger =>
        Get("/hello") ~> OpenAMSession ~> logOpenAMRequest {
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
          logger.messages should be("[INFO] %s GET http://example.com/hello%n".format(commonNameQuoted))
        }
      }

      "should log the request with query parameters" in withTestLogger { logger =>
        Get(Uri(path = Path("/hello"), query = Query("key1" -> "value1"))) ~>
          OpenAMSession ~> logOpenAMRequest {
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
          logger.messages should be("[INFO] %s GET http://example.com/hello?key1=value1%n".format(commonNameQuoted))
        }
      }

      "should log the post body when debug is enabled" in withTestLogger { logger =>
        logger.setLevel(Level.DEBUG)
        Post("/hello", Map.empty[String, String]) ~> OpenAMSession ~> logOpenAMRequest {
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
          logger.messages should be("[INFO] %s POST http://example.com/hello%n{%n%n}%n".format(commonNameQuoted))
        }
      }

      "should not log the post body when debug is not enabled" in withTestLogger { logger =>
        Post("/hello", Map.empty[String, String]) ~> OpenAMSession ~> logOpenAMRequest {
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
          logger.messages should be("[INFO] %s POST http://example.com/hello%n".format(commonNameQuoted))
        }
      }

      "should log the request without headers" in withTestLogger { logger =>
        Get("/hello") ~> OpenAMSession ~>
          // NOTE: Still unsure how to simply add a header to a spray-testkit.
          //   Based the snippet including "headerValuePF ..." from HeaderDirectivesSpec. Example:
          //   https://github.com/spray/spray/blob/340614c9af7facf6a8b95bf4cc7733e0fca9299e/spray-routing-tests/src/test/scala/spray/routing/HeaderDirectivesSpec.scala#L81
          `User-Agent`("custom-agent") ~> headerValuePF { case h => h } { h =>
          logOpenAMRequest {
            completeOk
          }
        } ~>
          check {
            status should be(OK)
            response should be(OkResponse)
            logger.messages should be("[INFO] %s GET http://example.com/hello%n".format(commonNameQuoted))
          }
      }

      "should reject the request when missing cookie" in withTestLogger { logger =>
        Get("/hello") ~> logOpenAMRequest {
          completeOk
        } ~> check {
          rejection should be(MissingCookieRejection(OpenAMConfig.tokenCookie))
        }
      }

    }

    "when accessing OpenAM tokenFromCookie directive" - {

      "should get a token" in {
        Get("/hello") ~> OpenAMSession ~> tokenFromCookie { token =>
          token shouldNot be(empty)
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
        }
      }

      "should reject the request when missing cookie" in {
        Get("/hello") ~> tokenFromCookie { token =>
          fail("should not run this line")
          completeOk
        } ~> check {
          rejection should be(MissingCookieRejection(OpenAMConfig.tokenCookie))
        }
      }

    }

    "when accessing OpenAM tokenFromOptionalCookie directive" - {

      "should get an optional token" in {
        Get("/hello") ~> OpenAMSession ~> tokenFromOptionalCookie { token =>
          token shouldNot be(empty)
          token.get shouldNot be(empty)
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
        }
      }

      "should get an empty token when missing cookie" in {
        Get("/hello") ~> tokenFromOptionalCookie { token =>
          token should be(empty)
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
        }
      }

    }

    "when accessing OpenAM commonNameFromCookie directive" - {

      "should get the common name" in {
        Get("/hello") ~> OpenAMSession ~> commonNameFromCookie { commonName =>
          commonName shouldNot be(empty)
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
        }
      }

      "should reject the request when missing cookie" in {
        Get("/hello") ~> commonNameFromCookie { commonName =>
          fail("should not run this line")
          completeOk
        } ~> check {
          rejection should be(MissingCookieRejection(OpenAMConfig.tokenCookie))
        }
      }

    }

    "when accessing OpenAM commonNameFromOptionalCookie directive" - {

      "should get an optional common name" in {
        Get("/hello") ~> OpenAMSession ~> commonNameFromOptionalCookie { commonName =>
          commonName shouldNot be(empty)
          commonName.get shouldNot be(empty)
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
        }
      }

      "should get an empty common name when missing cookie" in {
        Get("/hello") ~> commonNameFromOptionalCookie { commonName =>
          commonName should be(empty)
          completeOk
        } ~> check {
          status should be(OK)
          response should be(OkResponse)
        }
      }

    }

  }

}
