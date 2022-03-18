package org.big.pete.sft.front

import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ScalaComponent}
import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.{BaseUrl, RouterCtl, RouterWithProps, RouterWithPropsConfig, RouterWithPropsConfigDsl, SetRouteVia}
import japgolly.scalajs.react.vdom.VdomElement
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Account, Currency, EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, CategoriesPage, MoneyAccountsPage, SftPages, TransactionsPage}
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction}

import java.time.LocalDate

object Routing {
  case class Props(
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
      currencies: List[Currency],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],

      categoryTree: List[CategoryTree],
      displayTransactions: List[EnhancedTransaction],

      setFromDate: LocalDate => CallbackTo[LocalDate],
      setToDate: LocalDate => CallbackTo[LocalDate],
      setActiveFilter: FiltersOpen => Callback,
      setTtFilter: (MICheckbox.Status, String) => Callback,
      setTrackingFilter: (MICheckbox.Status, String) => Callback,
      setContentFilter: ReactFormEventFromInput => Callback,
      setCategoriesFilter: (MICheckbox.Status, String) => Callback,
      setMoneyAccountsFilter: (MICheckbox.Status, String) => Callback,
      checkTransaction: (MICheckbox.Status, String) => Callback,
      transactionTrackingClick: (Int, TransactionTracking) => Callback,
      onPageChange: (SftPages, Option[SftPages]) => Callback,

      publishAccount: (String, String) => Callback,
      publishCategory: (String, String, Option[Int]) => Callback,
      publishMoneyAccount: (String, BigDecimal, String, LocalDate) => Callback,
      publishTransaction: (LocalDate, TransactionType, BigDecimal, String, Int, Int, Option[BigDecimal], Option[Int]) => Callback
  )

  class Backend() {

    def genPage(router: RouterCtl[SftPages], activePage: SftPages, props: Props): VdomElement = {
      FullPage.component.apply(FullPage.Props(router, activePage, props))
    }

    val routerConfig: RouterWithPropsConfig[SftPages, Props] = RouterWithPropsConfigDsl[SftPages, Props].buildConfig { dsl =>
      import dsl._

      val mainRoute = staticRoute(root, AccountsSelectionPage)
      val transactionsRoute = ("#" / string("[a-zA-Z0-9_-]+") / "transactions").caseClass[TransactionsPage]
      val categoriesRoute = ("#" / string("[a-zA-Z0-9_-]+") / "categories").caseClass[CategoriesPage]
      val moneyAccountRoute = ("#" / string("[a-zA-Z0-9_-]+") / "money-accounts").caseClass[MoneyAccountsPage]

      (emptyRule
        | mainRoute ~> renderRP((rCtl, p) => genPage(rCtl, AccountsSelectionPage, p))
        | dynamicRoute(transactionsRoute) { case x: TransactionsPage => x } ~> dynRenderRP((page, rCtl, p) => genPage(rCtl, page, p))
        | dynamicRoute(categoriesRoute) { case x: CategoriesPage => x } ~> dynRenderRP((page, rCtl, p) => genPage(rCtl, page, p))
        | dynamicRoute(moneyAccountRoute) { case x: MoneyAccountsPage => x } ~> dynRenderRP((page, rCtl, p) => genPage(rCtl, page, p))
        ).notFound(redirectToPage(AccountsSelectionPage)(SetRouteVia.HistoryReplace))
    }

    def render(props: Props): VdomElement = {
      val baseUrl = BaseUrl.fromWindowOrigin + "/"
      val routerComponent = RouterWithProps(baseUrl, routerConfig)
      routerComponent.apply(props)
    }
  }

  val component: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .build
}
