/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */


import akka.util.Timeout
import play.api.libs.ws.ssl.debug.DebugConfiguration
import scala.concurrent.duration._

import play.api.test._
import scala.util.control.NonFatal
import com.typesafe.config.ConfigFactory
import play.api.libs.ws._
import play.api.libs.ws.ssl._
import play.api.libs.ws.ning._
import scala.util.{Failure, Success, Try}

object SSLSpec extends PlaySpecification {

  val timeout: Timeout = 20.seconds

  "WS" should {

    // test SNI: https://sni.velox.ch/

    "use a custom server" in {
      val json = play.api.libs.json.Json.toJson("kip")

      val result = try {
        val configuration = play.api.Configuration(ConfigFactory.load(
          """
            |ws.ssl.loose.disableCheckRevocation=true
          """.stripMargin))
        val parser = new DefaultWSConfigParser(configuration)
        val builder = new NingAsyncHttpClientConfigBuilder(parser.parse())
        val client = new NingWSClient(builder.build())
        val response = client.url("https://example.com")
        success
      } catch {
        case NonFatal(e) =>
          failure
      }
      result
    }

    "connect to a remote server" in {
      val rawConfig = play.api.Configuration(ConfigFactory.load(
        """
          |ws.ssl.debug = ["ssl"]
          |
          |logger.certpath=DEBUG
          |logger=DEBUG
        """.stripMargin))

      val parser = new DefaultWSConfigParser(rawConfig)
      val clientConfig : WSClientConfig = parser.parse()
      clientConfig.ssl.map { _.debug.map(new DebugConfiguration().configure) }
      val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
      val client = new NingWSClient(builder.build())

      val response = await(client.url("https://www.howsmyssl.com/a/check").get())(timeout)
      println(response.json)
      // jsonOutput.must(beMatching("awesomeness!"))
      response.status must be_==(200)

    }

    "connect to cacert.org server" in {

      val result = Try {
        val rawConfig = play.api.Configuration(ConfigFactory.load(
          """ws.ssl {
            |  disabledAlgorithms = "MD2"
            |  loose {
            |    disableCheckRevocation = true
            |  }
            |  trustManager = {
            |    stores = [
            |      { type: "JKS", path: "./keys/cacert-root-md5.crt" }
            |    ]
            |  }
            |}
          """.stripMargin))

        val parser = new DefaultWSConfigParser(rawConfig)
        val clientConfig = parser.parse()
        val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
        val client = new NingWSClient(builder.build())

        await(client.url("https://doc.to/create-x509-certs-in-java").get())(timeout)
      }

      result match {
        case Success(response) =>
          success

        case Failure(fail) =>
          fail.printStackTrace()
          CompositeCertificateException.unwrap(fail) {
            case certEx: java.security.cert.CertPathValidatorException =>
              //println(s"reason = ${certEx}")
              certEx.printStackTrace()
            case NonFatal(generalEx) =>
            //generalEx.printStackTrace()
          }
          failure
      }
    }

    "connect to expired certificate" in {

      val timeout: Timeout = 10.seconds

      var count = 0
      var succeeded = false
      try {
        val rawConfig = play.api.Configuration(ConfigFactory.load(
          """ws.ssl {
            |}
          """.stripMargin))

        val parser = new DefaultWSConfigParser(rawConfig)
        val builder = new NingAsyncHttpClientConfigBuilder(parser.parse())
        val client = new NingWSClient(builder.build())
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
        val rawConfig = play.api.Configuration(ConfigFactory.load(
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
