package org.big.pete.sft.server.api

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.{FlatMapSyntax, FunctorSyntax, ParallelSyntax}
import doobie.syntax.ToConnectionIOOps
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.sft.db.dao.{General, MoneyAccounts => DBMA, Transactions => DBT}
import org.big.pete.sft.db.domain.Balance
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, MoneyAccount, PeriodAmountStatus, ShiftStrategy, TransactionType}
import org.big.pete.sft.domain.Implicits._
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._

import java.time.LocalDate


class MoneyAccounts[F[_]: Async: Parallel](
    dsl: Http4sDsl[F],
    implicit val transactor: Transactor[F]
) extends ToConnectionIOOps with FlatMapSyntax with FunctorSyntax with ParallelSyntax {
  import dsl._

  private def calculateBalances(
      ids: List[Int],
      start: LocalDate,
      end: LocalDate,
      balances: List[Balance]
  ): Map[(Int, LocalDate), BigDecimal] = {
    val elements = ids.flatMap(id => List(((id, start), BigDecimal(0)), ((id, end), BigDecimal(0))))
    val calculatedBalances = scala.collection.mutable.Map(elements: _*)

    balances.foreach { balance =>
      val date = if (balance.date.isBefore(start)) start else end
      val primaryKey = balance.moneyAccount -> date

      balance.transactionType match {
        case TransactionType.Income =>
          calculatedBalances(primaryKey) = calculatedBalances(primaryKey) + balance.amount
        case TransactionType.Expense =>
          calculatedBalances(primaryKey) = calculatedBalances(primaryKey) - balance.amount
        case TransactionType.Transfer =>
          val otherKey = balance.destinationMoneyAccountId.get -> date
          calculatedBalances(primaryKey) = calculatedBalances(primaryKey) - balance.amount
          calculatedBalances(otherKey) = calculatedBalances(otherKey) + balance.destinationAmount.get
      }
    }

    calculatedBalances.toMap
  }

  private def enhanceMoneyAccount(
      calculatedBalances: Map[(Int, LocalDate), BigDecimal],
      currencies: Map[String, Currency],
      start: LocalDate,
      end: LocalDate
  )(
      ma: MoneyAccount
  ): EnhancedMoneyAccount = {
    val period = PeriodAmountStatus(
      calculatedBalances(ma.id -> start) + ma.startAmount,
      calculatedBalances(ma.id -> end) + ma.startAmount
    )
    EnhancedMoneyAccount(ma.id, ma.name, ma.startAmount, currencies(ma.currencyId), ma.created, period, ma.owner)
  }

  def listExtendedMoneyAccounts(accountId: Int, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      mas <- DBMA.listMoneyAccounts(accountId).transact(transactor)
      ids = mas.map(_.id)

      (balances, currencies) <- NonEmptyList.fromList(ids).map { nonEmptyIds =>
        (
          DBT.getBalances(nonEmptyIds, end).transact(transactor),
          General.listCurrencies.transact(transactor)
        ).parTupled
      }.getOrElse(
        Async[F].pure(List.empty[Balance] -> List.empty[Currency])
      )

      calculatedBalances = calculateBalances(ids, start, end, balances)
      enhancer = enhanceMoneyAccount(calculatedBalances, currencies.map(c => c.id -> c).toMap, start, end) _
      enhanced = mas.map(enhancer)

      response <- Ok(enhanced.asJson)
    } yield response
  }

  def addMoneyAccount(ma: MoneyAccount): F[Response[F]] = {
    for {
      newId <- DBMA.addMoneyAccount(ma).transact(transactor)
      newMa <- DBMA.getMoneyAccount(newId, ma.accountId).transact(transactor)
      response <- Ok(newMa.asJson)
    } yield response
  }

  def editMoneyAccount(ma: MoneyAccount, accountId: Int): F[Response[F]] = {
    for {
      _ <- DBMA.editMoneyAccount(ma, accountId).transact(transactor)
      newMa <- DBMA.getMoneyAccount(ma.id, accountId).transact(transactor)
      response <- Ok(newMa.asJson)
    } yield response
  }

  def deleteMoneyAccount(id: Int, accountId: Int, transactionsShiftStrategy: ShiftStrategy): F[Response[F]] = {
    for {
      _ <- DBT.changeMoneyAccount(id, transactionsShiftStrategy.newId.get, accountId).map(_.transact(transactor)).parSequence
      _ <- DBMA.deleteMoneyAccount(id, accountId).transact(transactor)
      response <- Ok("")
    } yield response
  }
}
