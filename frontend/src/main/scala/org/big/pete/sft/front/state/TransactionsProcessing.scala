package org.big.pete.sft.front.state

import japgolly.scalajs.react.Callback
import org.big.pete.BPJson
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{EnhancedMoneyAccount, TrackingEdit, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.domain.MAUpdateAction
import org.big.pete.sft.front.utilz.getAccountPermalink

import java.time.LocalDate


trait TransactionsProcessing extends DataLoad {
  import org.big.pete.sft.domain.Implicits._

  def checkTransaction(status: MICheckbox.Status, id: String): Callback = $.modState { state =>
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
      destinationAmount: Option[BigDecimal],
      destinationMoneyAccountId: Option[Int]
  ): Callback = {
    $.props.flatMap { props =>
      val account = getAccountPermalink(props.activePage).getOrElse("")
      val method = if (id.isDefined) "POST" else "PUT"

      ajaxUpdate[Transaction](
        method,
        "/" + account + "/transactions",
        BPJson.write(
          Transaction(
            id.getOrElse(-1), date, transactionType, amount, description, category, moneyAccount,
            TransactionTracking.None, destinationAmount, destinationMoneyAccountId, None
          )
        ),
        transaction => $.modState { state =>
          val updatedMA = {
            if (id.isDefined) {
              val oldTransaction = state.transactions.find(_.id == id.get).get
              val removedTransactionMAs = updateMoneyAccountsWithTransaction(
                oldTransaction, state.from, state.to, state.moneyAccounts, MAUpdateAction.Reverse
              )
              updateMoneyAccountsWithTransaction(transaction, state.from, state.to, removedTransactionMAs, MAUpdateAction.Attach)
            } else
              updateMoneyAccountsWithTransaction(transaction, state.from, state.to, state.moneyAccounts, MAUpdateAction.Attach)
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
      val account = getAccountPermalink(props.activePage).getOrElse("")
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
      val account = getAccountPermalink(props.activePage).getOrElse("")
      ajaxUpdate[String](
        "DELETE",
        "/" + account + "/transactions/" + id.toString,
        "",
        _ => $.modState { state =>
          val removedTransaction = state.transactions.find(_.id == id).get
          val updatedMA = updateMoneyAccountsWithTransaction(
            removedTransaction, state.from, state.to, state.moneyAccounts, MAUpdateAction.Reverse
          )
          updateStateWithTransaction(state, state.transactions.filter(_.id != id), updatedMA)
        }
      )
    }
  }

  def updateStateWithTransaction(
      state: State,
      newTransactions: List[Transaction],
      updatedMoneyAccounts: Map[Int, EnhancedMoneyAccount]
  ): State = {
    state.copy(
      moneyAccounts = updatedMoneyAccounts,
      transactions = newTransactions,
      displayTransactions = filterTransactions(state, Some(newTransactions))
    )
  }
}
