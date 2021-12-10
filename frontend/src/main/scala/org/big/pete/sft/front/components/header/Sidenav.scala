package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, CategoriesPage, MoneyAccountsPage, SftPages, TransactionsPage}


object Sidenav {
  case class Props(top: SidenavTopProps)
  case class SidenavTopProps(routerCtl: RouterCtl[SftPages], activePage: SftPages, account: Option[String])

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      <.ul(^.id := "sidenav-left", ^.cls := "sidenav sidenav-fixed",
        <.li(^.id := "top-navigation", sidenavTopComponent(props.top)),
        <.li(<.h6("Filters", MaterialIcon("filter_list", Set("left")))),
        <.li(^.id := "filters")
      )
    }.build

  val sidenavTopComponent: Component[SidenavTopProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[SidenavTopProps]
    .stateless
    .render_P { props =>
      <.ul(^.cls := "collection with-header",
        <.li(^.classSet("collection-header" -> true, "active" -> (props.activePage == AccountsSelectionPage)),
          props.routerCtl.link(AccountsSelectionPage)(<.h5("Peter Baran", MaterialIcon("account_balance", Set("left"))))
        ),

        props.account.map { account =>
          VdomArray.apply(
            <.li(^.key := "top-n-transactions",
              ^.classSet("collection-item" -> true, "active" -> (props.account.isDefined && props.activePage == TransactionsPage(props.account.get))),
              props.routerCtl.link(TransactionsPage(account))("Transactions", MaterialIcon("receipt_long", Set("left")))
            ),
            <.li(^.key := "top-n-categories",
              ^.classSet("collection-item" -> true, "active" -> (props.account.isDefined && props.activePage == CategoriesPage(props.account.get))),
              props.routerCtl.link(CategoriesPage(account))("Categories", MaterialIcon("category", Set("left")))
            ),
            <.li(^.key := "top-n-money-accounts",
              ^.classSet("collection-item" -> true, "active" -> (props.account.isDefined && props.activePage == MoneyAccountsPage(props.account.get))),
              props.routerCtl.link(MoneyAccountsPage(account))("Money Accounts", MaterialIcon("local_atm", Set("left")))
            )
          )
        }
      )
    }.build


}
