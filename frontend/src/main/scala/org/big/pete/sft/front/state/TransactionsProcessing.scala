package org.big.pete.sft.front.state

import japgolly.scalajs.react.Callback
import org.big.pete.BPJson
import org.big.pete.react.MICheckbox
import org.big.pete.react.MICheckbox.Status
import org.big.pete.sft.domain.{DeleteTransactions, EnhancedAccount, MassEditTransactions, ShiftStrategy, TrackingEdit, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.domain.AccountUpdateAction
import org.big.pete.sft.front.utilz.getWalletPermalink

import java.time.LocalDate


trait TransactionsProcessing extends DataLoad {
  import org.big.pete.sft.domain.Implicits._

  def checkTransaction(status: MICheckbox.Status, id: String): Callback = $.modState { state =>
    if (id == CheckAllId) {
      status match {
        case Status.none => state.copy(checkedTransactions = Set.empty)
        case Status.indeterminate => state
        case Status.checkedStatus => state.copy(checkedTransactions = state.displayTransactions.map(_.id).toSet)
      }
    } else
      state.copy(checkedTransactions = modStateForSet(status, state, _.checkedTransactions, id.toInt))
  }

  def saveTransaction(
      id: Option[Int],
      date: LocalDate,
      transactionType: TransactionType,
      amount: BigDecimal,
      description: String,
      category: Int,
      moneyAccount: Int,
      currency: String,
      destinationAmount: Option[BigDecimal],
      destinationMoneyAccountId: Option[Int],
      destinationCurrency: Option[String]
  ): Callback = {
    $.props.flatMap { props =>
      val account = getWalletPermalink(props.activePage).getOrElse("")
      val method = if (id.isDefined) "POST" else "PUT"

      ajaxUpdate[Transaction](
        method,
        "/" + account + "/transactions",
        BPJson.write(
          Transaction(
            id.getOrElse(-1), date, transactionType, amount, description, category, moneyAccount, currency,
            TransactionTracking.None, destinationAmount, destinationMoneyAccountId, destinationCurrency, None
          )
        ),
        transaction => $.modState { state =>
          val updatedMA = {
            if (id.isDefined) {
              val oldTransaction = state.transactions.find(_.id == id.get).get
              val removedTransactionMAs = updateAccountsWithTransaction(
                oldTransaction, state.from, state.to, state.moneyAccounts, AccountUpdateAction.Reverse
              )
              updateAccountsWithTransaction(transaction, state.from, state.to, removedTransactionMAs, AccountUpdateAction.Attach)
            } else
              updateAccountsWithTransaction(transaction, state.from, state.to, state.moneyAccounts, AccountUpdateAction.Attach)
          }
          updateStateWithTransaction(state, state.transactions.filter(_.id != transaction.id) ++ List(transaction), updatedMA)
        }
      )
    }
  }

  def transactionTrackingClick(id: Int, tracking: TransactionTracking): Callback = {
    val newTracking = tracking match {
      case TransactionTracking.None => TransactionTracking.Verified
      case TransactionTracking.Auto => TransactionTracking.Verified
      case TransactionTracking.Verified => TransactionTracking.None
    }

    $.props.flatMap { props =>
      val account = getWalletPermalink(props.activePage).getOrElse("")
      ajaxUpdate[Transaction](
        "POST",
        "/" + account + "/transactions/tracking",
        BPJson.write(TrackingEdit(id, newTracking)),
        transaction => $.modState { state =>
          updateStateWithTransaction(state, state.transactions.filter(_.id != id) ++ List(transaction), state.moneyAccounts)
        }
      )
    }
  }

  def deleteTransaction(id: Int): Callback = {
    $.props.flatMap { props =>
      val account = getWalletPermalink(props.activePage).getOrElse("")
      ajaxUpdate[String](
        "DELETE",
        "/" + account + "/transactions/" + id.toString,
        "",
        _ => $.modState { state =>
          val removedTransaction = state.transactions.find(_.id == id).get
          val updatedMA = updateAccountsWithTransaction(
            removedTransaction, state.from, state.to, state.moneyAccounts, AccountUpdateAction.Reverse
          )
          updateStateWithTransaction(state, state.transactions.filter(_.id != id), updatedMA)
        }
      )
    }
  }

  def deleteTransactions(ids: Set[Int]): Callback = {
    $.props.flatMap { props =>
      val account = getWalletPermalink(props.activePage).getOrElse("")
      ajaxUpdate[Int](
        "DELETE",
        "/" + account + "/transactions",
        BPJson.write(DeleteTransactions(ids.toList)),
        _ => $.modState(_.copy(checkedTransactions = Set.empty)) >> refreshWallet(account).toCallback
      )
    }
  }

  def massEditTransactions(ids: Set[Int], newCat: Option[Int], newMoneyAccount: Option[Int]): Callback = {
    $.props.flatMap { props =>
      val account = getWalletPermalink(props.activePage).getOrElse("")
      ajaxUpdate[Int](
        "POST",
        "/" + account + "/transactions/mass-edit",
        BPJson.write(MassEditTransactions(ids.toList, ShiftStrategy(newCat), ShiftStrategy(newMoneyAccount))),
        _ => $.modState(_.copy(checkedTransactions = Set.empty)) >> refreshWallet(account).toCallback
      )
    }
  }

  private def updateStateWithTransaction(
      state: State,
      newTransactions: List[Transaction],
      updatedMoneyAccounts: Map[Int, EnhancedAccount]
  ): State = {
    state.copy(
      moneyAccounts = updatedMoneyAccounts,
      transactions = newTransactions,
      displayTransactions = filterTransactions(state, Some(newTransactions))
    )
  }
}
