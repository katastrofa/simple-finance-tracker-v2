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
import org.big.pete.sft.domain.{AccountEdit, AddAccount, GeneralData, Currency, FullAccount}
import org.big.pete.sft.domain.Implicits._
import org.big.pete.sft.server.auth.domain.AuthUser
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._


class General[F[_]: Async: Parallel](
    usersCache: BpCache[F, Int, User],
    accountsCache: BpCache[F, String, FullAccount],
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
    (patrons, currencies, accounts) <- (
      DBG.listPatrons.transact(transactor),
      currencyCache.getValues,
      DBG.listFullAccounts(user).transact(transactor)
    ).parTupled
    _ <- accounts.map(account => accountsCache.put(account.permalink, account)).sequence
    response <- Ok(GeneralData(user.simple, patrons, currencies, accounts).asJson)
  } yield response

  def listAccounts(authUser: AuthUser): F[Response[F]] = for {
    accounts <- DBG.listFullAccounts(authUser.db).transact(transactor)
    _ <- accounts.map(account => accountsCache.put(account.permalink, account)).sequence
    response <- Ok(accounts.asJson)
  } yield response

  def addAccount(authUser: AuthUser, account: AddAccount): F[Response[F]] = {
    val permissions = authUser.db.permissions
    for {
      newId <- DBG.addAccount(account).transact(transactor)
      _ <- DBG.addPatrons((authUser.db.id :: account.patrons).toSet, newId).transact(transactor)
      newAccount <- DBG.getFullAccount(newId).transact(transactor)
      newPermissions = permissions.copy(perAccount = permissions.perAccount + (newId -> permissions.default))
      _ <- Users.updatePermissions(authUser.db.id, newPermissions).transact(transactor)
      _ <- usersCache.remove(authUser.db.id)
      _ <- accountsCache.put(newAccount.get.permalink, newAccount.get)
      response <- Ok(newAccount.get.asJson)
    } yield response
  }

  def editAccount(account: AccountEdit): F[Response[F]] = {
    for {
      oldAccount <- accountsCache.get(account.oldPermalink).map(_.get)
      _ <- accountsCache.remove(account.oldPermalink)
      _ <- DBG.editAccount(account).transact(transactor)

      newAccountPatrons = account.patrons.toSet
      patronsToRemove = oldAccount.patrons.filterNot(user => newAccountPatrons.contains(user.id)).map(_.id)
      patronsToAdd = newAccountPatrons.filterNot(id => oldAccount.patrons.exists(_.id == id))
      _ <- DBG.addPatrons(patronsToAdd, account.id).transact(transactor)
      _ <- DBG.removePatrons(patronsToRemove.toSet, account.id).transact(transactor)

      newAccount <- DBG.getFullAccount(account.id).transact(transactor)
      _ <- accountsCache.put(newAccount.get.permalink, newAccount.get)
      response <- Ok(newAccount.get.asJson)
    } yield response
  }

  def deleteAccount(id: Int, permalink: String): F[Response[F]] = for {
    _ <- DBG.deleteAccount(id).traverse(_.transact(transactor))
    _ <- accountsCache.remove(permalink)
    _ <- usersCache.clear()
    response <- Ok("")
  } yield response
}
