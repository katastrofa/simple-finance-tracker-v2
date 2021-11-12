package org.big.pete.sft.server

import cats.data.Kleisli
import cats.effect.{Async, Resource}
import cats.syntax.{FlatMapSyntax, FunctorSyntax, SemigroupKSyntax}
import com.comcast.ip4s.{Ipv4Address, Port}
import fs2.Stream
import fs2.io.net.tls.TLSContext
import org.big.pete.sft.server.auth.AuthHelper
import org.big.pete.sft.server.auth.domain.AuthUser
import org.http4s.{AuthedRoutes, HttpRoutes, Request, Response}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.staticcontent.{FileService, fileService}
import org.http4s.server.{AuthMiddleware, Router}


class SftV2Server[F[_]: Async](authHelper: AuthHelper[F], dsl: Http4sDsl[F], ipAddress: String, port: Int)
  extends FunctorSyntax with SemigroupKSyntax with FlatMapSyntax
{
  import dsl._
  import SftV2Server.{CodeQueryParamMatcher, ErrorQueryParamMatcher}

  val authMiddleware: AuthMiddleware[F, AuthUser] =
    AuthMiddleware(authHelper.authSftUser, authHelper.loginRedirectHandler)

  val loginSupportRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case request @ GET -> Root / "google" / "response" :? CodeQueryParamMatcher(codeToken) =>
      authHelper.processLoginFromElgoog(codeToken, request)
    case GET -> Root / "google" / "response" :? ErrorQueryParamMatcher(errorCode) =>
      authHelper.processLoginError(errorCode)
  }

  val apiRoutes: AuthedRoutes[AuthUser, F] = AuthedRoutes.of {
    case GET -> Root as user => Ok(s"Authed user: ${user.dbUser.toString}, ${user.login.toString}")
    case GET -> Root / "api" as user => Ok(s"api for ${user.dbUser.displayName}")
  }

  def stream(tls: TLSContext[F]): Stream[F, Nothing] = {
    val httpApp: Kleisli[F, Request[F], Response[F]] = Router(
      "" -> loginSupportRoutes.combineK(authMiddleware(apiRoutes)),
      "static" -> fileService[F](FileService.Config("./static-assets"))
    ).orNotFound

    Stream.resource(
      EmberServerBuilder.default[F]
        .withHost(Ipv4Address.fromString(ipAddress).get)
        .withPort(Port.fromInt(port).get)
        .withTLS(tls)
        .withHttpApp(httpApp)
        .build >> Resource.eval(Async[F].never)
    )
  }.drain
}

object SftV2Server {
  object CodeQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")
  object ErrorQueryParamMatcher extends QueryParamDecoderMatcher[String]("error")
}
