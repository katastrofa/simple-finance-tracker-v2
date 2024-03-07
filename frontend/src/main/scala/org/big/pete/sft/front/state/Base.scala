package org.big.pete.sft.front.state

import io.circe.Decoder
import japgolly.scalajs.react.callback.{AsyncCallback, Callback}
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.extra.internal.AjaxException
import org.big.pete.BPJson
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Category, Currency, EnhancedAccount, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.domain.{EnhancedTransaction, AccountUpdateAction, AccountUpdateOperation, Order, SortingColumn}
import org.big.pete.sft.front.utilz.TransactionsOrdering

import java.time.LocalDate
import java.time.chrono.ChronoLocalDate


trait Base {
  val $: BackendScope[Props, State]

  def modStateForSet[E](status: MICheckbox.Status, state: State, set: State => Set[E], element: E): Set[E] =
    if (status == MICheckbox.Status.checkedStatus) set(state) + element else set(state) - element

  def ajaxCall[T: Decoder](method: String, apiPath: String, payload: Option[String], empty: => T): AsyncCallback[T] = {
    $.props.async.flatMap { props =>
      val step1 = Ajax(method, props.apiBase + apiPath)
      payload.map(str => step1.send(str))
        .getOrElse(step1.send)
        .validateStatusIs(200)(displayException)
        .asAsyncCallback
        .flatMap { response =>
          BPJson.extract[T](response.responseText) match {
            case Left(value) => displayExceptionStr(value).async >> AsyncCallback.pure(empty)
            case Right(value) => AsyncCallback.pure(value)
          }
        }
    }
  }

  /// TODO: Do this
  private def displayException(ex: AjaxException): Callback =
    displayExceptionStr(ex.getMessage)

  /// TODO: Do this
  private def displayExceptionStr(error: String): Callback =
    Callback.log(error)

  def ajaxUpdate[T: Decoder](method: String, apiPath: String, payload: String, update: T => Callback): Callback = {
    $.props.flatMap { props =>
      Ajax(method, props.apiBase + apiPath)
        .setRequestContentTypeJsonUtf8
        .send(payload)
        .validateStatusIs(200)(displayException)
        .onComplete { response =>
          BPJson.extract[T](response.responseText) match {
            case Left(value) => displayExceptionStr(value)
            case Right(obj) => update(obj)
          }
        }.asCallback
    }
  }

  def filterTransactions(
      state: State,
      transactions: Option[List[Transaction]] = None,
      categories: Option[Map[Int, Category]] = None,
      accounts: Option[Map[Int, EnhancedAccount]] = None,
      transactionTypeActiveFilters: Option[Set[TransactionType]] = None,
      trackingActiveFilters: Option[Set[TransactionTracking]] = None,
      contentFilter: Option[String] = None,
      categoriesActiveFilters: Option[Set[Int]] = None,
      accountsActiveFilters: Option[Set[Int]] = None,
      transactionsSorting: Option[List[(SortingColumn, Order)]] = None,
      currencies: Option[Map[String, Currency]] = None
  ): List[EnhancedTransaction] = {
    transactions.getOrElse(state.transactions)
      .filterNonEmpty(transactionTypeActiveFilters.getOrElse(state.transactionTypeActiveFilters), _.transactionType)
      .filterNonEmpty(trackingActiveFilters.getOrElse(state.trackingActiveFilters), _.tracking)
      .filterNonEmpty(categoriesActiveFilters.getOrElse(state.categoriesActiveFilters), _.category)
      .filterNonEmpty(accountsActiveFilters.getOrElse(state.accountsActiveFilters), _.account)
      .filter(filterContent(contentFilter.getOrElse(state.contentFilter)))
      .map(EnhancedTransaction.enhance(
        categories.getOrElse(state.categories),
        accounts.getOrElse(state.moneyAccounts),
        currencies.getOrElse(state.currencies)
      ))
      .sorted(new TransactionsOrdering(transactionsSorting.getOrElse(state.transactionsSorting)))
  }

  private def filterContent(content: String)(transaction: Transaction): Boolean = {
    content.trim.split("\\s").filter(_.nonEmpty) match {
      case arr if arr.isEmpty =>
        true
      case items =>
        items.forall(str => transaction.description.contains(str))
    }
  }

  implicit class ListNonEmptyFilter[T](list: List[T]) {
    def filterNonEmpty[A](set: Set[A], zoom: T => A): List[T] =
      if (set.isEmpty) list else list.filter(t => set.contains(zoom(t)))
  }


  def updateAccountsWithTransaction(
      trans: Transaction,
      from: LocalDate,
      to: LocalDate,
      accounts: Map[Int, EnhancedAccount],
      action: AccountUpdateAction
  ): Map[Int, EnhancedAccount] = {
    if (trans.date.isBefore(to.asInstanceOf[ChronoLocalDate])) {
      trans.transactionType match {
        case TransactionType.Income =>
          accounts + (trans.account -> updateAccount(accounts(trans.account), action, AccountUpdateOperation.Add, trans, from))
        case TransactionType.Expense =>
          accounts + (trans.account -> updateAccount(
            accounts(trans.account), action, AccountUpdateOperation.Remove, trans, from
          ))
        case TransactionType.Transfer =>
          val updated = accounts + (trans.account -> updateAccount(
            accounts(trans.account), action, AccountUpdateOperation.Remove, trans, from
          ))
          updated + (trans.destinationAccount.get -> updateAccount(
            accounts(trans.destinationAccount.get), action, AccountUpdateOperation.Add, trans, from
          ))
      }
    } else
      accounts
  }

  private def updateAccount(
      account: EnhancedAccount,
      action: AccountUpdateAction,
      op: AccountUpdateOperation,
      trans: Transaction,
      from: LocalDate
  ): EnhancedAccount = {
    val realOp = AccountOperations(action)(op)
    if (trans.date.isAfter(from.asInstanceOf[ChronoLocalDate])) {
      val newStatus = account.status.filter(_.currency.id != trans.currency) ++
        account.status.find(_.currency.id == trans.currency).map { status =>
          status.copy(end = realOp(status.end, trans.amount))
        }.toList
      account.copy(status = newStatus)
    } else {
      val newStatus = account.status.filter(_.currency.id != trans.currency) ++
        account.status.find(_.currency.id == trans.currency).map { status =>
          status.copy(start = realOp(status.start, trans.amount), end = realOp(status.end, trans.amount))
        }
      account.copy(status = newStatus)
    }
  }
}
