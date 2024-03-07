package org.big.pete.sft.server

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Resource
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.syntax.ToConnectionIOOps
import fs2.io.net.tls.TLSContext
import org.big.pete.cache.{FullBpCache, FullRefreshBpCache}
import org.big.pete.sft.db.dao.{General, Users}
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{Currency, FullWallet}
import org.big.pete.sft.server.api.{Categories, Accounts, Transactions, General => GeneralApi}
import org.big.pete.sft.server.auth.AuthHelper
import org.big.pete.sft.server.security.AccessHelper
import org.http4s.dsl.Http4sDsl
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import wvlet.log.LogSupport

import java.io.FileInputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}


object Main extends IOApp with LogSupport with ToConnectionIOOps {
  private val mainConfig: Config = ConfigFactory.load()
  private val dbConfig = new HikariConfig()
  dbConfig.setJdbcUrl(mainConfig.getString("db.url"))
  dbConfig.setUsername(mainConfig.getString("db.user"))
  dbConfig.setPassword(mainConfig.getString("db.pass"))
  dbConfig.setMaximumPoolSize(mainConfig.getInt("db.poolSize"))

  private val useHttps: Boolean = mainConfig.getBoolean("server.use-https")

  private def createSSlContext(storeType: String, keyStorePath: String, password: String): IO[SSLContext] = {
    val openKeyStore = IO.delay(new FileInputStream(keyStorePath))
    val loadKeyStore = Resource.fromAutoCloseable(openKeyStore).use { input =>
      IO {
        val keyStoreRaw = KeyStore.getInstance(storeType)
        keyStoreRaw.load(input, password.toCharArray)
        keyStoreRaw
      }
    }
    def createTmf(keyStore: KeyStore) = IO {
      val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
      tmf.init(keyStore)
      tmf
    }
    def createKmf(keyStore: KeyStore) = IO {
      val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
      kmf.init(keyStore, password.toCharArray)
      kmf
    }
    def createSslContext(kmf: KeyManagerFactory, tmf: TrustManagerFactory) = IO {
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(kmf.getKeyManagers, tmf.getTrustManagers, new SecureRandom())
      sslContext
    }


    for {
      keyStore <- loadKeyStore
      tmf <- createTmf(keyStore)
      kmf <- createKmf(keyStore)
      sslContext <- createSslContext(kmf, tmf)
    } yield sslContext
  }

  private def createTLSContext(config: Config): Resource[IO, TLSContext[IO]] = {
    if (config.hasPath("ssl.key-store")) {
      val tls = createSSlContext(
        config.getString("ssl.store-type"),
        config.getString("ssl.key-store"),
        config.getString("ssl.password")
      ).map { sslContext =>
        TLSContext.Builder.forAsync[IO].fromSSLContext(sslContext)
      }

      Resource.eval(tls)
    } else {
      Resource.eval(TLSContext.Builder.forAsync[IO].system)
    }
  }

  private val resources =
    for {
      sttp <- HttpClientCatsBackend.resource[IO]()
      transactor <- HikariTransactor.fromHikariConfig[IO](dbConfig)
      tls <- createTLSContext(mainConfig)
      walletsCache <- Resource.eval(FullBpCache.apply[String, FullWallet](100, General.getFullWallet(_).transact(transactor)))
      usersCache <- Resource.eval(FullBpCache.apply[Int, User](100, Users.getUser(_).transact(transactor)))
      currencyCache <- FullRefreshBpCache[String, Currency](() => General.listCurrencies.transact(transactor).map(_.map(cur => cur.id -> cur)))
    } yield (sttp, transactor, tls, walletsCache, usersCache, currencyCache)

  override def run(args: List[String]): IO[ExitCode] = {
    resources.use { case (sttp, transactor, tls, walletsCache, usersCache, currencyCacheIO) =>
      val currencyCache = currencyCacheIO.unsafeRunSync()(runtime)
      val dsl = Http4sDsl[IO]
      val authHelper = new AuthHelper[IO](mainConfig, dsl, sttp, usersCache, transactor)
      val accessHelper = new AccessHelper[IO](walletsCache, dsl, transactor)
      val generalApi = new GeneralApi[IO](usersCache, walletsCache, currencyCache, dsl, transactor)
      val categoriesApi = new Categories[IO](dsl, transactor)
      val accountsApi = new Accounts[IO](dsl, currencyCache, transactor)
      val transactionsApi = new Transactions[IO](dsl, transactor)

      val server = new SftV2Server[IO](
        walletsCache,
        authHelper,
        accessHelper,
        generalApi,
        categoriesApi,
        accountsApi,
        transactionsApi,
        dsl,
        mainConfig.getString("server.ip"),
        mainConfig.getInt("server.port"),
        mainConfig.getString("server.environment")
      )

      val tlsOpt = if (useHttps) Some(tls) else None
      server.stream(tlsOpt).compile.drain.as(ExitCode.Success)
    }
  }
}
