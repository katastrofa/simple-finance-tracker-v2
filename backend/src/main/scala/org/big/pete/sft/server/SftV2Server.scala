package org.big.pete.sft.server

import cats.data.Kleisli
import cats.effect.{Async, Resource}
import cats.syntax.{FlatMapSyntax, FunctorSyntax, SemigroupKSyntax}
import com.comcast.ip4s.{Ipv4Address, Port}
import fs2.Stream
import fs2.io.net.tls.TLSContext
import org.big.pete.cache.BpCache
import org.big.pete.sft.domain.{Account, AccountEdit, ApiAction, Category, CategoryDeleteStrategies, MoneyAccount, MoneyAccountDeleteStrategy}
import org.big.pete.sft.domain.Implicits._
import org.big.pete.sft.server.api.{Accounts, Categories, MoneyAccounts}
import org.big.pete.sft.server.auth.AuthHelper
import org.big.pete.sft.server.auth.domain.AuthUser
import org.big.pete.sft.server.security.AccessHelper
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType, QueryParamDecoder, Request, Response, StaticFile}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.server.{AuthMiddleware, Router}

import java.time.LocalDate
import java.time.format.DateTimeFormatter


class SftV2Server[F[_]: Async](
    accountsCache: BpCache[F, String, Account],
    authHelper: AuthHelper[F],
    accessHelper: AccessHelper[F],
    accountsApi: Accounts[F],
    categoriesApi: Categories[F],
    moneyAccountsApi: MoneyAccounts[F],
    dsl: Http4sDsl[F],
    ipAddress: String,
    port: Int
) extends FunctorSyntax
    with SemigroupKSyntax
    with FlatMapSyntax
{
  import dsl._
  import SftV2Server.{CodeQueryParamMatcher, ErrorQueryParamMatcher, StartDateParamMatcher, EndDateParamMatcher}



  val authMiddleware: AuthMiddleware[F, AuthUser] =
    AuthMiddleware(authHelper.authSftUser, authHelper.loginRedirectHandler)

  val loginSupportRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "google" / "response" :? CodeQueryParamMatcher(codeToken) =>
      authHelper.processLoginFromElgoog(codeToken, request)
    case GET -> Root / "google" / "response" :? ErrorQueryParamMatcher(errorCode) =>
      authHelper.processLoginError(errorCode)
  }

  val apiRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {
    case request @ GET -> Root as _ =>
      StaticFile.fromPath(fs2.io.file.Path("./frontend/src/main/assets/index-main.html"), Some(request.req))
        .map(_.withContentType(`Content-Type`(MediaType.text.html)))
        .getOrElseF(NotFound())
    case GET -> Root / "api" as user =>
      Ok(s"api for ${user.db.displayName}")

    case GET -> Root / "api" / "accounts" as user =>
      accessHelper.verifyAccess(ApiAction.Basic, user)(accountsApi.listAccounts(user))
    case request @ PUT -> Root / "api" / "accounts" as user =>
      for {
        account <- request.req.as[Account]
          .map(_.copy(owner = Some(user.db.id)))
        response <- accessHelper.verifyAccess(ApiAction.ModifyOwnAccount, user)(accountsApi.addAccount(user, account))
      } yield response
    case request @ POST -> Root / "api" / "accounts" as user =>
      for {
        accountEdit <- request.req.as[AccountEdit]
        owner <- accountsCache.get(accountEdit.oldPermalink).map(_.get.owner)
        apiAction = if (owner.contains(user.db.id)) ApiAction.ModifyOwnAccount else ApiAction.ModifyAccount
        response <- accessHelper.verifyAccess(apiAction, user)(accountsApi.editAccount(accountEdit))
      } yield response
    case DELETE -> Root / "api" / "accounts" / permalink as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        apiAction = if (account.owner.contains(user.db.id)) ApiAction.DeleteOwnAccount else ApiAction.DeleteAccount
        response <- accessHelper.verifyAccess(apiAction, user)(accountsApi.deleteAccount(account.id, permalink))
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
          .map(_.copy(owner = Some(user.db.id), accountId = account.id))
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
    case request @ PUT -> Root / "api" / permalink / "money-accounts" as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        ma <- request.req.as[MoneyAccount]
          .map(_.copy(owner = Some(user.db.id), accountId = account.id))
        response <- accessHelper.verifyAccess(permalink, ApiAction.ModifyOwnMoneyAccount, user)(moneyAccountsApi.addMoneyAccount(ma))
      } yield response
    case request @ POST -> Root / "api" / permalink / "money-accounts" as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        ma <- request.req.as[MoneyAccount]
        apiAction = if (ma.owner.contains(user.db.id)) ApiAction.ModifyOwnMoneyAccount else ApiAction.ModifyMoneyAccount
        response <- accessHelper.verifyAccess(permalink, apiAction, user)(moneyAccountsApi.editMoneyAccount(ma, account.id))
      } yield response
    case request @ DELETE -> Root / "api" / permalink / "money-accounts" / IntVar(maId) as user =>
      for {
        account <- accountsCache.get(permalink).map(_.get)
        strategy <- request.req.as[MoneyAccountDeleteStrategy]
        response <- accessHelper.verifyAccess(permalink, ApiAction.DeleteMoneyAccount, user) {
          moneyAccountsApi.deleteMoneyAccount(maId, account.id, strategy.shiftTransactions)
        }
      } yield response
  }

  def stream(tlsOpt: Option[TLSContext[F]]): Stream[F, Nothing] = {
    val httpApp: Kleisli[F, Request[F], Response[F]] = Router(
      "" -> loginSupportRoutes.combineK(authMiddleware(apiRoutes)),
      "static" -> fileService[F](FileService.Config("./static-assets")),
      "dev-assets" -> fileService[F](FileService.Config("./frontend/src/main/assets"))
    ).orNotFound

    def standardServerBuilder =
      EmberServerBuilder.default[F]
        .withHost(Ipv4Address.fromString(ipAddress).get)
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
