package org.big.pete.sft.server.api

import cats.Monad
import cats.data.NonEmptyList
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.sft.db.dao.{Transactions => DBT}
import org.big.pete.sft.domain.{ShiftStrategy, TrackingEdit, Transaction}
import org.big.pete.sft.domain.Implicits._
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._

import java.time.LocalDate


class Transactions[F[_]: MonadCancelThrow](
    dsl: Http4sDsl[F],
    implicit val transactor: Transactor[F]
) extends ToConnectionIOOps with FlatMapSyntax with FunctorSyntax {
  import dsl._

  def listTransaction(accountId: Int, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      transactions <- DBT.listTransactions(accountId, start, end).transact(transactor)
      response <- Ok(transactions.asJson)
    } yield response
  }

  def addTransaction(trans: Transaction): F[Response[F]] = {
    for {
      newId <- DBT.addTransaction(trans).transact(transactor)
      newTransaction <- DBT.getTransaction(newId).transact(transactor)
      response <- Ok(newTransaction.get.asJson)
    } yield response
  }

  def editTransaction(trans: Transaction): F[Response[F]] = {
    for {
      _ <- DBT.editTransaction(trans).transact(transactor)
      newTransaction <- DBT.getTransaction(trans.id).transact(transactor)
      response <- Ok(newTransaction.get.asJson)
    } yield response
  }

  def editTracking(tracking: TrackingEdit): F[Response[F]] = {
    for {
      _ <- DBT.editTracking(tracking.id, tracking.tracking).transact(transactor)
      newTransaction <- DBT.getTransaction(tracking.id).transact(transactor)
      response <- Ok(newTransaction.get.asJson)
    } yield response
  }

  def massEditTransactions(ids: List[Int], shiftCat: ShiftStrategy, shiftMoneyAccount: ShiftStrategy): F[Response[F]] = {
    for {
      edited <- if (shiftCat.newId.nonEmpty || shiftMoneyAccount.newId.nonEmpty)
        DBT.massEditTransactions(NonEmptyList(ids.head, ids.tail), shiftCat.newId, shiftMoneyAccount.newId).transact(transactor)
      else
        Monad[F].pure(0)
      response <- Ok(s"$edited")
    } yield response
  }

  def deleteTransaction(id: Int): F[Response[F]] = {
    for {
      _ <- DBT.deleteTransaction(id).transact(transactor)
      response <- Ok("")
    } yield response
  }

  def deleteTransactions(ids: List[Int]): F[Response[F]] = {
    for {
      deleted <- DBT.deleteTransactions(NonEmptyList(ids.head, ids.tail)).transact(transactor)
      response <- Ok(s"$deleted")
    } yield response
  }

}
