package org.big.pete.sft.front

import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.extra.router.{BaseUrl, RouterConfigDsl, RouterCtl, RouterWithProps, RouterWithPropsConfig, SetRouteVia}
import org.scalajs.dom.document

import java.time.LocalDate
import scala.scalajs.js.annotation.JSExport


object SftMain {
  sealed trait SftPages
  case object AccountsSelectionPage extends SftPages
  case class TransactionsPage(account: String) extends SftPages
  case class CategoriesPage(account: String) extends SftPages
  case class MoneyAccountsPage(account: String) extends SftPages

  val from: LocalDate = LocalDate.now().withDayOfMonth(1)
  val to: LocalDate = LocalDate.now().plusMonths(1L).withDayOfMonth(1).minusDays(1L)

  def generateFullPage(active: SftPages)(rCtl: RouterCtl[SftPages]): Unmounted[FullPage.Props, FullPage.State, FullPage.Backend] =
    FullPage.component(FullPage.Props(from, to, rCtl, active))

  val routerConfig: RouterWithPropsConfig[SftPages, Unit] = RouterConfigDsl[SftPages].buildConfig { dsl =>
    import dsl._

    val mainRoute = staticRoute(root, AccountsSelectionPage)
    val transactionsRoute = ("#" / string("[a-zA-Z0-9_-]+") / "transactions").caseClass[TransactionsPage]
    val categoriesRoute = ("#" / string("[a-zA-Z0-9_-]+") / "categories").caseClass[CategoriesPage]
    val moneyAccountRoute = ("#" / string("[a-zA-Z0-9_-]+") / "money-accounts").caseClass[MoneyAccountsPage]

    (emptyRule
      | mainRoute ~> renderR(generateFullPage(AccountsSelectionPage))
      | dynamicRoute(transactionsRoute) { case x: TransactionsPage => x } ~> dynRenderR { case (tp, rCtl) => generateFullPage(tp)(rCtl) }
      | dynamicRoute(categoriesRoute) { case x: CategoriesPage => x } ~> dynRenderR { case (tp, rCtl) => generateFullPage(tp)(rCtl) }
      | dynamicRoute(moneyAccountRoute) { case x: MoneyAccountsPage => x } ~> dynRenderR { case (tp, rCtl) => generateFullPage(tp)(rCtl) }
    ).notFound(redirectToPage(AccountsSelectionPage)(SetRouteVia.HistoryReplace))
  }

  @JSExport
  def main(args: Array[String]): Unit = {
    val baseUrl = BaseUrl.fromWindowOrigin + "/"
    val routerComponent = RouterWithProps(baseUrl, routerConfig)
    routerComponent.apply(()).renderIntoDOM(document.getElementById("sft-full"))
    ()
  }
}
