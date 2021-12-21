package org.big.pete.sft.server.auth

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.kernel.MonadCancelThrow
import cats.effect.kernel.syntax.MonadCancelSyntax
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import com.typesafe.config.Config
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.jawn
import org.big.pete.sft.db.dao.Users
import org.big.pete.sft.server.auth.domain._
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRequest, HttpDate, Request, RequestCookie, Response, ResponseCookie}
import org.http4s.headers.{Location, `User-Agent`}
import sttp.client3.{SttpBackend, basicRequest}

import java.security.MessageDigest


class AuthHelper[F[_]: MonadCancelThrow](
    config: Config,
    dsl: Http4sDsl[F],
    sttpBackend: SttpBackend[F, Any],
    implicit val transactor: Transactor[F]
) extends FunctorSyntax with FlatMapSyntax with MonadCancelSyntax with ToConnectionIOOps
{
  import dsl._

  final val AuthCookieName = "SftV2Auth"
  final val Scopes = List("https://www.googleapis.com/auth/userinfo.email", "https://www.googleapis.com/auth/userinfo.profile")
    .mkString(" ")

  final private val md = MessageDigest.getInstance("SHA-256")

  def authSftUser: Kleisli[F, Request[F], Either[LoginRedirect, AuthUser]] = Kleisli { request =>
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
    OptionT.liftF(Users.getLogins(cookie.id).transact(transactor)).flatMap { logins =>
      val auth = logins.find { case (login, _) =>
        generateAuthCode(cookie.id, login.accessToken, browserInfo) == cookie.authCode
      }.map { case (login, user) => AuthUser(user, login) }
      OptionT.fromOption[F](auth)
    }
  }

  private def generateAuthCode(userId: Int, token: String, browserInfo: String): String = {
    val toObscure = userId.toString + browserInfo + token + config.getString("login.secret")
    md.digest(toObscure.getBytes("UTF-8")).map("%02X".format(_)).mkString
  }

  private def parseBrowserInfo(request: Request[F]): String =
    request.headers.get(`User-Agent`.name).map(_.head.value).getOrElse("")

  def loginRedirectHandler: Kleisli[OptionT[F, *], AuthedRequest[F, LoginRedirect], Response[F]] = Kleisli { request =>
    OptionT.liftF(Found(Location(request.context.uri)))
  }

  def processLoginError(errorCode: String): F[Response[F]] =
    Forbidden(s"Error code: $errorCode")

  def processLoginFromElgoog(codeToken: String, request: Request[F]): F[Response[F]] = {
    import org.http4s.implicits.http4sLiteralsSyntax

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

  def getUri(endpoint: String): sttp.model.Uri =
    sttp.model.Uri.parse(endpoint)
      .getOrElse(throw new RuntimeException("Cannot parse uri"))
}
