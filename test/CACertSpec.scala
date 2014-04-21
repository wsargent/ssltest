/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */

import akka.util.Timeout
import scala.concurrent.duration._

import play.api.test._
import scala.util.control.NonFatal
import com.typesafe.config.ConfigFactory
import play.api.libs.ws._
import play.api.libs.ws.ssl._

class CACertSpec extends PlaySpecification {

  implicit val timeout: Timeout = 20.seconds

  // Loggers not needed, but useful to doublecheck that the code is doing what it should.
  // play test-only CACertSpec
  val internalDebugLogger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug")
  //val certpathDebugLogger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixCertpathDebugLogging")

  def setLoggerDebug(slf4jLogger: org.slf4j.Logger) {
    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
  }

  def configToMap(configString: String): Map[String, _] = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
  }

  "connect to cacert.org server" in {

    java.security.Security.setProperty("jdk.certpath.disabledAlgorithms", "MD2, MD5")

    val rawConfig = """ws.ssl {
                      |  checkRevocation = false
                      |  trustManager = {
                      |    stores = [
                      |      { type: "PEM", path: "./certs/root.crt" }
                      |    ]
                      |  }
                      |}
                    """.stripMargin

    implicit val application = FakeApplication(additionalConfiguration = configToMap(rawConfig))

    try {
      val server = TestServer(port = Helpers.testServerPort)
      Helpers.running(server) {
        //setLoggerDebug(internalDebugLogger)
        val client = WS.client
        await(client.url("https://doc.to/create-x509-certs-in-java").get()).status must_== 200
      }
      success
    } catch {
      case NonFatal(e) =>
        CompositeCertificateException.unwrap(e) {
          ex => ex.printStackTrace()
        }
        failure
    }
  }

  //
  //    result match {
  //      case Success(response) =>
  //        success
  //
  //      case Failure(fail) =>
  //        fail.printStackTrace()
  //        CompositeCertificateException.unwrap(fail) {
  //          case certEx: java.security.cert.CertPathValidatorException =>
  //            //println(s"reason = ${certEx}")
  //            certEx.printStackTrace()
  //          case NonFatal(generalEx) =>
  //          //generalEx.printStackTrace()
  //        }
  //        failure
  //    }
  //  }


}
