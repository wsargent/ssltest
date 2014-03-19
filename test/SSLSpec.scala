/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */


import akka.util.Timeout
import play.api.libs.json.{JsError, JsSuccess}
import scala.concurrent.duration._

import play.api.test._
import scala.util.control.NonFatal
import com.typesafe.config.ConfigFactory
import play.api.libs.ws._
import play.api.libs.ws.ssl._
import play.api.libs.ws.ssl.debug._
import play.api.libs.ws.ning._

object SSLSpec extends PlaySpecification with CommonMethods {

  val timeout: Timeout = 20.seconds

  "WS" should {

    // test SNI: https://sni.velox.ch/

    "use a custom server" in {
      val json = play.api.libs.json.Json.toJson("kip")

      val result = try {
        val rawConfig = play.api.Configuration(ConfigFactory.parseString(
          """
            |ws.ssl.checkRevocation=false
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
            |
            |}
          """.stripMargin))

        val parser = new DefaultWSConfigParser(rawConfig)
        val clientConfig = parser.parse()
        val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
        val client = new NingWSClient(builder.build())
        clientConfig.ssl.map {
          _.debug.map(new DebugConfiguration().configure)
        }

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

}
