/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */

import play.api.libs.json.JsSuccess


import akka.util.Timeout
import scala.concurrent.duration._

import play.api.test._
import com.typesafe.config.ConfigFactory

class HowsMySSLSpec extends PlaySpecification with CommonMethods {

  val timeout: Timeout = 20.seconds

  "WS" should {

    "connect to a remote server" in {
      val rawConfig = play.api.Configuration(ConfigFactory.parseString(
        """
          |ws.ssl.checkRevocation=true
        """.stripMargin))

      val client = createClient(rawConfig)

      val response = await(client.url("https://www.howsmyssl.com/a/check").get())(timeout)
      response.status must be_==(200)

      val jsonOutput = response.json
      Console.println(jsonOutput)
      val result = (jsonOutput \ "tls_version").validate[String]
      result must beLike {
        case JsSuccess(value, path) =>
          value must contain("TLS 1.2")
      }
    }
  }

}
