
import akka.util.Timeout
import javax.net.ssl.SSLContext
import org.slf4j.LoggerFactory
import scala.concurrent.duration._

import play.api.test._
import com.typesafe.config.ConfigFactory
import play.api.libs.concurrent.Execution.{defaultContext => executionContext}

class DerpSpec extends PlaySpecification with ClientMethods {

  import ch.qos.logback.classic.Level
  import ch.qos.logback.classic.Logger


  val timeout: Timeout = 5.seconds

  // test SNI: https://sni.velox.ch/

  "WS" should {


    "use a custom server" in {
      //SSLContext.getDefault()
      val rawConfig = play.api.Configuration(ConfigFactory.parseString(
        """
          |ws.ssl {
          |  trustManager = {
          |    stores = [
          |      { path = "/Users/wsargent/work/ssltest/certs/exampletrust.jks" }
          |    ]
          |  }
          |    keyManager = {
          |      stores = [
          |      { type = "JKS", path = "/Users/wsargent/work/ssltest/certs/client.jks", password = "ZlXQnQtVJ8" }
          |      ]
          |    }
          |
          |}
        """.
          stripMargin))
      val client = createClient(rawConfig)
      val root = LoggerFactory.getLogger("play.api.libs.ws.ssl").asInstanceOf[Logger]
      root.setLevel(Level.DEBUG)

      val response = client.url("https://example.com:9443").get()
      response.map {
        r =>
        //Console.println(r.body)
      }(executionContext)
      await(response).status must be_==(200)
    }
  }
}