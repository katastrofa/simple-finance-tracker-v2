package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.{BaseUrl, RouterCtl, RouterWithProps, RouterWithPropsConfig, RouterWithPropsConfigDsl, SetRouteVia}
import japgolly.scalajs.react.vdom.VdomElement
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, CategoriesPage, MoneyAccountsPage, SftPages, TransactionsPage}
import org.scalajs.dom.console

import java.time.LocalDate


object Routing {
  case class Props(initialFrom: LocalDate, initialTo: LocalDate, apiBase: String)

  class Backend() {

    def genPage(router: RouterCtl[SftPages], activePage: SftPages, props: Props): VdomElement = {
      console.log(s"genPage: ${activePage.toString}")
      SftState.component.apply(SftState.Props(
        router,
        activePage,
        props.initialFrom,
        props.initialTo,
        props.apiBase
      ))
    }

    val routerConfig: RouterWithPropsConfig[SftPages, Props] = RouterWithPropsConfigDsl[SftPages, Props].buildConfig { dsl =>
      import dsl._

      val mainRoute = staticRoute(root, AccountsSelectionPage)
      val transactionsRoute = ("#" / string("[a-zA-Z0-9_-]+") / "transactions").caseClass[TransactionsPage]
      val categoriesRoute = ("#" / string("[a-zA-Z0-9_-]+") / "categories").caseClass[CategoriesPage]
      val moneyAccountRoute = ("#" / string("[a-zA-Z0-9_-]+") / "money-accounts").caseClass[MoneyAccountsPage]

      (emptyRule
        | mainRoute ~> renderRP((rCtl, p) => genPage(rCtl, AccountsSelectionPage, p))
        | dynamicRoute(transactionsRoute) { case x: TransactionsPage => x } ~> dynRenderRP(
        (page, rCtl, p) => genPage(rCtl, page, p)
      )
        | dynamicRoute(categoriesRoute) { case x: CategoriesPage => x } ~> dynRenderRP((page, rCtl, p) => genPage(rCtl, page, p))
        | dynamicRoute(moneyAccountRoute) { case x: MoneyAccountsPage => x } ~> dynRenderRP(
        (page, rCtl, p) => genPage(rCtl, page, p)
      )
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
