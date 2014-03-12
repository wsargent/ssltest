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
import play.api.libs.ws.ssl.debug._
import play.api.libs.ws.ning._

object SSLSpec extends PlaySpecification {

  val timeout: Timeout = 20.seconds

  // Loggers not needed, but useful to doublecheck that the code is doing what it should.
  // ./build test-only play.api.libs.ws.ssl.debug.DebugConfigurationSpec
  val internalDebugLogger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixInternalDebugLogging")
  val certpathDebugLogger = org.slf4j.LoggerFactory.getLogger("play.api.libs.ws.ssl.debug.FixCertpathDebugLogging")

  def setLoggerDebug(slf4jLogger: org.slf4j.Logger) {
    val logbackLogger = slf4jLogger.asInstanceOf[ch.qos.logback.classic.Logger]
    logbackLogger.setLevel(ch.qos.logback.classic.Level.DEBUG)
  }

  def createClient(rawConfig:play.api.Configuration) : WSClient = {
    val parser = new DefaultWSConfigParser(rawConfig)
    val clientConfig = parser.parse()
    clientConfig.ssl.map { _.debug.map(new DebugConfiguration().configure) }
    val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
    val client = new NingWSClient(builder.build())
    client
  }

  "WS" should {

    // test SNI: https://sni.velox.ch/

    "use a custom server" in {
      val json = play.api.libs.json.Json.toJson("kip")

      val result = try {
        val rawConfig = play.api.Configuration(ConfigFactory.parseString(
          """
            |ws.ssl.loose.disableCheckRevocation=true
          """.stripMargin))
        val client = createClient(rawConfig)

        val response = client.url("https://example.com")
        success
      } catch {
        case NonFatal(e) =>
          failure
      }
      result
    }

    "connect to a remote server" in {
      val rawConfig = play.api.Configuration(ConfigFactory.parseString(
        """
        """.stripMargin))

      val client = createClient(rawConfig)

      val response = await(client.url("https://www.howsmyssl.com/a/check").get())(timeout)
      println(response.json)
      // jsonOutput.must(beMatching("awesomeness!"))
      response.status must be_==(200)
    }


    "connect to expired certificate" in {

      val timeout: Timeout = 10.seconds

      var count = 0
      var succeeded = false
      try {
        val rawConfig = play.api.Configuration(ConfigFactory.parseString(
          """ws.ssl {
            |}
          """.stripMargin))

        val client = createClient(rawConfig)

        val body = await(client.url("https://doctivity.io/").get())(timeout).body
        failure
      } catch {
        case NonFatal(e) =>
          CompositeCertificateException.unwrap(e) {
            ex =>
              count = count + 1
          }
      }
      //val body = await(WS.url("https://www.ssllabs.com/ssltest/viewMyClient.html").get())(timeout).body

      // println(body)

      // body.must(beMatching ("awesomeness!"))
      succeeded must beFalse
      count must beGreaterThan(1)
    }


    "try a bad certificate" in {
      val timeout: Timeout = 10.seconds

      var succeeded = false

      var count = 0
      try {
        val rawConfig = play.api.Configuration(ConfigFactory.parseString(
          """ws.ssl {
            | debug = [ "certpath" ]
            |}
          """.stripMargin))

        val parser = new DefaultWSConfigParser(rawConfig)
        val clientConfig = parser.parse()
        val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
        val client = new NingWSClient(builder.build())
        clientConfig.ssl.map { _.debug.map(new DebugConfiguration().configure) }

        await(client.url("https://mms.nw.ru/").get())(timeout).body
        failure
      } catch {
        case NonFatal(e) =>
          //          CompositeCertificateException.unwrap(e) { ex =>
          //            ex.printStackTrace()
          //          }
          success
      }
    }
  }

  def configToMap(configString: String): Map[String, _] = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
  }

}
