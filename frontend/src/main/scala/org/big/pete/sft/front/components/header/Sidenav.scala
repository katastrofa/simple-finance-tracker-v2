package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.domain.SimpleUser
import org.big.pete.sft.front.SftMain.{WalletSelectionPage, CategoriesPage, AccountsPage, SftPages, TransactionsPage}
import org.big.pete.sft.front.utilz.getWalletPermalink

import scala.scalajs.js


object Sidenav {
  case class Props(isMenuOpen: Boolean, top: TopProps, filters: SidenavFilters.Props)
  case class TopProps(
      me: SimpleUser,
      routerCtl: RouterCtl[SftPages],
      activePage: SftPages,
      onPageChange: (SftPages, Option[SftPages]) => Callback
  )
  case class SidenavLinkProps(isActive: Boolean, url: String, linkClick: ^.onClick.Event => Callback, changePage: Callback)

  val topHeader: ScalaFn.Component[Unit, CtorType.Children] = ScalaFnComponent.justChildren { children =>
    <.div(^.cls := "navbar-fixed",
      <.nav(^.cls := "navbar",
        <.div(^.cls := "nav-wrapper center-align center", children)
      )
    )
  }

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val filters: js.UndefOr[VdomNode] = props.top.activePage match {
        case page if getWalletPermalink(page).isDefined =>
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
      val accountOpt = getWalletPermalink(props.activePage)

      def genLinkProps(toPage: SftPages): SidenavLinkProps =
        SidenavLinkProps(
          props.activePage == toPage,
          props.routerCtl.urlFor(toPage).value,
          props.routerCtl.setEH(toPage),
          props.onPageChange(toPage, Some(props.activePage))
        )

      <.ul(^.cls := "collection with-header",
        <.li(^.classSet("collection-header" -> true, "active" -> (props.activePage == WalletSelectionPage)),
          props.routerCtl.link(WalletSelectionPage)(<.h5(props.me.displayName, MaterialIcon("account_balance", Set("left"))))
        ),

        accountOpt.map { account =>
          ReactFragment(
            sidenavLink(genLinkProps(TransactionsPage(account))) {
              ReactFragment("Transactions", MaterialIcon("receipt_long", Set("left")))
            },
            sidenavLink(genLinkProps(CategoriesPage(account))) {
              ReactFragment("Categories", MaterialIcon("category", Set("left")))
            },
            sidenavLink(genLinkProps(AccountsPage(account))) {
              ReactFragment("Accounts", MaterialIcon("local_atm", Set("left")))
            },
          )
        }
      )
    }.build

  private val sidenavLink: ScalaFn.Component[SidenavLinkProps, CtorType.PropsAndChildren] =
    ScalaFnComponent.withChildren[SidenavLinkProps] { case (props, children) =>
      def changePage(evt: ^.onClick.Event): Callback =
        props.linkClick(evt) >> props.changePage

      <.li(
        ^.classSet("collection-item" -> true, "active" -> props.isActive),
        <.a(^.href := props.url, ^.onClick ==> changePage, children)
      )
    }

}
