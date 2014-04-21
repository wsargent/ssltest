import javax.net.ssl.{SSLEngine, SSLContext}
import play.core.ApplicationProvider
import play.server.api.SSLEngineProvider

/**
 * Creates an SSL Engine that needs client authentication.
 */
class NeedsClientAuthSSLEngineProvider extends SSLEngineProvider {
  override def createSSLEngine(appProvider: ApplicationProvider, sslContext: SSLContext): SSLEngine = {
    val engine = sslContext.createSSLEngine()
    engine.setNeedClientAuth(true)
    engine
  }
}
