package org.big.pete.sft.server.api

import cats.Parallel
import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.{FlatMapSyntax, FunctorSyntax, ParallelSyntax}
import doobie.syntax.ToConnectionIOOps
import cats.implicits._
import doobie.util.transactor.Transactor
import io.circe.syntax.EncoderOps
import org.big.pete.cache.FullRefreshBpCache
import org.big.pete.sft.db.dao.{Accounts => DBMA, Transactions => DBT}
import org.big.pete.sft.db.domain.Balance
import org.big.pete.sft.domain.{Currency, CurrencyAndStatus, EnhancedAccount, ExpandedAccountCurrency, Account, AccountWithCurrency, ShiftStrategyPerCurrency, TransactionType}
import org.big.pete.sft.domain.Implicits._
import org.http4s.Response
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityEncoder._

import java.time.LocalDate


class MoneyAccounts[F[_]: Async: Parallel](
    dsl: Http4sDsl[F],
    currencyCache: FullRefreshBpCache[F, String, Currency],
    implicit val transactor: Transactor[F]
) extends ToConnectionIOOps with FlatMapSyntax with FunctorSyntax with ParallelSyntax {
  import dsl._

  private def calculateBalances(
      mas: List[(Int, AccountWithCurrency)],
      start: LocalDate,
      end: LocalDate,
      balances: List[Balance]
  ): Map[(Int, String, LocalDate), BigDecimal] = {
    val elements = mas.flatMap(x => List((x._1, x._2.currency, start), (x._1, x._2.currency, end)))
    val calculatedBalances = scala.collection.mutable.Map.from(elements.map(_ -> BigDecimal(0)))

    balances.foreach { balance =>
      val date = if (balance.date.isBefore(start)) start else end
      val primaryKey = (balance.account, balance.currency, date)

      balance.transactionType match {
        case TransactionType.Income =>
          calculatedBalances(primaryKey) = calculatedBalances(primaryKey) + balance.amount
        case TransactionType.Expense =>
          calculatedBalances(primaryKey) = calculatedBalances(primaryKey) - balance.amount
        case TransactionType.Transfer =>
          val otherKey = (balance.destinationAccountId.get, balance.destinationCurrency.get, date)
          if (calculatedBalances.contains(primaryKey))
            calculatedBalances(primaryKey) = calculatedBalances(primaryKey) - balance.amount
          if (calculatedBalances.contains(otherKey))
            calculatedBalances(otherKey) = calculatedBalances(otherKey) + balance.destinationAmount.get
      }
    }

    calculatedBalances.toMap
  }

  private def enhanceMoneyAccount(
      calculatedBalances: Map[(Int, String, LocalDate), BigDecimal],
      expandedCurrencies: Map[Int, List[ExpandedAccountCurrency]],
      start: LocalDate,
      end: LocalDate
  )(
      ma: AccountWithCurrency
  ): EnhancedAccount = {
    val currenciesData = expandedCurrencies(ma.id).map { currency =>
      CurrencyAndStatus(
        currency.currency,
        currency.startAmount,
        calculatedBalances((ma.id, currency.currency.id, start)) + currency.startAmount,
        calculatedBalances((ma.id, currency.currency.id, end)) + currency.startAmount
      )
    }

    EnhancedAccount(ma.id, ma.name, ma.created, expandedCurrencies(ma.id), currenciesData, ma.owner)
  }

  private def enhanceMoneyAccounts(mas: List[AccountWithCurrency], start: LocalDate, end: LocalDate): F[List[EnhancedAccount]] = {
    for {
      currencies <- currencyCache.getValues
      balances <- NonEmptyList.fromList(mas.map(_.id).distinct).map { ids =>
        DBT.getBalances(ids, end).transact(transactor)
      }.getOrElse(Async[F].pure(List.empty[Balance]))

      expandedCurrencies = mas.groupBy(_.id).map { case (id, items) =>
        id -> items.map(_.getCurrency.expand(currencies))
      }
      calculatedBalances = calculateBalances(mas.map(ma => ma.id -> ma), start, end, balances)
      enhancer = enhanceMoneyAccount(calculatedBalances, expandedCurrencies, start, end) _
      singleMas = mas.groupBy(_.id).view.mapValues(_.head)
    } yield singleMas.values.map(enhancer).toList
  }

  def listExtendedMoneyAccounts(accountId: Int, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      mas <- DBMA.listAccounts(accountId).transact(transactor)
      enhanced <- enhanceMoneyAccounts(mas, start, end)
      response <- Ok(enhanced.asJson)
    } yield response
  }

  def addMoneyAccount(ma: Account, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      newId <- DBMA.addAccount(ma).transact(transactor)
      _ <- DBMA.addCurrencies(ma.currencies.map(_.copy(account = newId))).transact(transactor)
      newMoneyAccount <- DBMA.getAccount(newId).transact(transactor)
      enhanced <- enhanceMoneyAccounts(newMoneyAccount, start, end)
      response <- Ok(enhanced.head.asJson)
    } yield response
  }

  def editMoneyAccount(ma: Account, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      (_, existingCurrencies) <- (DBMA.editAccount(ma).transact(transactor), DBMA.listCurrenciesForAccount(ma.id).transact(transactor)).parTupled

      mapped = existingCurrencies.map(cur => cur.currency -> cur).toMap
      newMapped = ma.currencies.map(cur => cur.currency -> cur).toMap
      toAdd = ma.currencies.filter(cur => !mapped.contains(cur.currency))
      toDelete = existingCurrencies.filter(cur => !newMapped.contains(cur.currency))
      toEdit = ma.currencies.filter(cur => mapped.get(cur.currency).exists(_.startAmount != cur.startAmount))

      _ <- (
        DBMA.addCurrencies(toAdd).transact(transactor),
        DBMA.deleteCurrencies(NonEmptyList.fromList(toDelete.map(_.id))).transact(transactor),
        toEdit.map(cur => DBMA.updateStartAmount(cur.id, cur.startAmount).transact(transactor)).parSequence
      ).parTupled

      newMa <- DBMA.getAccount(ma.id).transact(transactor)
      enhanced <- enhanceMoneyAccounts(newMa, start, end)
      response <- Ok(enhanced.head.asJson)
    } yield response
  }

  def deleteMoneyAccount(id: Int, shiftStrategies: List[ShiftStrategyPerCurrency]): F[Response[F]] = {
    for {
      _ <- shiftStrategies.flatMap { strategy =>
        DBT.changeAccount(strategy.currency, id, strategy.newId)
      }.map(_.transact(transactor)).parSequence
      _ <- DBMA.deleteAccount(id).transact(transactor)
      response <- Ok("")
    } yield response
  }
}
