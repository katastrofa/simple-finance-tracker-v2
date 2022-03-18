package org.big.pete.sft.server

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Resource
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import doobie.syntax.ToConnectionIOOps
import fs2.io.net.tls.TLSContext
import org.big.pete.cache.FullBpCache
import org.big.pete.sft.db.dao.{General, Users}
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.Account
import org.big.pete.sft.server.api.{Categories, MoneyAccounts, Transactions, General => GeneralApi}
import org.big.pete.sft.server.auth.AuthHelper
import org.big.pete.sft.server.security.AccessHelper
import org.http4s.dsl.Http4sDsl
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import wvlet.log.LogSupport

import scala.concurrent.ExecutionContext.global


object Main extends IOApp with LogSupport with ToConnectionIOOps {
  val mainConfig: Config = ConfigFactory.load()
  val dbConfig = new HikariConfig()
  dbConfig.setJdbcUrl(mainConfig.getString("db.url"))
  dbConfig.setUsername(mainConfig.getString("db.user"))
  dbConfig.setPassword(mainConfig.getString("db.pass"))
  dbConfig.setMaximumPoolSize(mainConfig.getInt("db.poolSize"))

  val useHttps: Boolean = mainConfig.getBoolean("server.use-https")

  val resources: Resource[IO, (SttpBackend[IO, Any], HikariTransactor[IO], TLSContext[IO], FullBpCache[IO, String, Account], FullBpCache[IO, Int, User])] = for {
    sttp <- AsyncHttpClientCatsBackend.resource[IO]()
    transactor <- HikariTransactor.fromHikariConfig[IO](dbConfig, global)
    tls <- Resource.eval(TLSContext.Builder.forAsync[IO].system)
    accountsCache <- Resource.eval(FullBpCache.apply[String, Account](100, General.getAccount(_).transact(transactor)))
    usersCache <- Resource.eval(FullBpCache.apply[Int, User](100, Users.getUser(_).transact(transactor)))
  } yield (sttp, transactor, tls, accountsCache, usersCache)

  override def run(args: List[String]): IO[ExitCode] = {
    resources.use { case (sttp, transactor, tls, accountsCache, usersCache) =>
      val dsl = Http4sDsl[IO]
      val authHelper = new AuthHelper[IO](mainConfig, dsl, sttp, usersCache, transactor)
      val accessHelper = new AccessHelper[IO](accountsCache, dsl, transactor)
      val accountsApi = new GeneralApi[IO](usersCache, accountsCache, dsl, transactor)
      val categoriesApi = new Categories[IO](dsl, transactor)
      val moneyAccountsApi = new MoneyAccounts[IO](dsl, transactor)
      val transactionsApi = new Transactions[IO](dsl, transactor)

      val server = new SftV2Server[IO](
        accountsCache,
        authHelper,
        accessHelper,
        accountsApi,
        categoriesApi,
        moneyAccountsApi,
        transactionsApi,
        dsl,
        mainConfig.getString("server.ip"),
        mainConfig.getInt("server.port")
      )

      val tlsOpt = if (useHttps) Some(tls) else None
      server.stream(tlsOpt).compile.drain.as(ExitCode.Success)
    }
  }
}
