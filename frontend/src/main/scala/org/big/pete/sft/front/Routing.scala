package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.{BaseUrl, RouterCtl, RouterWithProps, RouterWithPropsConfig, RouterWithPropsConfigDsl, SetRouteVia}
import japgolly.scalajs.react.vdom.VdomElement
import org.big.pete.sft.front.SftMain.{WalletSelectionPage, CategoriesPage, AccountsPage, SftPages, TransactionsPage}


object Routing {
  case class Props(apiBase: String)

  class Backend() {

    def genPage(router: RouterCtl[SftPages], activePage: SftPages, props: Props): VdomElement = {
      SftState.component.apply(state.Props(
        router,
        activePage,
        props.apiBase
      ))
    }

    val routerConfig: RouterWithPropsConfig[SftPages, Props] = RouterWithPropsConfigDsl[SftPages, Props].buildConfig { dsl =>
      import dsl._

      val mainRoute = staticRoute(root, WalletSelectionPage)
      val transactionsRoute = ("#" / string("[a-zA-Z0-9_-]+") / "transactions").caseClass[TransactionsPage]
      val categoriesRoute = ("#" / string("[a-zA-Z0-9_-]+") / "categories").caseClass[CategoriesPage]
      val moneyAccountRoute = ("#" / string("[a-zA-Z0-9_-]+") / "money-accounts").caseClass[AccountsPage]

      (emptyRule
        | mainRoute ~> renderRP((rCtl, p) => genPage(rCtl, WalletSelectionPage, p))
        | dynamicRoute(transactionsRoute) { case x: TransactionsPage => x } ~> dynRenderRP(
        (page, rCtl, p) => genPage(rCtl, page, p)
      )
        | dynamicRoute(categoriesRoute) { case x: CategoriesPage => x } ~> dynRenderRP((page, rCtl, p) => genPage(rCtl, page, p))
        | dynamicRoute(moneyAccountRoute) { case x: AccountsPage => x } ~> dynRenderRP(
        (page, rCtl, p) => genPage(rCtl, page, p)
      )
        ).notFound(redirectToPage(WalletSelectionPage)(SetRouteVia.HistoryReplace))
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
