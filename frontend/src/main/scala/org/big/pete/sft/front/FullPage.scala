package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.util.EffectSyntax
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.{Sidenav, SidenavFilters, TopHeader}
import org.big.pete.sft.front.components.main.moneyaccount.Page
import org.big.pete.sft.front.components.main.transactions.Page
import org.big.pete.sft.front.components.main.{Accounts, Categories}


object FullPage extends EffectSyntax {
  case class Props(
      activePage: SftPages,
      isMenuOpen: Boolean,

      topHeader: TopHeader.Props,
      sidenavTop: Sidenav.TopProps,
      sidenavFilters: SidenavFilters.Props,

      accounts: Accounts.Props,
      transactions: Page.Props,
      categories: Categories.Props,
      moneyAccounts: Page.Props
  )

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val mainPage = props.activePage match {
        case SftMain.AccountsSelectionPage =>
          Accounts.component(props.accounts)
        case SftMain.TransactionsPage(_) =>
          Page.component(props.transactions)
        case SftMain.CategoriesPage(_) =>
          Categories.component(props.categories)
        case SftMain.MoneyAccountsPage(_) =>
          Page.component(props.moneyAccounts)
      }

      ReactFragment(
        <.header(
          TopHeader.component(props.topHeader),
          Sidenav.component(Sidenav.Props(props.isMenuOpen, props.sidenavTop, props.sidenavFilters))
        ),
        mainPage
      )
    }.build
}
