package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ReactFormEventFromInput, ScalaComponent}
import japgolly.scalajs.react.callback.{AsyncCallback, Callback, CallbackTo}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.extra.internal.AjaxException
import org.big.pete.BPJson
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Account, Category, EnhancedMoneyAccount, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, SftPages}
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction}
import org.big.pete.sft.front.utilz.getAccountPermalink

import java.time.LocalDate
import scala.annotation.nowarn


object SftState {
  case class Props(initialFrom: LocalDate, initialTo: LocalDate, apiBase: String)
  case class State(
      from: LocalDate,
      to: LocalDate,
      activePage: Option[SftPages],

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
    def setFromDate(newFrom: LocalDate): CallbackTo[LocalDate] = {
      $.state.map(state => state.from -> state.to)
        .flatMap { case (from, to) =>
          if (newFrom.isBefore(to))
            $.modState(_.copy(from = newFrom)) >> CallbackTo.pure(newFrom)
          else
            CallbackTo.pure(from)
        }
    }

    /// TODO: Load valid data
    def setToDate(newTo: LocalDate): CallbackTo[LocalDate] = {
      $.state.map(state => state.from -> state.to)
        .flatMap { case (from, to) =>
          if (newTo.isAfter(from))
            $.modState(_.copy(to = newTo)) >> CallbackTo.pure(newTo)
          else
            CallbackTo.pure(to)
        }
    }

    def setActiveFilter(opened: FiltersOpen): Callback = $.modState { state =>
      if (state.activeFilter.contains(opened))
        state.copy(activeFilter = None)
      else
        state.copy(activeFilter = Some(opened))
    }

    protected def modStateForSet[E](status: MICheckbox.Status, state: State, set: State => Set[E], element: E): Set[E] =
      if (status == MICheckbox.Status.checkedStatus) set(state) + element else set(state) - element

    def setTtFilter(status: MICheckbox.Status, tt: String): Callback = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.transactionTypeActiveFilters, TransactionType.withName(tt))
      state.copy(
        transactionTypeActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, transactionTypeActiveFilters = Some(newFilter))
      )
    }

    def setTrackingFilter(status: MICheckbox.Status, tracking: String): Callback = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.trackingActiveFilters, TransactionTracking.withName(tracking))
      state.copy(
        trackingActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, trackingActiveFilters = Some(newFilter))
      )
    }

    def setContentFilter(e: ReactFormEventFromInput): Callback = $.modState { state =>
      val newFilter = e.target.value.trim
      state.copy(
        contentFilter = newFilter,
        displayTransactions = filterTransactions(state, contentFilter = Some(newFilter))
      )
    }

    def setCategoriesFilter(status: MICheckbox.Status, catId: String): Callback = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.categoriesActiveFilters, catId.toInt)
      state.copy(
        categoriesActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, categoriesActiveFilters = Some(newFilter))
      )
    }

    def setMoneyAccountsFilter(status: MICheckbox.Status, moneyAccountId: String): Callback = $.modState { state =>
      val newFilter = modStateForSet(status, state, _.moneyAccountsActiveFilters, moneyAccountId.toInt)
      state.copy(
        moneyAccountsActiveFilters = newFilter,
        displayTransactions = filterTransactions(state, moneyAccountsActiveFilters = Some(newFilter))
      )
    }

    def checkTransaction(status: MICheckbox.Status, id: String): Callback = $.modState { state =>
      state.copy(checkedTransactions = modStateForSet(status, state, _.checkedTransactions, id.toInt))
    }

    /// TODO: Do this
    @nowarn
    def transactionTrackingClick(id: Int, tracking: TransactionTracking): Callback = Callback.empty

    /// TODO: Do this
    def loadAccounts: AsyncCallback[List[Account]] = {
      import org.big.pete.sft.domain.Implicits._

      $.props.async.flatMap { props =>
        Ajax("GET", props.apiBase + "/accounts")
          .send
          .validateStatusIs(200)(displayException)
          .asAsyncCallback
          .flatMap { response =>
            BPJson.extract[List[Account]](response.responseText) match {
              case Left(value) => displayExceptionStr(value).async >> AsyncCallback.pure(List.empty)
              case Right(value) => AsyncCallback.pure(value)
            }
          }
      }
    }

    /// TODO: Do this
    @nowarn
    def loadTransactions(accountPermalink: String): AsyncCallback[List[Transaction]] = {
//      AsyncCallback.delay {
//        val accountId = sample.accounts.find(_.permalink == accountPermalink).get.id
//        val categoryIds = sample.categories.filter(_.accountId == accountId).map(_.id).toSet
//        sample.transactions.filter(transaction => categoryIds.contains(transaction.categoryId))
//      }
      AsyncCallback.pure(List.empty)
    }

    /// TODO: Do this
    @nowarn
    def loadMoneyAccounts(accountPermalink: String): AsyncCallback[Map[Int, EnhancedMoneyAccount]] = {
//      AsyncCallback.delay {
//        val accountId = sample.accounts.find(_.permalink == accountPermalink).get.id
//        sample.moneyAccounts.filter(_._1 == accountId).map { case (_, ma) => ma.id -> ma }.toMap
//      }
      AsyncCallback.pure(Map.empty)
    }

    /// TODO: Do this
    @nowarn
    def loadCategories(accountPermalink: String): AsyncCallback[Map[Int, Category]] = {
//      AsyncCallback.delay {
//        val accountId = sample.accounts.find(_.permalink == accountPermalink).get.id
//        sample.categories.filter(_.accountId == accountId).map(cat => cat.id -> cat).toMap
//      }
      AsyncCallback.pure(Map.empty)
    }

    def refreshAccount(account: String, newPage: SftPages): AsyncCallback[Unit] = {
      $.state.async.flatMap { state =>
        val data = AsyncCallback.sequence(List(
          if (state.accounts.isEmpty) loadAccounts else AsyncCallback.pure(state.accounts),
          loadCategories(account),
          loadMoneyAccounts(account),
          loadTransactions(account)
        ))

        data.flatMap { dataList =>
          val accounts = dataList.head.asInstanceOf[List[Account]]
          val cats = dataList(1).asInstanceOf[Map[Int, Category]]
          val moneyAccounts = dataList(2).asInstanceOf[Map[Int, EnhancedMoneyAccount]]
          val transactions = dataList(3).asInstanceOf[List[Transaction]]

          $.modStateAsync(s => s.copy(
            activePage = Some(newPage),
            accounts = accounts,
            categories = cats,
            moneyAccounts = moneyAccounts,
            transactions = transactions,
            categoryTree = CategoryTree.generateTree(cats.values.toList),
            displayTransactions = filterTransactions(
              s,
              Some(transactions),
              Some(cats),
              Some(moneyAccounts)
            )
          ))
        }
      }
    }

    def onPageClick(newPage: SftPages, oldPage: Option[SftPages]): Callback = {
      val aCall = (oldPage, newPage) match {
        case (None, AccountsSelectionPage) =>
          loadAccounts.flatMap(accounts => $.modStateAsync(_.copy(accounts = accounts, activePage = Some(AccountsSelectionPage)))).map(_ => 3)
        case (None, page) if getAccountPermalink(page).nonEmpty =>
          val account = getAccountPermalink(page)
          refreshAccount(account.get, page).map(_ => 10)
        case (Some(_), AccountsSelectionPage) =>
          AsyncCallback.pure(1)
        case (Some(old), page) if getAccountPermalink(old) != getAccountPermalink(page) =>
          val account = getAccountPermalink(page)
          refreshAccount(account.get, page).map(_ => 15)
        case _ =>
          AsyncCallback.pure(999)
      }
      aCall.toCallback
    }

    /// TODO: Do this
    def displayException(ex: AjaxException): Callback =
      displayExceptionStr(ex.getMessage)

    /// TODO: Do this
    def displayExceptionStr(error: String): Callback =
      Callback.log(error)

    def publishAccount(name: String, permalink: String): Callback = {
      import org.big.pete.sft.domain.Implicits._

      $.props.flatMap { props =>
        Ajax("PUT", props.apiBase + "/accounts")
          .setRequestContentTypeJsonUtf8
          .send(BPJson.write(Account(-1, name, permalink, None)))
          .validateStatusIs(200)(displayException)
          .onComplete { response =>
            BPJson.extract[Account](response.responseText) match {
              case Left(value) => displayExceptionStr(value)
              case Right(account) => $.modState(state => state.copy(accounts = account :: state.accounts))
            }
          }.asCallback
      }
    }

    def render(state: State): Unmounted[Routing.Props, Unit, Routing.Backend] = {
      Routing.component.apply(Routing.Props(
        state.from,
        state.to,
        state.activePage,
        state.activeFilter,
        state.transactionTypeActiveFilters,
        state.trackingActiveFilters,
        state.contentFilter,
        state.categoriesActiveFilters,
        state.moneyAccountsActiveFilters,
        state.checkedTransactions,
        state.accounts,
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
        publishAccount
      ))
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

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(p => State(p.initialFrom, p.initialTo, None, None, Set.empty, Set.empty, "", Set.empty, Set.empty, Set.empty, List.empty, Map.empty, Map.empty, List.empty, List.empty, List.empty))
    .renderBackend[Backend]
    .componentDidMount(_.backend.onPageClick(AccountsSelectionPage, None))
    .build
}

