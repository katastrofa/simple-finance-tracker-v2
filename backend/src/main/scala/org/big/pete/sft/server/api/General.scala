package org.big.pete.sft.server.api

import cats.effect.kernel.MonadCancelThrow
import cats.implicits.toTraverseOps
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.cache.{BpCache, FullRefreshBpCache}
import org.big.pete.sft.db.dao.{Users, General => DBG}
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{Wallet, WalletEdit, Currency}
import org.big.pete.sft.domain.Givens._
import org.big.pete.sft.server.auth.domain.AuthUser
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._


class General[F[_]: MonadCancelThrow](
    usersCache: BpCache[F, Int, User],
    walletsCache: BpCache[F, String, Wallet],
    currencyCache: FullRefreshBpCache[F, String, Currency],
    dsl: Http4sDsl[F]
)(
    using transactor: Transactor[F]
) extends ToConnectionIOOps with FunctorSyntax with FlatMapSyntax {
  import dsl._

  def listCurrencies: F[Response[F]] = {
    for {
      currencies <- currencyCache.getValues
      response <- Ok(currencies.asJson)
    } yield response
  }

  def listWallets(authUser: AuthUser): F[Response[F]] = {
    for {
      wallets <- DBG.listWallets(authUser.db).transact(transactor)
      _ <- wallets.map(wallet => walletsCache.put(wallet.permalink, wallet)).sequence
      response <- Ok(wallets.asJson)
    } yield response
  }

  def addWallet(authUser: AuthUser, wallet: Wallet): F[Response[F]] = {
    val permissions = authUser.db.permissions
    for {
      id <- DBG.addWallet(wallet).transact(transactor)
      newWallet <- DBG.getWallet(id).transact(transactor)
      newPermissions = permissions.copy(perWallet = permissions.perWallet + (id -> permissions.default))
      _ <- Users.updatePermissions(authUser.db.id, newPermissions).transact(transactor)
      _ <- usersCache.remove(authUser.db.id)
      _ <- walletsCache.put(newWallet.get.permalink, newWallet.get)
      response <- Ok(newWallet.get.asJson)
    } yield response
  }

  def editWallet(wallet: WalletEdit): F[Response[F]] = {
    for {
      _ <- walletsCache.remove(wallet.oldPermalink)
      _ <- DBG.editWallet(wallet).transact(transactor)
      newWallet <- DBG.getWallet(wallet.id).transact(transactor)
      _ <- walletsCache.put(newWallet.get.permalink, newWallet.get)
      response <- Ok(newWallet.get.asJson)
    } yield response
  }

  def deleteWallet(id: Int, permalink: String): F[Response[F]] = {
    for {
      _ <- DBG.deleteWallet(id).traverse(_.transact(transactor))
      _ <- walletsCache.remove(permalink)
      _ <- usersCache.clear()
      response <- Ok("")
    } yield response
  }
}
