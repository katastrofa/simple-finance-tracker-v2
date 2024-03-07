package org.big.pete.sft.front.state

import japgolly.scalajs.react.callback.AsyncCallback
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.sft.domain.{Category, EnhancedAccount, GeneralData, Transaction}
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

  private def loadTransactions(walletPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[List[Transaction]] = {
    val apiPath = "/" + walletPermalink + "/transactions?" +
      "start=" + start.format(ReactDatePicker.DateFormat) +
      "&end=" + end.format(ReactDatePicker.DateFormat)
    ajaxCall[List[Transaction]]("GET", apiPath, None, List.empty)
  }

  private def loadAccounts(walletPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[Map[Int, EnhancedAccount]] = {
    val apiPath = "/" + walletPermalink + "/accounts?" +
      "start=" + start.format(ReactDatePicker.DateFormat) +
      "&end=" + end.format(ReactDatePicker.DateFormat)
    ajaxCall[List[EnhancedAccount]]("GET", apiPath, None, List.empty)
      .map(_.map(account => account.id -> account).toMap)
  }

  private def loadCategories(walletPermalink: String): AsyncCallback[Map[Int, Category]] = {
    ajaxCall[List[Category]]("GET", "/" + walletPermalink + "/categories", None, List.empty)
      .map(_.map(cat => cat.id -> cat).toMap)
  }

  def refreshWallet(wallet: String): AsyncCallback[Unit] = {
    $.state.async.flatMap { state =>
      val data = AsyncCallback.sequence(
        List(
          if (state.accounts.isEmpty || state.currencies.isEmpty)
            loadGeneralData
          else
            AsyncCallback.pure(GeneralData(state.me, state.availablePatrons, state.currencies.values.toList, state.accounts)),
          loadCategories(wallet),
          loadAccounts(wallet, state.from, state.to),
          loadTransactions(wallet, state.from, state.to)
        )
      )

      data.flatMap { dataList =>
        val generalData = dataList.head.asInstanceOf[GeneralData]
        val currencies = generalData.currencies.map(cur => cur.id -> cur).toMap
        val cats = dataList(2).asInstanceOf[Map[Int, Category]]
        val accounts = dataList(3).asInstanceOf[Map[Int, EnhancedAccount]]
        val transactions = dataList(4).asInstanceOf[List[Transaction]]

        $.modStateAsync { s =>
          s.copy(
            me = generalData.me,
            availablePatrons = generalData.patrons,
            wallets = generalData.wallets,
            currencies = currencies,
            categories = cats,
            accounts = accounts,
            transactions = transactions,
            categoryTree = CategoryTree.generateTree(cats.values.toList),
            displayTransactions = filterTransactions(s, Some(transactions), Some(cats), Some(accounts), currencies = Some(currencies))
          )
        }
      }
    }
  }
}
