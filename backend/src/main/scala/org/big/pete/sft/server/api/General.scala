package org.big.pete.sft.server.api

import cats.Parallel
import cats.effect.Async
import cats.implicits.{catsSyntaxTuple3Parallel, toTraverseOps}
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.cache.{BpCache, FullRefreshBpCache}
import org.big.pete.sft.db.dao.{Users, General => DBG}
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{WalletEdit, AddWallet, GeneralData, Currency, FullWallet}
import org.big.pete.sft.domain.Implicits._
import org.big.pete.sft.server.auth.domain.AuthUser
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._


class General[F[_]: Async: Parallel](
    usersCache: BpCache[F, Int, User],
    walletsCache: BpCache[F, String, FullWallet],
    currencyCache: FullRefreshBpCache[F, String, Currency],
    dsl: Http4sDsl[F],
    implicit val transactor: Transactor[F]
) extends ToConnectionIOOps with FunctorSyntax with FlatMapSyntax {
  import dsl._

  def me(user: User): F[Response[F]] =
    Ok(user.simple.asJson)

  def listCurrencies: F[Response[F]] = for {
    currencies <- currencyCache.getValues
    response <- Ok(currencies.asJson)
  } yield response

  def listPatrons: F[Response[F]] = for {
    patrons <- DBG.listPatrons.transact(transactor)
    response <- Ok(patrons.asJson)
  } yield response

  def generalDataFetch(user: User): F[Response[F]] = for {
    (patrons, currencies, wallets) <- (
      DBG.listPatrons.transact(transactor),
      currencyCache.getValues,
      DBG.listFullWallets(user).transact(transactor)
    ).parTupled
    _ <- wallets.map(wallet => walletsCache.put(wallet.permalink, wallet)).sequence
    response <- Ok(GeneralData(user.simple, patrons, currencies, wallets).asJson)
  } yield response

  def listWallets(authUser: AuthUser): F[Response[F]] = for {
    wallets <- DBG.listFullWallets(authUser.db).transact(transactor)
    _ <- wallets.map(wallet => walletsCache.put(wallet.permalink, wallet)).sequence
    response <- Ok(wallets.asJson)
  } yield response

  def addWallet(authUser: AuthUser, wallet: AddWallet): F[Response[F]] = {
    val permissions = authUser.db.permissions
    for {
      newId <- DBG.addWallet(wallet).transact(transactor)
      _ <- DBG.addPatrons((authUser.db.id :: wallet.patrons).toSet, newId).transact(transactor)
      newWallet <- DBG.getFullWallet(newId).transact(transactor)
      newPermissions = permissions.copy(perWallet = permissions.perWallet + (newId -> permissions.default))
      _ <- Users.updatePermissions(authUser.db.id, newPermissions).transact(transactor)
      _ <- usersCache.remove(authUser.db.id)
      _ <- walletsCache.put(newWallet.get.permalink, newWallet.get)
      response <- Ok(newWallet.get.asJson)
    } yield response
  }

  def editWallet(wallet: WalletEdit): F[Response[F]] = {
    for {
      oldWallet <- walletsCache.get(wallet.oldPermalink).map(_.get)
      _ <- walletsCache.remove(wallet.oldPermalink)
      _ <- DBG.editWallet(wallet).transact(transactor)

      newWalletPatrons = wallet.patrons.toSet
      patronsToRemove = oldWallet.patrons.filterNot(user => newWalletPatrons.contains(user.id)).map(_.id)
      patronsToAdd = newWalletPatrons.filterNot(id => oldWallet.patrons.exists(_.id == id))
      _ <- DBG.addPatrons(patronsToAdd, wallet.id).transact(transactor)
      _ <- DBG.removePatrons(patronsToRemove.toSet, wallet.id).transact(transactor)

      newWallet <- DBG.getFullWallet(wallet.id).transact(transactor)
      _ <- walletsCache.put(newWallet.get.permalink, newWallet.get)
      response <- Ok(newWallet.get.asJson)
    } yield response
  }

  def deleteWallet(id: Int, permalink: String): F[Response[F]] = for {
    _ <- DBG.deleteWallet(id).traverse(_.transact(transactor))
    _ <- walletsCache.remove(permalink)
    _ <- usersCache.clear()
    response <- Ok("")
  } yield response
}
