package org.big.pete.sft.server.api

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.{FlatMapSyntax, FunctorSyntax}
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.sft.db.dao.{Transactions => DBT}
import org.big.pete.sft.domain.Transaction
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
}
