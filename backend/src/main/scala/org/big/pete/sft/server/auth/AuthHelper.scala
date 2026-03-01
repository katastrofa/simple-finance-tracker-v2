package org.big.pete.sft.server.auth

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.syntax.MonadCancelSyntax
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import com.typesafe.config.Config
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.jawn
import org.big.pete.cache.BpCache
import org.big.pete.sft.db.dao.Users
import org.big.pete.sft.db.domain.Login
import org.big.pete.sft.domain.User
import org.big.pete.sft.server.auth.domain.*
import org.big.pete.sft.server.auth.domain.Implicits.{googleTokenResponseDecoder, personResponseDecoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRequest, HttpDate, Request, RequestCookie, Response, ResponseCookie}
import org.http4s.headers.{Location, `User-Agent`}
import sttp.client3.{SttpBackend, basicRequest}
import wvlet.log.LogSupport

import java.security.MessageDigest


class AuthHelper[F[_]: MonadCancelThrow](
    config: Config,
    dsl: Http4sDsl[F],
    sttpBackend: SttpBackend[F, Any],
    usersCache: BpCache[F, Int, User],
    devMode: Boolean = false
)(
    using transactor: Transactor[F]
) extends FunctorSyntax with FlatMapSyntax with MonadCancelSyntax with ToConnectionIOOps with LogSupport
{
  import dsl._

  final private val AuthCookieName = "SftV2Auth"
  final private val Scopes = List("https://www.googleapis.com/auth/userinfo.email", "https://www.googleapis.com/auth/userinfo.profile")
    .mkString(" ")

  private def devBypassAuth: Kleisli[F, Request[F], Either[LoginRedirect, AuthUser]] = Kleisli { _ =>
    val syntheticLogin = Login(0, 1, java.time.LocalDateTime.now(), "dev-token", "")
    usersCache.get(1).map {
      case Some(user) => Right[LoginRedirect, AuthUser](AuthUser(user, syntheticLogin))
      case None => throw new RuntimeException("Dev mode: user ID 1 not found in database. Run seed data first.")
    }
  }

  def authSftUser: Kleisli[F, Request[F], Either[LoginRedirect, AuthUser]] =
    if (devMode) devBypassAuth
    else normalAuth

  private def normalAuth: Kleisli[F, Request[F], Either[LoginRedirect, AuthUser]] = Kleisli { request =>
    val loginOption = for {
      cookieAuthData <- OptionT.fromOption(extractAuthData(request.cookies))
      authUser <- verifyLogin(cookieAuthData, parseBrowserInfo(request))
    } yield authUser

    loginOption.value.map {
      case Some(authUser) => Right[LoginRedirect, AuthUser](authUser)
      case None =>
        val uri = org.http4s.Uri.unsafeFromString(config.getString("google.uri.auth"))
          .+?("client_id" -> config.getString("google.client-id"))
          .+?("redirect_uri" -> (config.getString("server.base-url") + "/google/response"))
          .+?("response_type" -> "code")
          .+?("scope" -> Scopes)
          .+?("access_type" -> "online")
          .+?("include_granted_scopes" -> "true")
        Left[LoginRedirect, AuthUser](LoginRedirect(uri))
    }
  }

  private def extractAuthData(cookies: List[RequestCookie]): Option[AuthCookieData] = {
    cookies.filter(_.name == AuthCookieName)
      .flatMap(_.content.split(AuthCookieSeparator, 2) match {
        case Array(userId, authCode) =>
          userId.toIntOption.map(AuthCookieData(_, authCode))
        case _ =>
          None
      }).headOption
  }

  private def verifyLogin(cookie: AuthCookieData, browserInfo: String): OptionT[F, AuthUser] = {
    for {
      logins <- OptionT.liftF(Users.getLogins(cookie.id).transact(transactor))
      login <- OptionT.fromOption(logins.find(login => generateAuthCode(cookie.id, login.accessToken, browserInfo) == cookie.authCode))
      user <- OptionT(usersCache.get(login.userId))
    } yield AuthUser(user, login)
  }

  private def generateAuthCode(userId: Int, token: String, browserInfo: String): String = {
    val toObscure = userId.toString + browserInfo + token + config.getString("login.secret")
    val md = MessageDigest.getInstance("SHA-256")
    val loginToken = md.digest(toObscure.getBytes("UTF-8")).map("%02X".format(_)).mkString
    md.reset()

    loginToken
  }

  private def parseBrowserInfo(request: Request[F]): String =
    request.headers.get(`User-Agent`.name).map(_.head.value).getOrElse("")

  def loginRedirectHandler = Kleisli { (request: AuthedRequest[F, LoginRedirect]) =>
    val kokot = Location(request.context.uri)
    val pica = Found(kokot)
    val kokotPica = OptionT.liftF(Found(Location(request.context.uri)))
    kokotPica
  }

  def processLoginError(errorCode: String): F[Response[F]] =
    Forbidden(s"Error code: $errorCode")

  def processLoginFromElgoog(codeToken: String, request: Request[F]): F[Response[F]] = {
    import org.http4s.implicits._

    val loginResult = for {
      tokenResponse <- getTokenFromElgoog(codeToken)
      personResponse <- getUserFromElgoog(tokenResponse.access_token)
      userId <- storeLogin(tokenResponse, personResponse)
    } yield (tokenResponse, userId)

    loginResult.foldF(
      error => Forbidden(error),
      result => Found(Location(uri"/")).map(_.addCookie(
        ResponseCookie(
          AuthCookieName,
          result._2.toString + AuthCookieSeparator + generateAuthCode(result._2, result._1.access_token, parseBrowserInfo(request)),
          Some(HttpDate.unsafeFromEpochSecond((System.currentTimeMillis() / 1000) + 2592000)),
          path = Some("/")
        )
      ))
    )
  }

  private def getTokenFromElgoog(codeToken: String): EitherT[F, String, GoogleTokenResponse] = {
    val bodyData = Map(
      "client_id" -> config.getString("google.client-id"),
      "client_secret" -> config.getString("google.secret"),
      "code" -> codeToken,
      "grant_type" -> "authorization_code",
      "redirect_uri" -> (config.getString("server.base-url") + "/google/response")
    )
    val response = basicRequest.body(bodyData)
      .post(getUri(config.getString("google.uri.token")))
      .send(sttpBackend)
      .map { sttpResponse =>
        sttpResponse.body.flatMap { rawResponse =>
          jawn.decode[GoogleTokenResponse](rawResponse)
            .left.map(_.getMessage)
        }
      }

    EitherT(response)
  }

  private def getUserFromElgoog(bearerToken: String): EitherT[F, String, PersonResponse] = {
    val response = basicRequest.header("Authorization", s"Bearer $bearerToken")
      .header("Accept", "application/json")
      .get(getUri(config.getString("google.uri.user")))
      .send(sttpBackend)
      .map { sttpResponse =>
        sttpResponse.body.flatMap { rawResponse =>
          jawn.decode[PersonResponse](rawResponse)
            .left.map(_.getMessage)
        }
      }

    EitherT(response)
  }

  private def storeLogin(token: GoogleTokenResponse, person: PersonResponse): EitherT[F, String, Int] = {
    val result = for {
      email <- OptionT.fromOption[F](person.emailAddresses.headOption.map(_.value))
      user <- OptionT(Users.getUser(email).transact(transactor))
      _ <- OptionT.liftF(Users.storeLogin(user.id, token.access_token, "").transact(transactor))
    } yield user.id

    EitherT.fromOptionF[F, String, Int](result.value, "Could not store login in DB")
  }

  private def getUri(endpoint: String): sttp.model.Uri =
    sttp.model.Uri.parse(endpoint)
      .getOrElse(throw new RuntimeException("Cannot parse uri"))
}
