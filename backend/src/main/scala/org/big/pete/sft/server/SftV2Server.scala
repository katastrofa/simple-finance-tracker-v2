package org.big.pete.sft.server

import cats.Monad
import cats.data.Kleisli
import cats.effect.{Async, Resource}
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.{FlatMapSyntax, FunctorSyntax, SemigroupKSyntax}
import com.comcast.ip4s.{Host, Hostname, Port}
import fs2.Stream
import fs2.io.net.tls.TLSContext
import org.big.pete.cache.BpCache
import org.big.pete.sft.domain.{WalletEdit, AddWallet, ApiAction, Category, CategoryDeleteStrategies, DeleteTransactions, FullWallet, MassEditTransactions, Account, AccountDeleteStrategy, TrackingEdit, Transaction}
import org.big.pete.sft.domain.Implicits._
import org.big.pete.sft.server.api.{Categories, General, MoneyAccounts, Transactions}
import org.big.pete.sft.server.auth.AuthHelper
import org.big.pete.sft.server.auth.domain.AuthUser
import org.big.pete.sft.server.security.AccessHelper
import org.http4s.{AuthedRequest, AuthedRoutes, CacheDirective, Headers, HttpDate, HttpRoutes, MediaType, QueryParamDecoder, Request, Response, headers}
import org.http4s.Charset.`UTF-8`
import org.http4s.EntityEncoder
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scodec.bits.ByteVector

import java.time.LocalDate
import java.time.format.DateTimeFormatter


class SftV2Server[F[_]: Async](
    accountsCache: BpCache[F, String, FullWallet],
    authHelper: AuthHelper[F],
    accessHelper: AccessHelper[F],
    generalApi: General[F],
    categoriesApi: Categories[F],
    moneyAccountsApi: MoneyAccounts[F],
    transactionsApi: Transactions[F],
    dsl: Http4sDsl[F],
    host: String,
    port: Int,
    environment: String
) extends FunctorSyntax
    with SemigroupKSyntax
    with FlatMapSyntax
{
  import dsl._
  import SftV2Server.{CodeQueryParamMatcher, ErrorQueryParamMatcher, StartDateParamMatcher, EndDateParamMatcher}


  private final val logger = Slf4jLogger.getLogger[F]
  private val mainHtmlPath = if (environment.toLowerCase == "prod")
    "./static-assets/index-main.html"
  else
    "./frontend/src/main/resources/index-main.html"

  private val hostObj = if (environment.toLowerCase == "prod")
    Hostname.fromString(host)
  else
    Host.fromString(host)

  private val authMiddleware: AuthMiddleware[F, AuthUser] =
    AuthMiddleware(authHelper.authSftUser, authHelper.loginRedirectHandler)

  private val loginSupportRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "google" / "response" :? CodeQueryParamMatcher(codeToken) =>
      authHelper.processLoginFromElgoog(codeToken, request)
    case GET -> Root / "google" / "response" :? ErrorQueryParamMatcher(errorCode) =>
      authHelper.processLoginError(errorCode)
  }

  private def recovery(pf: PartialFunction[AuthedRequest[F, AuthUser], F[Response[F]]]): PartialFunction[AuthedRequest[F, AuthUser], F[Response[F]]] = {
    pf.andThen(_.handleErrorWith { throwable =>
      logger.error(throwable)(throwable.getMessage)
        .flatMap(_ => InternalServerError(throwable.getMessage))
    })
  }

  private val apiRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of(recovery {
    case _ @ GET -> Root as _ =>
      val htmlDataF = fs2.io.file.Files[F].readAll(fs2.io.file.Path(mainHtmlPath))
        .through(fs2.text.utf8.decode)
        .map(_.replace("{current}", System.currentTimeMillis().toString))
        .compile.string

      Ok.apply(htmlDataF)(
        implicitly[Monad[F]],
        EntityEncoder.simple[String](headers.`Content-Type`(MediaType.text.html))(s => ByteVector(s.getBytes(`UTF-8`.nioCharset)))
      ).map {
        _.withHeaders(Headers(
          headers.Expires(HttpDate.MinValue),
          headers.`Cache-Control`(CacheDirective.`no-cache`(), CacheDirective.`no-store`, CacheDirective.`must-revalidate`)
        ))
      }

    case GET -> Root / "api" as user =>
      Ok(s"api for ${user.db.displayName}")

    case GET -> Root / "api" / "me" as user =>
      generalApi.me(user.db)

    case GET -> Root / "api" / "general" as user =>
      accessHelper.verifyAccess(ApiAction.Basic, user)(generalApi.generalDataFetch(user.db))

    case GET -> Root / "api" / "currencies" as user =>
      accessHelper.verifyAccess(ApiAction.Basic, user)(generalApi.listCurrencies)

    case GET -> Root / "api" / "patrons" as user =>
      accessHelper.verifyAccess(ApiAction.Basic, user)(generalApi.listPatrons)

    case GET -> Root / "api" / "accounts" as user =>
      accessHelper.verifyAccess(ApiAction.Basic, user)(generalApi.listWallets(user))
    case request @ PUT -> Root / "api" / "accounts" as user =>
      for {
        account <- request.req.as[AddWallet]
          .map(_.copy(owner = Some(user.db.id)))
        response <- accessHelper.verifyAccess(ApiAction.ModifyOwnWallet, user)(generalApi.addAccount(user, account))
      } yield response
    case request @ POST -> Root / "api" / "accounts" as user =>
      for {
        accountEdit <- request.req.as[WalletEdit]
        owner <- accountsCache.get(accountEdit.oldPermalink).map(_.get.owner)
        apiAction = if (owner.contains(user.db.id)) ApiAction.ModifyOwnWallet else ApiAction.ModifyWallet
        response <- accessHelper.verifyAccess(apiAction, user)(generalApi.editAccount(accountEdit))
      } yield response
    case DELETE -> Root / "api" / "accounts" / permalink as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        apiAction = if (account.owner.contains(user.db.id)) ApiAction.DeleteOwnWallet else ApiAction.DeleteWallet
        response <- accessHelper.verifyAccess(apiAction, user)(generalApi.deleteAccount(account.id, permalink))
      } yield response

    case GET -> Root / "api" / permalink / "categories" as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        response <- accessHelper.verifyAccess(permalink, ApiAction.Basic, user)(categoriesApi.listCategories(account.id))
      } yield response
    case request @ PUT -> Root / "api" / permalink / "categories" as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        cat <- request.req.as[Category]
          .map(_.copy(owner = Some(user.db.id), wallet = account.id))
        response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnCategory, user)(categoriesApi.addCategory(cat))
      } yield response
    case request @ POST -> Root / "api" / permalink / "categories" as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        cat <- request.req.as[Category]
        apiAction = if (cat.owner.contains(user.db.id)) ApiAction.ModifyOwnCategory else ApiAction.ModifyCategory
        response <- accessHelper.verifyAccess(permalink, apiAction, user)(categoriesApi.editCategory(cat, account.id))
      } yield response
    case request @ DELETE -> Root / "api" / permalink / "categories" / IntVar(catId) as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        strategies <- request.req.as[CategoryDeleteStrategies]
        response <- accessHelper.verifyAccess(permalink, ApiAction.DeleteCategory, user) {
          categoriesApi.deleteCategory(catId, account.id, strategies.shiftSubCats, strategies.shiftTransactions)
        }
      } yield response

    case GET -> Root / "api" / permalink / "money-accounts" :? StartDateParamMatcher(start) +& EndDateParamMatcher(end) as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        response <- accessHelper.verifyAccess(permalink, ApiAction.Basic, user) {
          moneyAccountsApi.listExtendedMoneyAccounts(account.id, start, end)
        }
      } yield response
    case request @ PUT -> Root / "api" / permalink / "money-accounts" :? StartDateParamMatcher(start) +& EndDateParamMatcher(end) as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        ma <- request.req.as[Account]
          .map(_.copy(owner = Some(user.db.id), wallet = account.id))
        response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnAccount, user)(
          moneyAccountsApi.addMoneyAccount(ma, start, end)
        )
      } yield response
    case request @ POST -> Root / "api" / permalink / "money-accounts" :? StartDateParamMatcher(start) +& EndDateParamMatcher(end) as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        ma <- request.req.as[Account].map(_.copy(wallet = account.id))
        apiAction = if (ma.owner.contains(user.db.id)) ApiAction.ModifyOwnAccount else ApiAction.ModifyAccount
        response <- accessHelper.verifyAccess(permalink, apiAction, user)(
          moneyAccountsApi.editMoneyAccount(ma, start, end)
        )
      } yield response
    case request @ DELETE -> Root / "api" / permalink / "money-accounts" / IntVar(maId) as user =>
      for {
        strategy <- request.req.as[AccountDeleteStrategy]
        response <- accessHelper.verifyAccess(permalink, ApiAction.DeleteAccount, user) {
          moneyAccountsApi.deleteMoneyAccount(maId, strategy.shiftTransactions)
        }
      } yield response

    case GET -> Root / "api" / permalink / "transactions" :? StartDateParamMatcher(start) +& EndDateParamMatcher(end) as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        response <- accessHelper.verifyAccess(permalink, ApiAction.Basic, user)(
          transactionsApi.listTransaction(account.id, start, end)
        )
      } yield response
    case request @ PUT -> Root / "api" / permalink / "transactions" as user =>
      for {
        trans <- request.req.as[Transaction]
          .map(_.copy(owner = Some(user.db.id)))
        response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnTransactions, user)(
          transactionsApi.addTransaction(trans)
        )
      } yield response
    case request @ POST -> Root / "api" / permalink / "transactions" / "mass-edit" as user =>
      for {
        massEditData <- request.req.as[MassEditTransactions]
          response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnTransactions, user)(
            transactionsApi.massEditTransactions(massEditData.ids, massEditData.changeCat, massEditData.changeAccount)
          )
      } yield response
    case request @ POST -> Root / "api" / permalink / "transactions" as user =>
      for {
        trans <- request.req.as[Transaction]
        response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnTransactions, user)(
          transactionsApi.editTransaction(trans)
        )
      } yield response
    case _ @ DELETE -> Root / "api" / permalink / "transactions" / IntVar(idTrans) as user =>
      accessHelper.verifyAccess(permalink, ApiAction.DeleteTransactions, user)(
        transactionsApi.deleteTransaction(idTrans)
      )
    case request @ DELETE -> Root / "api" / permalink / "transactions" as user =>
      for {
        ids <- request.req.as[DeleteTransactions]
        response <- accessHelper.verifyAccess(permalink, ApiAction.DeleteTransactions, user)(
          transactionsApi.deleteTransactions(ids.ids)
        )
      } yield response
    case request @ POST -> Root / "api" / permalink / "transactions" / "tracking" as user =>
      for {
        data <- request.req.as[TrackingEdit]
        response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnTransactions, user)(
          transactionsApi.editTracking(data)
        )
      } yield response
  })

  def stream(tlsOpt: Option[TLSContext[F]]): Stream[F, Nothing] = {
    val httpApp: Kleisli[F, Request[F], Response[F]] = Router(
      "" -> loginSupportRoutes.combineK(authMiddleware(apiRoutes)),
      "static" -> fileService[F](FileService.Config("./static-assets")),
      "dev-assets" -> fileService[F](FileService.Config("./frontend/src/main/resources"))
    ).orNotFound

    EmberServerBuilder.default[F]
      .withHttp2
    def standardServerBuilder =
      EmberServerBuilder.default[F]
        .withHost(hostObj.get)
        .withPort(Port.fromInt(port).get)
        .withHttpApp(httpApp)

    Stream.resource(
      tlsOpt.map(tls => standardServerBuilder.withTLS(tls))
        .getOrElse(standardServerBuilder)
        .build >> Resource.eval(Async[F].never)
    )
  }.drain
}

object SftV2Server {
  import org.http4s.dsl.impl.QueryParamDecoderMatcher

  implicit val localDateParamDecoder: QueryParamDecoder[LocalDate] =
    QueryParamDecoder[String].map(date => LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd")))

  object CodeQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")
  object ErrorQueryParamMatcher extends QueryParamDecoderMatcher[String]("error")

  object StartDateParamMatcher extends QueryParamDecoderMatcher[LocalDate]("start")
  object EndDateParamMatcher extends QueryParamDecoderMatcher[LocalDate]("end")
}
