import com.typesafe.config.ConfigFactory
import play.api.libs.ws.ning.{NingWSClient, NingAsyncHttpClientConfigBuilder}
import play.api.libs.ws.ssl.debug.DebugConfiguration
import play.api.libs.ws.{DefaultWSConfigParser, WSClient}

/*
 *
 *  * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 *
 */

trait CommonMethods {

  def createClient(rawConfig: play.api.Configuration): WSClient = {
    val parser = new DefaultWSConfigParser(rawConfig)
    val clientConfig = parser.parse()
    clientConfig.ssl.map {
      _.debug.map(new DebugConfiguration().configure)
    }
    val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
    val client = new NingWSClient(builder.build())
    client
  }

  def configToMap(configString: String): Map[String, _] = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
  }
}
