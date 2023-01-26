package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.domain.SimpleUser
import org.big.pete.sft.front.SftMain.{AccountsSelectionPage, CategoriesPage, MoneyAccountsPage, SftPages, TransactionsPage}
import org.big.pete.sft.front.utilz.getAccountPermalink

import scala.scalajs.js


object Sidenav {
  case class Props(isMenuOpen: Boolean, top: TopProps, filters: SidenavFilters.Props)
  case class TopProps(
      me: SimpleUser,
      routerCtl: RouterCtl[SftPages],
      activePage: SftPages,
      onPageChange: (SftPages, Option[SftPages]) => Callback
  )

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val filters: js.UndefOr[VdomNode] = props.top.activePage match {
        case page if getAccountPermalink(page).isDefined =>
          <.li(^.id := "filters", SidenavFilters.component(props.filters))
        case _ =>
          js.undefined
      }

      <.ul(^.id := "sidenav-left", ^.classSet("sidenav" -> true, "sidenav-fixed" -> true, "open" -> props.isMenuOpen),
        <.li(^.id := "top-navigation", sidenavTopComponent(props.top)),
        <.li(<.h6("Filters", MaterialIcon("filter_list", Set("left")))),
        filters
      )
    }.build

  private val sidenavTopComponent: Component[TopProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[TopProps]
    .stateless
    .render_P { props =>
      val accountOpt = getAccountPermalink(props.activePage)

      <.ul(^.cls := "collection with-header",
        <.li(^.classSet("collection-header" -> true, "active" -> (props.activePage == AccountsSelectionPage)),
          props.routerCtl.link(AccountsSelectionPage)(<.h5(props.me.displayName, MaterialIcon("account_balance", Set("left"))))
        ),

        accountOpt.map { account =>
          VdomArray.apply(
            <.li(^.key := "top-n-transactions",
              ^.classSet("collection-item" -> true, "active" -> (props.activePage == TransactionsPage(account))),
              <.a(
                ^.href := props.routerCtl.urlFor(TransactionsPage(account)).value,
                ^.onClick ==> (e => props.routerCtl.setEH(TransactionsPage(account))(e) >> props.onPageChange(TransactionsPage(account), Some(props.activePage))),
                "Transactions",
                MaterialIcon("receipt_long", Set("left"))
              )
            ),
            <.li(^.key := "top-n-categories",
              ^.classSet("collection-item" -> true, "active" -> (props.activePage == CategoriesPage(account))),
              <.a(
                ^.href := props.routerCtl.urlFor(CategoriesPage(account)).value,
                ^.onClick ==> (e => props.routerCtl.setEH(CategoriesPage(account))(e) >> props.onPageChange(CategoriesPage(account), Some(props.activePage))),
                "Categories",
                MaterialIcon("category", Set("left"))
              )
            ),
            <.li(^.key := "top-n-money-accounts",
              ^.classSet("collection-item" -> true, "active" -> (props.activePage == MoneyAccountsPage(account))),
              <.a(
                ^.href := props.routerCtl.urlFor(MoneyAccountsPage(account)).value,
                ^.onClick ==> (e => props.routerCtl.setEH(MoneyAccountsPage(account))(e) >> props.onPageChange(MoneyAccountsPage(account), Some(props.activePage))),
                "Money Accounts",
                MaterialIcon("local_atm", Set("left"))
              )
            )
          )
        }
      )
    }.build

}
