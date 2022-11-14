package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.callback.{AsyncCallback, Callback, CallbackTo}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import org.big.pete.sft.domain.{Account, Currency}
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, SftPages}
import org.big.pete.sft.front.state.{CookieStorage, DataUpdate, DefaultSorting, Filtering, Props, State, TransactionsProcessing}
import org.big.pete.sft.front.domain.{Order, SortingColumn}
import org.big.pete.sft.front.utilz.getAccountPermalink

import java.time.LocalDate


object SftState {

  class Backend(val $: BackendScope[Props, State]) extends DataUpdate with Filtering with TransactionsProcessing {

    def setFromDate(newFrom: LocalDate): CallbackTo[LocalDate] = {
      def updateFrom(state: State): CallbackTo[LocalDate] = {
        if (newFrom.isBefore(state.to))
          $.setState(state.copy(from = newFrom)) >> CallbackTo.pure(newFrom)
        else
          CallbackTo.pure(state.from)
      }

      for {
        state <- $.state
        props <- $.props
        from <- updateFrom(state)
        _ = CookieStorage.updateBrowserSettings(CookieStorage.getBrowserSettings.copy(from = from))
        account = getAccountPermalink(props.activePage)
        _ <- if (account.isDefined) refreshAccount(account.get).toCallback else Callback.empty
      } yield from
    }

    def setToDate(newTo: LocalDate): CallbackTo[LocalDate] = {
      for {
        state <- $.state
        props <- $.props
        to <- if (newTo.isAfter(state.from)) $.setState(state.copy(to = newTo)) >> CallbackTo.pure(newTo) else CallbackTo.pure(state.to)
        _ = CookieStorage.updateBrowserSettings(CookieStorage.getBrowserSettings.copy(to = to))
        account = getAccountPermalink(props.activePage)
        _ <- if (account.isDefined) refreshAccount(account.get).toCallback else Callback.empty
      } yield to
    }

    def clickOrdering(column: SortingColumn): Callback = $.modState { state =>
      val newEntry: List[(SortingColumn, Order)] = state.transactionsSorting.find(_._1 == column) match {
        case Some((_, Order.Asc)) => List(column -> Order.Desc)
        case Some((_, Order.Desc)) => List.empty
        case None => List(column -> Order.Asc)
      }
      val newSorting = newEntry ++ state.transactionsSorting.filter(_._1 != column)
      state.copy(
        transactionsSorting = newSorting,
        displayTransactions = filterTransactions(state, transactionsSorting = Some(newSorting))
      )
    }

    def onPageClick(newPage: SftPages, oldPage: Option[SftPages]): Callback = {
      val aCall = (oldPage, newPage) match {
        case (None, AccountsSelectionPage) => for {
          ajaxData <- AsyncCallback.sequence(List(loadAccounts, loadCurrencies))
          _ <- $.modStateAsync(_.copy(
            accounts = ajaxData.head.asInstanceOf[List[Account]],
            currencies = ajaxData.last.asInstanceOf[List[Currency]]
          ))
        } yield 3

        case (None, page) if getAccountPermalink(page).nonEmpty =>
          val account = getAccountPermalink(page)
          refreshAccount(account.get).map(_ => 10)

        case (Some(_), AccountsSelectionPage) =>
          AsyncCallback.pure(1)

        case (Some(old), page) if getAccountPermalink(old) != getAccountPermalink(page) =>
          val account = getAccountPermalink(page)
          refreshAccount(account.get).map(_ => 15)

        case _ =>
          AsyncCallback.pure(999)
      }

      aCall.toCallback
    }

    def render(props: Props, state: State): Unmounted[FullPage.Props, Unit, Unit] = {
      FullPage.component.apply(FullPage.Props(
        props.router,
        state.from,
        state.to,
        props.activePage,
        state.activeFilter,
        state.transactionTypeActiveFilters,
        state.trackingActiveFilters,
        state.contentFilter,
        state.categoriesActiveFilters,
        state.moneyAccountsActiveFilters,
        state.checkedTransactions,
        state.transactionsSorting,
        state.accounts,
        state.currencies,
        state.moneyAccounts,
        state.categoryTree,
        state.displayTransactions,
        setFromDate,
        setToDate,
        setActiveFilter,
        setTtFilter,
        setTrackingFilter,
        setContentFilter,
        setCategoriesFilter,
        setMoneyAccountsFilter,
        checkTransaction,
        transactionTrackingClick,
        onPageClick,
        clickOrdering,
        saveAccount,
        saveCategory,
        saveMoneyAccount,
        saveTransaction,
        deleteCategory,
        deleteMoneyAccount,
        deleteTransactions,
        massEditTransactions
      ))
    }

  }

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(_ => State(CookieStorage.getBrowserSettings.from, CookieStorage.getBrowserSettings.to, None, Set.empty, Set.empty, "", Set.empty, Set.empty, Set.empty, DefaultSorting, List.empty, List.empty, Map.empty, Map.empty, List.empty, List.empty, List.empty))
    .renderBackend[Backend]
    .componentDidMount(component => component.backend.onPageClick(component.props.activePage, None))
    .build
}

