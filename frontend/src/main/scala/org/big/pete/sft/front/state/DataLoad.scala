package org.big.pete.sft.front.state

import japgolly.scalajs.react.callback.AsyncCallback
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.sft.domain.{Category, EnhancedMoneyAccount, GeneralData, Transaction}
import org.big.pete.sft.front.domain.CategoryTree

import java.time.LocalDate


trait DataLoad extends Base {
  import org.big.pete.sft.domain.Implicits._

//  def loadAccounts: AsyncCallback[List[FullAccount]] =
//    ajaxCall[List[FullAccount]]("GET", "/accounts", None, List.empty)
//
//  def loadCurrencies: AsyncCallback[List[Currency]] =
//    ajaxCall[List[Currency]]("GET", "/currencies", None, List.empty)
//
//  def loadPatrons: AsyncCallback[List[SimpleUser]] =
//    ajaxCall[List[SimpleUser]]("GET", "/patrons", None, List.empty)

  def loadGeneralData: AsyncCallback[GeneralData] =
    ajaxCall[GeneralData]("GET", "/general", None, GeneralData(domain.emptyMe, List.empty, List.empty, List.empty))

  private def loadTransactions(accountPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[List[Transaction]] = {
    val apiPath = "/" + accountPermalink + "/transactions?" +
      "start=" + start.format(ReactDatePicker.DateFormat) +
      "&end=" + end.format(ReactDatePicker.DateFormat)
    ajaxCall[List[Transaction]]("GET", apiPath, None, List.empty)
  }

  private def loadMoneyAccounts(accountPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[Map[Int, EnhancedMoneyAccount]] = {
    val apiPath = "/" + accountPermalink + "/money-accounts?" +
      "start=" + start.format(ReactDatePicker.DateFormat) +
      "&end=" + end.format(ReactDatePicker.DateFormat)
    ajaxCall[List[EnhancedMoneyAccount]]("GET", apiPath, None, List.empty)
      .map(_.map(ma => ma.id -> ma).toMap)
  }

  private def loadCategories(accountPermalink: String): AsyncCallback[Map[Int, Category]] = {
    ajaxCall[List[Category]]("GET", "/" + accountPermalink + "/categories", None, List.empty)
      .map(_.map(cat => cat.id -> cat).toMap)
  }

  def refreshAccount(account: String): AsyncCallback[Unit] = {
    $.state.async.flatMap { state =>
      val data = AsyncCallback.sequence(
        List(
          if (state.accounts.isEmpty || state.currencies.isEmpty)
            loadGeneralData
          else
            AsyncCallback.pure(GeneralData(state.me, state.availablePatrons, state.currencies.values.toList, state.accounts)),
          loadCategories(account),
          loadMoneyAccounts(account, state.from, state.to),
          loadTransactions(account, state.from, state.to)
        )
      )

      data.flatMap { dataList =>
        val generalData = dataList.head.asInstanceOf[GeneralData]
        val currencies = generalData.currencies.map(cur => cur.id -> cur).toMap
        val cats = dataList(2).asInstanceOf[Map[Int, Category]]
        val moneyAccounts = dataList(3).asInstanceOf[Map[Int, EnhancedMoneyAccount]]
        val transactions = dataList(4).asInstanceOf[List[Transaction]]

        $.modStateAsync { s =>
          s.copy(
            me = generalData.me,
            availablePatrons = generalData.patrons,
            accounts = generalData.accounts,
            currencies = currencies,
            categories = cats,
            moneyAccounts = moneyAccounts,
            transactions = transactions,
            categoryTree = CategoryTree.generateTree(cats.values.toList),
            displayTransactions = filterTransactions(s, Some(transactions), Some(cats), Some(moneyAccounts), currencies = Some(currencies))
          )
        }
      }
    }
  }
}
