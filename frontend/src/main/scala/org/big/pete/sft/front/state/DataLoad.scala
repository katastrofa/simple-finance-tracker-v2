package org.big.pete.sft.front.state

import japgolly.scalajs.react.callback.AsyncCallback
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.sft.domain.{Account, Category, Currency, EnhancedMoneyAccount, Transaction}
import org.big.pete.sft.front.domain.CategoryTree

import java.time.LocalDate


trait DataLoad extends Base {
  import org.big.pete.sft.domain.Implicits._

  def loadAccounts: AsyncCallback[List[Account]] = {
    ajaxCall[List[Account]]("GET", "/accounts", None, List.empty)
  }

  def loadCurrencies: AsyncCallback[List[Currency]] = {
    ajaxCall[List[Currency]]("GET", "/currencies", None, List.empty)
  }

  def loadTransactions(accountPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[List[Transaction]] = {
    val apiPath = "/" + accountPermalink + "/transactions?" +
      "start=" + start.format(ReactDatePicker.DateFormat) +
      "&end=" + end.format(ReactDatePicker.DateFormat)
    ajaxCall[List[Transaction]]("GET", apiPath, None, List.empty)
  }

  def loadMoneyAccounts(accountPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[Map[Int, EnhancedMoneyAccount]] = {
    val apiPath = "/" + accountPermalink + "/money-accounts?" +
      "start=" + start.format(ReactDatePicker.DateFormat) +
      "&end=" + end.format(ReactDatePicker.DateFormat)
    ajaxCall[List[EnhancedMoneyAccount]]("GET", apiPath, None, List.empty)
      .map(_.map(ma => ma.id -> ma).toMap)
  }

  def loadCategories(accountPermalink: String): AsyncCallback[Map[Int, Category]] = {
    ajaxCall[List[Category]]("GET", "/" + accountPermalink + "/categories", None, List.empty)
      .map(_.map(cat => cat.id -> cat).toMap)
  }

  def refreshAccount(account: String): AsyncCallback[Unit] = {
    $.state.async.flatMap { state =>
      val data = AsyncCallback.sequence(
        List(
          if (state.accounts.isEmpty) loadAccounts else AsyncCallback.pure(state.accounts),
          if (state.currencies.isEmpty) loadCurrencies else AsyncCallback.pure(state.currencies),
          loadCategories(account),
          loadMoneyAccounts(account, state.from, state.to),
          loadTransactions(account, state.from, state.to)
        )
      )

      data.flatMap { dataList =>
        val accounts = dataList.head.asInstanceOf[List[Account]]
        val currencies = dataList(1).asInstanceOf[List[Currency]].map(cur => cur.id -> cur).toMap
        val cats = dataList(2).asInstanceOf[Map[Int, Category]]
        val moneyAccounts = dataList(3).asInstanceOf[Map[Int, EnhancedMoneyAccount]]
        val transactions = dataList(4).asInstanceOf[List[Transaction]]

        $.modStateAsync(
          s => s.copy(
            accounts = accounts,
            currencies = currencies,
            categories = cats,
            moneyAccounts = moneyAccounts,
            transactions = transactions,
            categoryTree = CategoryTree.generateTree(cats.values.toList),
            displayTransactions = filterTransactions(s, Some(transactions), Some(cats), Some(moneyAccounts), currencies = Some(currencies))
          )
        )
      }
    }
  }
}
