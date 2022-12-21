package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.callback.{AsyncCallback, Callback, CallbackTo}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import org.big.pete.sft.domain.{Account, Currency}
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, SftPages}
import org.big.pete.sft.front.components.header.{Sidenav, SidenavFilters, TopHeader}
import org.big.pete.sft.front.components.main.moneyaccount
import org.big.pete.sft.front.components.main.{Accounts, Categories}
import org.big.pete.sft.front.components.main.transactions
import org.big.pete.sft.front.state.{CookieStorage, DataUpdate, DefaultSorting, Filtering, Props, State, TransactionsProcessing}
import org.big.pete.sft.front.domain.{Order, SortingColumn}
import org.big.pete.sft.front.utilz.getAccountPermalink

import java.time.LocalDate


object SftState {

  class Backend(val $: BackendScope[Props, State]) extends DataUpdate with Filtering with TransactionsProcessing {

    private def setFromDate(newFrom: LocalDate): CallbackTo[LocalDate] = {
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

    private def setToDate(newTo: LocalDate): CallbackTo[LocalDate] = {
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
            isMenuOpen = false,
            accounts = ajaxData.head.asInstanceOf[List[Account]],
            currencies = ajaxData.last.asInstanceOf[List[Currency]].map(cur => cur.id -> cur).toMap
          ))
        } yield 3

        case (None, page) if getAccountPermalink(page).nonEmpty =>
          val account = getAccountPermalink(page)
          $.modStateAsync(_.copy(isMenuOpen = false)) >> refreshAccount(account.get).map(_ => 10)

        case (Some(_), AccountsSelectionPage) =>
          $.modStateAsync(_.copy(isMenuOpen = false)) >> AsyncCallback.pure(1)

        case (Some(old), page) if getAccountPermalink(old) != getAccountPermalink(page) =>
          val account = getAccountPermalink(page)
          $.modStateAsync(_.copy(isMenuOpen = false)) >> refreshAccount(account.get).map(_ => 15)

        case _ =>
          $.modStateAsync(_.copy(isMenuOpen = false)) >> AsyncCallback.pure(999)
      }

      aCall.toCallback
    }

    def menuClick: Callback = $.modState { state =>
      state.copy(isMenuOpen = !state.isMenuOpen)
    }

    def render(props: Props, state: State): Unmounted[FullPage.Props, Unit, Unit] = {
      FullPage.component(FullPage.Props(
        props.activePage,
        state.isMenuOpen,
        TopHeader.Props(state.from, state.to, setFromDate, setToDate, menuClick),
        Sidenav.TopProps(props.router, props.activePage, onPageClick),
        SidenavFilters.Props(
          state.activeFilter,
          setActiveFilter,
          SidenavFilters.TransactionsProps(
            state.transactionTypeActiveFilters,
            setTtFilter,
            state.trackingActiveFilters,
            setTrackingFilter,
            state.contentFilter,
            setContentFilter
          ),
          SidenavFilters.CategoriesProps(state.categoriesActiveFilters, setCategoriesFilter, state.categoryTree),
          SidenavFilters.MoneyAccountProps(state.moneyAccountsActiveFilters, setMoneyAccountsFilter, state.moneyAccounts.values.toList)
        ),

        Accounts.Props(state.accounts, props.activePage, props.router, onPageClick, saveAccount),
        transactions.Page.Props(
          state.displayTransactions,
          state.categoryTree,
          state.moneyAccounts,
          state.checkedTransactions,
          state.transactionsSorting,
          clickOrdering,
          checkTransaction,
          transactionTrackingClick,
          saveTransaction,
          deleteTransactions,
          massEditTransactions
        ),
        Categories.Props(state.categoryTree, saveCategory, deleteCategory),
        moneyaccount.Page.Props(state.moneyAccounts.values.toList, state.currencies, saveMoneyAccount, deleteMoneyAccount)
      ))
    }

  }

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState(State(
      CookieStorage.getBrowserSettings.from, CookieStorage.getBrowserSettings.to, isMenuOpen = false, None, Set.empty, Set.empty,
      "", Set.empty, Set.empty, Set.empty, DefaultSorting, List.empty, Map.empty, Map.empty, Map.empty, List.empty, List.empty,
      List.empty
    ))
    .renderBackend[Backend]
    .componentDidMount(component => component.backend.onPageClick(component.props.activePage, None))
    .build
}

