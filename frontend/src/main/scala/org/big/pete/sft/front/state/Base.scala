package org.big.pete.sft.front.state

import io.circe.Decoder
import japgolly.scalajs.react.callback.{AsyncCallback, Callback}
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.extra.internal.AjaxException
import org.big.pete.BPJson
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Category, EnhancedMoneyAccount, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.domain.{EnhancedTransaction, MAUpdateAction, MAUpdateOperation, Order, SortingColumn}
import org.big.pete.sft.front.utilz.TransactionsOrdering

import java.time.LocalDate
import java.time.chrono.ChronoLocalDate


trait Base {
  val $: BackendScope[Props, State]

  def modStateForSet[E](status: MICheckbox.Status, state: State, set: State => Set[E], element: E): Set[E] =
    if (status == MICheckbox.Status.checkedStatus) set(state) + element else set(state) - element

  def ajaxCall[T: Decoder](method: String, apiPath: String, payload: Option[String], empty: => T): AsyncCallback[T] = {
    Callback.log(s"AJAX - $method $apiPath with payload $payload").async >>
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
  def displayException(ex: AjaxException): Callback =
    displayExceptionStr(ex.getMessage)

  /// TODO: Do this
  def displayExceptionStr(error: String): Callback =
    Callback.log(error)

  def ajaxUpdate[T: Decoder](method: String, apiPath: String, payload: String, update: T => Callback): Callback = {
    Callback.log(s"AJAX - $method $apiPath with $payload") >>
      $.props.flatMap { props =>
        Ajax(method, props.apiBase + apiPath)
          .setRequestContentTypeJsonUtf8
          .send(payload)
          .validateStatusIs(200)(displayException)
          .onComplete { response =>
            BPJson.extract[T](response.responseText) match {
              case Left(value) => displayExceptionStr(value)
              case Right(obj) => Callback.log(s"Response - ${response.status} - ${response.responseText}") >> update(obj)
            }
          }.asCallback
      }
  }

  def filterTransactions(
      state: State,
      transactions: Option[List[Transaction]] = None,
      categories: Option[Map[Int, Category]] = None,
      moneyAccounts: Option[Map[Int, EnhancedMoneyAccount]] = None,
      transactionTypeActiveFilters: Option[Set[TransactionType]] = None,
      trackingActiveFilters: Option[Set[TransactionTracking]] = None,
      contentFilter: Option[String] = None,
      categoriesActiveFilters: Option[Set[Int]] = None,
      moneyAccountsActiveFilters: Option[Set[Int]] = None,
      transactionsSorting: Option[List[(SortingColumn, Order)]] = None
  ): List[EnhancedTransaction] = {
    transactions.getOrElse(state.transactions)
      .filterNonEmpty(transactionTypeActiveFilters.getOrElse(state.transactionTypeActiveFilters), _.transactionType)
      .filterNonEmpty(trackingActiveFilters.getOrElse(state.trackingActiveFilters), _.tracking)
      .filterNonEmpty(categoriesActiveFilters.getOrElse(state.categoriesActiveFilters), _.categoryId)
      .filterNonEmpty(moneyAccountsActiveFilters.getOrElse(state.moneyAccountsActiveFilters), _.moneyAccount)
      .filter(filterContent(contentFilter.getOrElse(state.contentFilter)))
      .map(EnhancedTransaction.enhance(categories.getOrElse(state.categories), moneyAccounts.getOrElse(state.moneyAccounts)))
      .sorted(new TransactionsOrdering(transactionsSorting.getOrElse(state.transactionsSorting)))
  }

  def filterContent(content: String)(transaction: Transaction): Boolean = {
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


  def updateMoneyAccountsWithTransaction(
      trans: Transaction,
      from: LocalDate,
      to: LocalDate,
      mas: Map[Int, EnhancedMoneyAccount],
      action: MAUpdateAction
  ): Map[Int, EnhancedMoneyAccount] = {
    if (trans.date.isBefore(to.asInstanceOf[ChronoLocalDate])) {
      trans.transactionType match {
        case TransactionType.Income =>
          mas + (trans.moneyAccount -> updateMoneyAccount(mas(trans.moneyAccount), action, MAUpdateOperation.Add, trans, from))
        case TransactionType.Expense =>
          mas + (trans.moneyAccount -> updateMoneyAccount(
            mas(trans.moneyAccount), action, MAUpdateOperation.Remove, trans, from
          ))
        case TransactionType.Transfer =>
          val updated = mas + (trans.moneyAccount -> updateMoneyAccount(
            mas(trans.moneyAccount), action, MAUpdateOperation.Remove, trans, from
          ))
          updated + (trans.destinationMoneyAccountId.get -> updateMoneyAccount(
            mas(trans.destinationMoneyAccountId.get), action, MAUpdateOperation.Add, trans, from
          ))
      }
    } else
      mas
  }

  def updateMoneyAccount(
      ma: EnhancedMoneyAccount,
      action: MAUpdateAction,
      op: MAUpdateOperation,
      trans: Transaction,
      from: LocalDate
  ): EnhancedMoneyAccount = {
    val realOp = MAOperations(action)(op)
    if (trans.date.isAfter(from.asInstanceOf[ChronoLocalDate]))
      ma.copy(periodStatus = ma.periodStatus.copy(end = realOp(ma.periodStatus.end, trans.amount)))
    else
      ma.copy(
        periodStatus = ma.periodStatus
          .copy(realOp(ma.periodStatus.start, trans.amount), realOp(ma.periodStatus.end, trans.amount))
      )
  }
}