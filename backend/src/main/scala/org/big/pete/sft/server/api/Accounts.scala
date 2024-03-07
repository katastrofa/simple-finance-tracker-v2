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


class Accounts[F[_]: Async: Parallel](
    dsl: Http4sDsl[F],
    currencyCache: FullRefreshBpCache[F, String, Currency],
    implicit val transactor: Transactor[F]
) extends ToConnectionIOOps with FlatMapSyntax with FunctorSyntax with ParallelSyntax {
  import dsl._

  private def calculateBalances(
      accounts: List[(Int, AccountWithCurrency)],
      start: LocalDate,
      end: LocalDate,
      balances: List[Balance]
  ): Map[(Int, String, LocalDate), BigDecimal] = {
    val elements = accounts.flatMap(x => List((x._1, x._2.currency, start), (x._1, x._2.currency, end)))
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

  private def enhanceAccount(
      calculatedBalances: Map[(Int, String, LocalDate), BigDecimal],
      expandedCurrencies: Map[Int, List[ExpandedAccountCurrency]],
      start: LocalDate,
      end: LocalDate
  )(
      account: AccountWithCurrency
  ): EnhancedAccount = {
    val currenciesData = expandedCurrencies(account.id).map { currency =>
      CurrencyAndStatus(
        currency.currency,
        currency.startAmount,
        calculatedBalances((account.id, currency.currency.id, start)) + currency.startAmount,
        calculatedBalances((account.id, currency.currency.id, end)) + currency.startAmount
      )
    }

    EnhancedAccount(account.id, account.name, account.created, expandedCurrencies(account.id), currenciesData, account.owner)
  }

  private def enhanceAccounts(accounts: List[AccountWithCurrency], start: LocalDate, end: LocalDate): F[List[EnhancedAccount]] = {
    for {
      currencies <- currencyCache.getValues
      balances <- NonEmptyList.fromList(accounts.map(_.id).distinct).map { ids =>
        DBT.getBalances(ids, end).transact(transactor)
      }.getOrElse(Async[F].pure(List.empty[Balance]))

      expandedCurrencies = accounts.groupBy(_.id).map { case (id, items) =>
        id -> items.map(_.getCurrency.expand(currencies))
      }
      calculatedBalances = calculateBalances(accounts.map(ma => ma.id -> ma), start, end, balances)
      enhancer = enhanceAccount(calculatedBalances, expandedCurrencies, start, end) _
      singleAccounts = accounts.groupBy(_.id).view.mapValues(_.head)
    } yield singleAccounts.values.map(enhancer).toList
  }

  def listExtendedAccounts(wallet: Int, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      accounts <- DBMA.listAccounts(wallet).transact(transactor)
      enhanced <- enhanceAccounts(accounts, start, end)
      response <- Ok(enhanced.asJson)
    } yield response
  }

  def addAccount(account: Account, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      newId <- DBMA.addAccount(account).transact(transactor)
      _ <- DBMA.addCurrencies(account.currencies.map(_.copy(account = newId))).transact(transactor)
      newAccount <- DBMA.getAccount(newId).transact(transactor)
      enhanced <- enhanceAccounts(newAccount, start, end)
      response <- Ok(enhanced.head.asJson)
    } yield response
  }

  def editAccount(account: Account, start: LocalDate, end: LocalDate): F[Response[F]] = {
    for {
      (_, existingCurrencies) <- (DBMA.editAccount(account).transact(transactor), DBMA.listCurrenciesForAccount(account.id).transact(transactor)).parTupled

      mapped = existingCurrencies.map(cur => cur.currency -> cur).toMap
      newMapped = account.currencies.map(cur => cur.currency -> cur).toMap
      toAdd = account.currencies.filter(cur => !mapped.contains(cur.currency))
      toDelete = existingCurrencies.filter(cur => !newMapped.contains(cur.currency))
      toEdit = account.currencies.filter(cur => mapped.get(cur.currency).exists(_.startAmount != cur.startAmount))

      _ <- (
        DBMA.addCurrencies(toAdd).transact(transactor),
        DBMA.deleteCurrencies(NonEmptyList.fromList(toDelete.map(_.id))).transact(transactor),
        toEdit.map(cur => DBMA.updateStartAmount(cur.id, cur.startAmount).transact(transactor)).parSequence
      ).parTupled

      newAccount <- DBMA.getAccount(account.id).transact(transactor)
      enhanced <- enhanceAccounts(newAccount, start, end)
      response <- Ok(enhanced.head.asJson)
    } yield response
  }

  def deleteAccount(id: Int, shiftStrategies: List[ShiftStrategyPerCurrency]): F[Response[F]] = {
    for {
      _ <- shiftStrategies.flatMap { strategy =>
        DBT.changeAccount(strategy.currency, id, strategy.newId)
      }.map(_.transact(transactor)).parSequence
      _ <- DBMA.deleteAccount(id).transact(transactor)
      response <- Ok("")
    } yield response
  }
}
