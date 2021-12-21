package org.big.pete.sft.server

import cats.data.Kleisli
import cats.effect.{Async, Resource}
import cats.syntax.{FlatMapSyntax, FunctorSyntax, SemigroupKSyntax}
import com.comcast.ip4s.{Ipv4Address, Port}
import fs2.Stream
import fs2.io.net.tls.TLSContext
import org.big.pete.sft.server.auth.AuthHelper
import org.big.pete.sft.server.auth.domain.AuthUser
import org.http4s.{AuthedRoutes, HttpRoutes, MediaType, Request, Response, StaticFile}
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.QueryParamDecoderMatcher
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
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
    case request @ GET -> Root as _ =>
      StaticFile.fromPath(fs2.io.file.Path("./frontend/src/main/assets/index-main.html"), Some(request.req))
        .map(_.withContentType(`Content-Type`(MediaType.text.html)))
        .getOrElseF(NotFound())
    case GET -> Root / "api" as user =>
      Ok(s"api for ${user.dbUser.displayName}")
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
  object CodeQueryParamMatcher extends QueryParamDecoderMatcher[String]("code")
  object ErrorQueryParamMatcher extends QueryParamDecoderMatcher[String]("error")
}
