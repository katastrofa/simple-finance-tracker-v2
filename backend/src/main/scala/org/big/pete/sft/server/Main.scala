package org.big.pete.sft.server

import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Resource
import com.typesafe.config.{Config, ConfigFactory}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import fs2.io.net.tls.TLSContext
import org.big.pete.sft.server.auth.AuthHelper
import org.http4s.dsl.Http4sDsl
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.cats.AsyncHttpClientCatsBackend
import wvlet.log.LogSupport

import scala.concurrent.ExecutionContext.global


object Main extends IOApp with LogSupport {
  val mainConfig: Config = ConfigFactory.load()
  val dbConfig = new HikariConfig()
  dbConfig.setJdbcUrl(mainConfig.getString("db.url"))
  dbConfig.setUsername(mainConfig.getString("db.user"))
  dbConfig.setPassword(mainConfig.getString("db.pass"))
  dbConfig.setMaximumPoolSize(mainConfig.getInt("db.poolSize"))

  val useHttps: Boolean = mainConfig.getBoolean("server.use-https")

  val resources: Resource[IO, (SttpBackend[IO, Any], HikariTransactor[IO], TLSContext[IO])] = for {
    sttp <- AsyncHttpClientCatsBackend.resource[IO]()
    transactor <- HikariTransactor.fromHikariConfig[IO](dbConfig, global)
    tls <- Resource.eval(TLSContext.Builder.forAsync[IO].system)
  } yield (sttp, transactor, tls)

  override def run(args: List[String]): IO[ExitCode] = {
    resources.use { case (sttp, transactor, tls) =>
      val dsl = Http4sDsl[IO]
      val authHelper = new AuthHelper[IO](mainConfig, dsl, sttp, transactor)
      val server = new SftV2Server[IO](authHelper, dsl, mainConfig.getString("server.ip"), mainConfig.getInt("server.port"))

      val tlsOpt = if (useHttps) Some(tls) else None
      server.stream(tlsOpt).compile.drain.as(ExitCode.Success)
    }
  }
}
