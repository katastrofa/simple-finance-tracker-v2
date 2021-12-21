package org.big.pete.sft.front

import cats.effect.{IO, SyncIO}
import cats.syntax.ParallelSyntax
import japgolly.scalajs.react.{CtorType, React, ReactFormEventFromInput, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.util.EffectCatsEffect.io
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Account, Category, EnhancedMoneyAccount, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.components.header.{Sidenav, SidenavFilters, TopHeader}
import org.big.pete.sft.front.components.main.{Accounts, Categories, MoneyAccounts, Transactions}
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction, sample}
import org.big.pete.sft.front.utilz.getAccountPermalink
import org.scalajs.dom.console

import java.time.LocalDate
import scala.annotation.nowarn


object FullPage extends ParallelSyntax {
  case class Props(from: LocalDate, to: LocalDate, router: RouterCtl[SftPages], activePage: SftPages)
  case class State(
      from: LocalDate,
      to: LocalDate,
      lastActivePage: Option[SftPages],
      activeFilter: Option[FiltersOpen],
      transactionTypeActiveFilters: Set[TransactionType],
      trackingActiveFilters: Set[TransactionTracking],
      contentFilter: String,
      categoriesActiveFilters: Set[Int],
      moneyAccountsActiveFilters: Set[Int],
      checkedTransactions: Set[Int],

      accounts: List[Account],
      categories: Map[Int, Category],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      transactions: List[Transaction],

      categoryTree: List[CategoryTree],
      displayTransactions: List[EnhancedTransaction]
  )

  class Backend($: BackendScope[Props, State]) {
    /// TODO: Load valid data
    def setFromDate(newFrom: LocalDate): SyncIO[LocalDate] = {
      $.state.map(state => state.from -> state.to)
        .flatMap { case (from, to) =>
          if (newFrom.isBefore(to))
            $.modState(_.copy(from = newFrom)) >> SyncIO.pure(newFrom)
          else
            SyncIO.pure(from)
        }
    }

    /// TODO: Load valid data
    def setToDate(newTo: LocalDate): SyncIO[LocalDate] = {
      $.state.map(state => state.from -> state.to)
        .flatMap { case (from, to) =>
          if (newTo.isAfter(from))
            $.modState(_.copy(to = newTo)) >> SyncIO.pure(newTo)
          else
            SyncIO.pure(to)
        }
    }

    def setActiveFilter(opened: FiltersOpen): SyncIO[Unit] = $.modState { state =>
      if (state.activeFilter.contains(opened))
        state.copy(activeFilter = None)
      else
        state.copy(activeFilter = Some(opened))
    }

    protected def modStateForSet[E](status: MICheckbox.Status, state: State, set: State => Set[E], element: E): Set[E] =
      if (status == MICheckbox.Status.checkedStatus) set(state) + element else set(state) - element

    def setTtFilter(status: MICheckbox.Status, tt: String): SyncIO[Unit] = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.transactionTypeActiveFilters, TransactionType.withName(tt))
      state.copy(
        transactionTypeActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, transactionTypeActiveFilters = Some(newFilter))
      )
    }

    def setTrackingFilter(status: MICheckbox.Status, tracking: String): SyncIO[Unit] = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.trackingActiveFilters, TransactionTracking.withName(tracking))
      state.copy(
        trackingActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, trackingActiveFilters = Some(newFilter))
      )
    }

    def setContentFilter(e: ReactFormEventFromInput): SyncIO[Unit] = $.modState { state =>
      val newFilter = e.target.value.trim
      state.copy(
        contentFilter = newFilter,
        displayTransactions = filterTransactions(state, contentFilter = Some(newFilter))
      )
    }

    def setCategoriesFilter(status: MICheckbox.Status, catId: String): SyncIO[Unit] = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.categoriesActiveFilters, catId.toInt)
      state.copy(
        categoriesActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, categoriesActiveFilters = Some(newFilter))
      )
    }

    def setMoneyAccountsFilter(status: MICheckbox.Status, moneyAccountId: String): SyncIO[Unit] = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.moneyAccountsActiveFilters, moneyAccountId.toInt)
      state.copy(
        moneyAccountsActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, moneyAccountsActiveFilters = Some(newFilter))
      )
    }

    def checkTransaction(status: MICheckbox.Status, id: String): SyncIO[Unit] = $.modState { state =>
      state.copy(checkedTransactions = modStateForSet(status, state, _.checkedTransactions, id.toInt))
    }
    /// TODO: Do this
    @nowarn
    def transactionTrackingClick(id: Int, tracking: TransactionTracking): SyncIO[Unit] = SyncIO.unit

    /// TODO: Do this
    def loadAccounts: IO[List[Account]] = {
      IO.pure(sample.accounts)
    }

    /// TODO: Do this
    def loadTransactions(accountPermalink: String): IO[List[Transaction]] = {
      IO.delay {
        val accountId = sample.accounts.find(_.permalink == accountPermalink).get.id
        val categoryIds = sample.categories.filter(_.accountId == accountId).map(_.id).toSet
        sample.transactions.filter(transaction => categoryIds.contains(transaction.categoryId))
      }
    }

    /// TODO: Do this
    def loadMoneyAccounts(accountPermalink: String): IO[Map[Int, EnhancedMoneyAccount]] = {
      IO.delay {
        val accountId = sample.accounts.find(_.permalink == accountPermalink).get.id
        sample.moneyAccounts.filter(_._1 == accountId).map { case (_, ma) => ma.id -> ma }.toMap
      }
    }

    /// TODO: Do this
    def loadCategories(accountPermalink: String): IO[Map[Int, Category]] = {
      IO.delay {
        val accountId = sample.accounts.find(_.permalink == accountPermalink).get.id
        sample.categories.filter(_.accountId == accountId).map(cat => cat.id -> cat).toMap
      }
    }

    private def loadMainPage(props: Props, state:State): VdomElement = props.activePage match {
      case SftMain.AccountsSelectionPage =>
        Accounts.component.apply(Accounts.Props(props.router, state.accounts))
      case SftMain.TransactionsPage(_) =>
        Transactions.component.apply(Transactions.Props(state.displayTransactions, checkTransaction, transactionTrackingClick))
      case SftMain.CategoriesPage(_) =>
        Categories.component.apply(Categories.Props(state.categoryTree))
      case SftMain.MoneyAccountsPage(_) =>
        MoneyAccounts.component.apply(MoneyAccounts.Props(state.moneyAccounts.values.toList))
    }


    def loadDataAndPage(props: Props, state: State): IO[VdomElement] = {
      val ioElement = (state.lastActivePage, props.activePage) match {
        case (Some(_), SftMain.AccountsSelectionPage) =>
          IO.delay(loadMainPage(props, state))
        case (None, SftMain.AccountsSelectionPage) =>
          for {
            accounts <- loadAccounts
            newState = state.copy(lastActivePage = Some(props.activePage), accounts = accounts)
            _ <- $.setStateAsync(newState)
            page = loadMainPage(props, newState)
          } yield page
        case (Some(lastPage), newPage) if getAccountPermalink(lastPage) == getAccountPermalink(newPage) =>
          IO.delay(loadMainPage(props, state))
        case (oldPage, page) =>
          val account = getAccountPermalink(page).get
          val dataFetch = (
            if (oldPage.isEmpty) loadAccounts else IO.pure(state.accounts),
            loadCategories(account),
            loadMoneyAccounts(account),
            loadTransactions(account)
          ).parTupled

          dataFetch.flatMap { case (accounts, cats, moneyAccounts, transactions) =>
            val newState = state.copy(
              lastActivePage = Some(props.activePage),
              accounts = accounts,
              categories = cats,
              categoryTree = CategoryTree.generateTree(cats.values.toList),
              moneyAccounts = moneyAccounts,
              transactions = transactions,
              displayTransactions = filterTransactions(state, Some(transactions), Some(cats), Some(moneyAccounts))
            )

            $.setStateAsync(newState)
              .map(_ => loadMainPage(props, newState))
          }
      }

      ioElement.map { el =>
        console.log(s"${props.activePage.toString} - loaded.")
        el
      }
    }

    def render(props: Props, state: State): VdomElement = {
      val mainPage = React.Suspense(
        <.div("Loading ..."),
        loadDataAndPage(props, state)
      )
      React.

      ReactFragment(
        <.header(
          /// TODO: save date change to cookie
          TopHeader.component(TopHeader.Props(None, None, setFromDate, setToDate)),
          Sidenav.component(Sidenav.Props(
            Sidenav.TopProps(props.router, props.activePage),
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
              SidenavFilters.CategoriesProps(
                state.categoriesActiveFilters,
                setCategoriesFilter,
                state.categoryTree
              ),
              SidenavFilters.MoneyAccountProps(
                state.moneyAccountsActiveFilters,
                setMoneyAccountsFilter,
                state.moneyAccounts.values.toList
              )
            )
          ))
        ),
        mainPage
      )
    }

    private def filterTransactions(
        state: State,
        transactions: Option[List[Transaction]] = None,
        categories: Option[Map[Int, Category]] = None,
        moneyAccounts: Option[Map[Int, EnhancedMoneyAccount]] = None,
        transactionTypeActiveFilters: Option[Set[TransactionType]] = None,
        trackingActiveFilters: Option[Set[TransactionTracking]] = None,
        contentFilter: Option[String] = None,
        categoriesActiveFilters: Option[Set[Int]] = None,
        moneyAccountsActiveFilters: Option[Set[Int]] = None
    ): List[EnhancedTransaction] = {
      transactions.getOrElse(state.transactions)
        .filterNonEmpty(transactionTypeActiveFilters.getOrElse(state.transactionTypeActiveFilters), _.transactionType)
        .filterNonEmpty(trackingActiveFilters.getOrElse(state.trackingActiveFilters), _.tracking)
        .filterNonEmpty(categoriesActiveFilters.getOrElse(state.categoriesActiveFilters), _.categoryId)
        .filterNonEmpty(moneyAccountsActiveFilters.getOrElse(state.moneyAccountsActiveFilters), _.moneyAccount)
        .filter(filterContent(contentFilter.getOrElse(state.contentFilter)))
        .map(EnhancedTransaction.enhance(categories.getOrElse(state.categories), moneyAccounts.getOrElse(state.moneyAccounts)))
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
      def filterNonEmpty[A](set: Set[A])(condition: (T, Set[A]) => Boolean): List[T] =
        if (set.isEmpty) list else list.filter(t => condition(t, set))

      def filterNonEmpty[A](set: Set[A], zoom: T => A): List[T] =
        if (set.isEmpty) list else list.filter(t => set.contains(zoom(t)))
    }
  }

  protected def propsToState(props: Props): State =
    State(props.from, props.to, None, None, Set.empty, Set.empty, "", Set.empty, Set.empty, Set.empty, List.empty, Map.empty, Map.empty, List.empty, List.empty, List.empty)


  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(propsToState)
    .renderBackend[Backend]
    .build
}
