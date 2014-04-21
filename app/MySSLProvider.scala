import java.io.{File, FileInputStream}
import java.security.KeyStore
import javax.net.ssl._
import org.slf4j.LoggerFactory
import play.core.ApplicationProvider
import play.server.api.{SSLEngineProvider, SSLContextProvider}
import scala.collection.JavaConverters

class MySSLProvider extends SSLContextProvider with SSLEngineProvider {

  private val logger = LoggerFactory.getLogger("play.ssl")

  override def createSSLEngine(appProvider: ApplicationProvider, sslContext: SSLContext): SSLEngine = {
    val engine = sslContext.createSSLEngine()

    appProvider.get.map { app =>
      app.configuration.getBoolean("play.ssl.wantClientAuth").map {
        wantClientAuth =>
          logger.debug(s"createSSLEngine: wantClientAuth = $wantClientAuth")

          engine.setWantClientAuth(wantClientAuth)
      }
      app.configuration.getBoolean("play.ssl.needClientAuth").map {
        needClientAuth =>
          logger.debug(s"createSSLEngine: needClientAuth = $needClientAuth")
          engine.setNeedClientAuth(needClientAuth)
      }
    }
    engine
  }

  def keyManagers(fileName: String, password: Array[Char] = null, storeType: String = "JKS") = {
    val ks = KeyStore.getInstance(storeType)
    val ksIs = new FileInputStream(new File(fileName))
    try {
      ks.load(ksIs, password)
    } finally {
      ksIs.close()
    }

    val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
    kmf.init(ks, password)
    kmf.getKeyManagers
  }

  def trustManagers(fileName: String, password: Array[Char] = null, storeType: String = "JKS") = {
    val ts = KeyStore.getInstance(storeType)
    val tsIs = new FileInputStream(new File(fileName))
    try {
      ts.load(tsIs, password)
    } finally {
      tsIs.close()
    }
    val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
    tmf.init(ts)
    tmf.getTrustManagers
  }

  override def createSSLContext(appProvider: ApplicationProvider): SSLContext = {
    import JavaConverters._

    appProvider.get.map {
      app =>
        val keyManagerConfig = app.configuration.getConfig("play.ssl.keyManager")
        val keys: Array[KeyManager] = keyManagerConfig.flatMap {
          kmc =>
            kmc.getConfigList("stores").flatMap { stores =>
              stores.asScala.headOption.map { (firstStore) =>
                var password = firstStore.getString("password").map(_.toCharArray).orNull
                val keystore = firstStore.getString("path").getOrElse {
                  throw new IllegalStateException("path is required in play.ssl.keyManager.stores[0]")
                }
                val km = keyManagers(keystore, password)
                password = null
                km
              }
            }
        }.orNull

        val trustManagerConfig = app.configuration.getConfig("play.ssl.trustManager")
        val trusts: Array[TrustManager] = trustManagerConfig.flatMap {
          tmc =>
            tmc.getConfigList("stores").flatMap { stores =>
              stores.asScala.headOption.map { (firstStore) =>
                var password = firstStore.getString("password").map(_.toCharArray).orNull
                val keystore = firstStore.getString("path").getOrElse {
                  throw new IllegalStateException("path is required in play.ssl.trustManager.stores[0]")
                }
                val tm = trustManagers(keystore, password)
                password = null
                tm
              }
            }
        }.orNull

        logger.debug(s"createSSLContext: keyManagers = ${keys.toSeq}")
        logger.debug(s"createSSLContext: trustManagers = ${trusts.toSeq}")

        val instance = SSLContext.getInstance("TLS")
        instance.init(keys, trusts, null)

        instance
    }.get
  }
}
