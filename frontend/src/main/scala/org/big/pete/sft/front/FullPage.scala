package org.big.pete.sft.front

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.util.EffectSyntax
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Account, Currency, EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.SidenavFilters.FiltersOpen
import org.big.pete.sft.front.components.header.{Sidenav, SidenavFilters, TopHeader}
import org.big.pete.sft.front.components.main.{Accounts, Categories, MoneyAccounts, Transactions}
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction}
import org.scalajs.dom.console

import java.time.LocalDate


object FullPage extends EffectSyntax {
  case class Props(
      router: RouterCtl[SftPages],
      from: LocalDate,
      to: LocalDate,
      activePage: SftPages,

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

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      console.log(s"FullPage: ${props.activePage}")
      val mainPage = props.activePage match {
        case SftMain.AccountsSelectionPage =>
          Accounts.component.apply(Accounts.Props(
            props.accounts,
            Accounts.AccountProps(props.router, props.activePage, props.onPageChange),
            props.publishAccount
          ))
        case SftMain.TransactionsPage(_) =>
          Transactions.component.apply(Transactions.Props(
            props.displayTransactions,
            CategoryTree.makeLinearCats(props.categoryTree),
            props.moneyAccounts,
            props.checkTransaction,
            props.transactionTrackingClick,
            props.publishTransaction
          ))
        case SftMain.CategoriesPage(_) =>
          Categories.component.apply(Categories.Props(props.categoryTree, props.publishCategory))
        case SftMain.MoneyAccountsPage(_) =>
          MoneyAccounts.component.apply(MoneyAccounts.Props(
            props.moneyAccounts.values.toList,
            props.currencies,
            props.publishMoneyAccount
          ))
      }

      ReactFragment(
        <.header(
          /// TODO: save date change to cookie
          TopHeader.component(TopHeader.Props(Some(props.from), Some(props.to), props.setFromDate, props.setToDate)),
          Sidenav.component(Sidenav.Props(
            Sidenav.TopProps(props.router, props.activePage, props.onPageChange),
            SidenavFilters.Props(
              props.activeFilter,
              props.setActiveFilter,
              SidenavFilters.TransactionsProps(
                props.transactionTypeActiveFilters,
                props.setTtFilter,
                props.trackingActiveFilters,
                props.setTrackingFilter,
                props.contentFilter,
                props.setContentFilter
              ),
              SidenavFilters.CategoriesProps(
                props.categoriesActiveFilters,
                props.setCategoriesFilter,
                props.categoryTree
              ),
              SidenavFilters.MoneyAccountProps(
                props.moneyAccountsActiveFilters,
                props.setMoneyAccountsFilter,
                props.moneyAccounts.values.toList
              )
            )
          ))
        ),
        mainPage
      )
    }.build
}
