package org.big.pete.sft.server.security

import cats.Monad
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.cache.BpCache
import org.big.pete.sft.domain.{Wallet, ApiAction, NotAllowedResponse}
import org.big.pete.sft.domain.Givens.given
import org.big.pete.sft.server.auth.domain.AuthUser
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl


class AccessHelper[F[_]: Monad](
    accountsCache: BpCache[F, String, Wallet],
    dsl: Http4sDsl[F]
)(
    using transactor: Transactor[F]
) extends FunctorSyntax
    with FlatMapSyntax
{
  import dsl._

  private val notAllowedResponse = Forbidden(NotAllowedResponse("You're not allowed to perform this action").asJson)

  def verifyAccess(apiAction: ApiAction, authUser: AuthUser)(response: => F[Response[F]]): F[Response[F]] = {
    if (authUser.db.permissions.global.contains(apiAction))
      response
    else
      notAllowedResponse
  }

  def verifyAccess(permalink: String, apiAction: ApiAction, authUser: AuthUser)(response: => F[Response[F]]): F[Response[F]] = {
    accountsCache.get(permalink).flatMap {
      case Some(account) if authUser.db.permissions.perWallet.get(account.id).exists(_.contains(apiAction)) =>
        response
      case _ =>
        notAllowedResponse
    }
  }

}
