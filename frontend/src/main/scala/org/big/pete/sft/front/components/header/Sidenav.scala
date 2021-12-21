package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, CategoriesPage, MoneyAccountsPage, SftPages, TransactionsPage}
import org.big.pete.sft.front.utilz.getAccountPermalink

import scala.scalajs.js


object Sidenav {
  case class Props(top: TopProps, filters: SidenavFilters.Props)
  case class TopProps(routerCtl: RouterCtl[SftPages], activePage: SftPages)

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val filters: js.UndefOr[VdomNode] = props.top.activePage match {
        case TransactionsPage(_) =>
          <.li(^.id := "filters", SidenavFilters.component(props.filters))
        case _ =>
          js.undefined
      }

      <.ul(^.id := "sidenav-left", ^.cls := "sidenav sidenav-fixed",
        <.li(^.id := "top-navigation", sidenavTopComponent(props.top)),
        <.li(<.h6("Filters", MaterialIcon("filter_list", Set("left")))),
        filters
      )
    }.build

  val sidenavTopComponent: Component[TopProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[TopProps]
    .stateless
    .render_P { props =>
      val accountOpt = getAccountPermalink(props.activePage)

      <.ul(^.cls := "collection with-header",
        <.li(^.classSet("collection-header" -> true, "active" -> (props.activePage == AccountsSelectionPage)),
          props.routerCtl.link(AccountsSelectionPage)(<.h5("Peter Baran", MaterialIcon("account_balance", Set("left"))))
        ),

        accountOpt.map { account =>
          VdomArray.apply(
            <.li(^.key := "top-n-transactions",
              ^.classSet("collection-item" -> true, "active" -> (props.activePage == TransactionsPage(account))),
              props.routerCtl.link(TransactionsPage(account))("Transactions", MaterialIcon("receipt_long", Set("left")))
            ),
            <.li(^.key := "top-n-categories",
              ^.classSet("collection-item" -> true, "active" -> (props.activePage == CategoriesPage(account))),
              props.routerCtl.link(CategoriesPage(account))("Categories", MaterialIcon("category", Set("left")))
            ),
            <.li(^.key := "top-n-money-accounts",
              ^.classSet("collection-item" -> true, "active" -> (props.activePage == MoneyAccountsPage(account))),
              props.routerCtl.link(MoneyAccountsPage(account))("Money Accounts", MaterialIcon("local_atm", Set("left")))
            )
          )
        }
      )
    }.build

}
