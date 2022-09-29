package org.big.pete.sft.front

import io.circe.Decoder
import japgolly.scalajs.react.{CtorType, ReactFormEventFromInput, ScalaComponent}
import japgolly.scalajs.react.callback.{AsyncCallback, Callback, CallbackTo}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import japgolly.scalajs.react.extra.Ajax
import japgolly.scalajs.react.extra.internal.AjaxException
import japgolly.scalajs.react.extra.router.RouterCtl
import org.big.pete.BPJson
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Account, AccountEdit, Category, Currency, EnhancedMoneyAccount, MoneyAccount, PeriodAmountStatus, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, SftPages}
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction}
import org.big.pete.sft.front.utilz.getAccountPermalink

import java.time.LocalDate
import java.time.chrono.ChronoLocalDate
import scala.annotation.nowarn


object SftState {
  case class Props(
      router: RouterCtl[SftPages],
      activePage: SftPages,
      initialFrom: LocalDate,
      initialTo: LocalDate,
      apiBase: String
  )
  case class State(
      from: LocalDate,
      to: LocalDate,

      activeFilter: Option[FiltersOpen],
      transactionTypeActiveFilters: Set[TransactionType],
      trackingActiveFilters: Set[TransactionTracking],
      contentFilter: String,
      categoriesActiveFilters: Set[Int],
      moneyAccountsActiveFilters: Set[Int],
      checkedTransactions: Set[Int],

      accounts: List[Account],
      currencies: List[Currency],
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

    def loadAccounts: AsyncCallback[List[Account]] = {
      import org.big.pete.sft.domain.Implicits._
      ajaxCall[List[Account]]("GET", "/accounts", None, List.empty)
    }

    def loadCurrencies: AsyncCallback[List[Currency]] = {
      import org.big.pete.sft.domain.Implicits._
      ajaxCall[List[Currency]]("GET", "/currencies", None, List.empty)
    }

    def loadTransactions(accountPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[List[Transaction]] = {
      import org.big.pete.sft.domain.Implicits._

      val apiPath = "/" + accountPermalink + "/transactions?" +
        "start=" + start.format(ReactDatePicker.DateFormat) +
        "&end=" + end.format(ReactDatePicker.DateFormat)
      ajaxCall[List[Transaction]]("GET", apiPath, None, List.empty)
    }

    def loadMoneyAccounts(accountPermalink: String, start: LocalDate, end: LocalDate): AsyncCallback[Map[Int, EnhancedMoneyAccount]] = {
      import org.big.pete.sft.domain.Implicits._

      val apiPath = "/" + accountPermalink + "/money-accounts?" +
        "start=" + start.format(ReactDatePicker.DateFormat) +
        "&end=" + end.format(ReactDatePicker.DateFormat)
      ajaxCall[List[EnhancedMoneyAccount]]("GET", apiPath, None, List.empty)
        .map(_.map(ma => ma.id -> ma).toMap)
    }

    def loadCategories(accountPermalink: String): AsyncCallback[Map[Int, Category]] = {
      import org.big.pete.sft.domain.Implicits._

      ajaxCall[List[Category]]("GET", "/" + accountPermalink + "/categories", None, List.empty)
        .map(_.map(cat => cat.id -> cat).toMap)
    }

    def refreshAccount(account: String): AsyncCallback[Unit] = {
      $.state.async.flatMap { state =>
        val data = AsyncCallback.sequence(List(
          if (state.accounts.isEmpty) loadAccounts else AsyncCallback.pure(state.accounts),
          if (state.currencies.isEmpty) loadCurrencies else AsyncCallback.pure(state.currencies),
          loadCategories(account),
          loadMoneyAccounts(account, state.from, state.to),
          loadTransactions(account, state.from, state.to)
        ))

        data.flatMap { dataList =>
          val accounts = dataList.head.asInstanceOf[List[Account]]
          val currencies = dataList(1).asInstanceOf[List[Currency]]
          val cats = dataList(2).asInstanceOf[Map[Int, Category]]
          val moneyAccounts = dataList(3).asInstanceOf[Map[Int, EnhancedMoneyAccount]]
          val transactions = dataList(4).asInstanceOf[List[Transaction]]

          $.modStateAsync(s => s.copy(
            accounts = accounts,
            currencies = currencies,
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

    def saveAccount(oldPermalink: Option[String], id: Option[Int], name: String, permalink: String): Callback = {
      import org.big.pete.sft.domain.Implicits._

      val method = if (id.isDefined) "POST" else "PUT"
      val payload = if (id.isDefined)
        BPJson.write(AccountEdit(oldPermalink.get, id.get, name, permalink, None))
      else
        BPJson.write(Account(-1, name, permalink, None))

      ajaxUpdate[Account](
        method,
        "/accounts",
        payload,
        account => $.modState { state =>
          val newAccounts = state.accounts.filter(_.id != account.id) ++ List(account)
          state.copy(accounts = newAccounts.sortBy(_.name))
        }
      )
    }

    def saveCategory(id: Option[Int], name: String, description: String, parent: Option[Int]): Callback = {
      import org.big.pete.sft.domain.Implicits._

      $.props.flatMap { props =>
        val account = getAccountPermalink(props.activePage).getOrElse("")
        val realParent = parent.flatMap(p => if (p == -42) None else Some(p))
        val realDescription = if (description.nonEmpty) Some(description) else None
        val method = if (id.isDefined) "POST" else "PUT"

        ajaxUpdate[Category](
          method,
          "/" + account + "/categories",
          BPJson.write(Category(id.getOrElse(-1), name, realDescription, realParent, -1, None)),
          cat => $.modState { state =>
            val newCats = state.categories + (cat.id -> cat)
            state.copy(categories = newCats, categoryTree = CategoryTree.generateTree(newCats.values.toList))
          }
        )
      }
    }

    def saveMoneyAccount(id: Option[Int], name: String, startAmount: BigDecimal, currency: String, created: LocalDate): Callback = {
      import org.big.pete.sft.domain.Implicits._

      $.props.flatMap { props =>
        val account = getAccountPermalink(props.activePage).getOrElse("")
        val method = if (id.isDefined) "POST" else "PUT"
        val maId = id.getOrElse(-1)

        ajaxUpdate[MoneyAccount](
          method,
          "/" + account + "/money-accounts",
          BPJson.write(MoneyAccount(maId, name, startAmount, currency, created, -1, None)),
          ma => $.modState { oldState =>
            val currencyObj = oldState.currencies.find(_.id == ma.currencyId).get
            val period = PeriodAmountStatus(ma.startAmount, ma.startAmount)
            val enhanced = EnhancedMoneyAccount(ma.id, ma.name, ma.startAmount, currencyObj, ma.created, period, ma.owner)
            oldState.copy(moneyAccounts = oldState.moneyAccounts + (ma.id -> enhanced))
          }
        )
      }
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
      import org.big.pete.sft.domain.Implicits._

      $.props.flatMap { props =>
        val account = getAccountPermalink(props.activePage).getOrElse("")
        val method = if (id.isDefined) "POST" else "PUT"

        ajaxUpdate[Transaction](
          method,
          "/" + account + "/transactions",
          BPJson.write(Transaction(id.getOrElse(-1), date, transactionType, amount, description, category, moneyAccount,
            TransactionTracking.None, destinationAmount, destinationMoneyAccountId, None
          )),
          transaction => $.modState { oldState =>
            val newTransactions = oldState.transactions ++ List(transaction)
            oldState.copy(
              moneyAccounts = updateMoneyAccountsWithTransaction(transaction, oldState.from, oldState.to, oldState.moneyAccounts),
              transactions = newTransactions,
              displayTransactions = filterTransactions(oldState, Some(newTransactions))
            )
          }
        )
      }
    }

    def updateMoneyAccountsWithTransaction(
        transaction: Transaction,
        from: LocalDate,
        to: LocalDate,
        moneyAccounts: Map[Int, EnhancedMoneyAccount]
    ): Map[Int, EnhancedMoneyAccount] = {
      if (transaction.date.isBefore(to.asInstanceOf[ChronoLocalDate])) {
        val updater = {
          if (transaction.date.isAfter(from.asInstanceOf[ChronoLocalDate]))
            (period: PeriodAmountStatus, operation: (BigDecimal, BigDecimal) => BigDecimal, value: BigDecimal) =>
              PeriodAmountStatus(period.start, operation(period.end, value))
          else
            (period: PeriodAmountStatus, operation: (BigDecimal, BigDecimal) => BigDecimal, value: BigDecimal) =>
              PeriodAmountStatus(operation(period.start, value), operation(period.end, value))
        }

        moneyAccounts.map { case (id, ma) =>
          transaction.transactionType match {
            case TransactionType.Income if id == transaction.moneyAccount =>
              id -> ma.copy(periodStatus = updater(ma.periodStatus, _ + _, transaction.amount))
            case TransactionType.Expense if id == transaction.moneyAccount =>
              id -> ma.copy(periodStatus = updater(ma.periodStatus, _ - _, transaction.amount))
            case TransactionType.Transfer if id == transaction.moneyAccount =>
              id -> ma.copy(periodStatus = updater(ma.periodStatus, _ - _, transaction.amount))
            case TransactionType.Transfer if transaction.destinationMoneyAccountId.contains(id) =>
              id -> ma.copy(periodStatus = updater(ma.periodStatus, _ + _, transaction.destinationAmount.get))
            case _ =>
              id -> ma
          }
        }
      } else moneyAccounts
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
        saveAccount,
        saveCategory,
        saveMoneyAccount,
        saveTransaction
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
    .initialStateFromProps(p => State(p.initialFrom, p.initialTo, None, Set.empty, Set.empty, "", Set.empty, Set.empty, Set.empty, List.empty, List.empty, Map.empty, Map.empty, List.empty, List.empty, List.empty))
    .renderBackend[Backend]
    .componentDidMount(component => component.backend.onPageClick(component.props.activePage, None))
    .build
}

