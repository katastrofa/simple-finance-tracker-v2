package org.big.pete.sft.front

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.util.EffectSyntax
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.{Sidenav, SidenavFilters, TopHeader}
import org.big.pete.sft.front.components.main.{Accounts, Categories, MoneyAccounts, Transactions}


object FullPage extends EffectSyntax {
  case class Props(router: RouterCtl[SftPages], activePage: SftPages, p: Routing.Props)

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val mainPage = props.activePage match {
        case SftMain.AccountsSelectionPage =>
          Accounts.component.apply(Accounts.Props(
            props.p.accounts,
            Accounts.AccountProps(props.router, props.activePage, props.p.onPageChange),
            props.p.publishAccount
          ))
        case SftMain.TransactionsPage(_) =>
          Transactions.component.apply(Transactions.Props(props.p.displayTransactions, props.p.checkTransaction, props.p.transactionTrackingClick))
        case SftMain.CategoriesPage(_) =>
          Categories.component.apply(Categories.Props(props.p.categoryTree))
        case SftMain.MoneyAccountsPage(_) =>
          MoneyAccounts.component.apply(MoneyAccounts.Props(props.p.moneyAccounts.values.toList))
      }

      ReactFragment(
        <.header(
          /// TODO: save date change to cookie
          TopHeader.component(TopHeader.Props(None, None, props.p.setFromDate, props.p.setToDate)),
          Sidenav.component(Sidenav.Props(
            Sidenav.TopProps(props.router, props.activePage, props.p.onPageChange),
            SidenavFilters.Props(
              props.p.activeFilter,
              props.p.setActiveFilter,
              SidenavFilters.TransactionsProps(
                props.p.transactionTypeActiveFilters,
                props.p.setTtFilter,
                props.p.trackingActiveFilters,
                props.p.setTrackingFilter,
                props.p.contentFilter,
                props.p.setContentFilter
              ),
              SidenavFilters.CategoriesProps(
                props.p.categoriesActiveFilters,
                props.p.setCategoriesFilter,
                props.p.categoryTree
              ),
              SidenavFilters.MoneyAccountProps(
                props.p.moneyAccountsActiveFilters,
                props.p.setMoneyAccountsFilter,
                props.p.moneyAccounts.values.toList
              )
            )
          ))
        ),
        mainPage
      )
    }.build
}
