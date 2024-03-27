package org.big.pete.sft.front

import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.util.EffectSyntax
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.front.SftMain.SftPages
import org.big.pete.sft.front.components.header.{Sidenav, SidenavFilters}
import org.big.pete.sft.front.components.main.account
import org.big.pete.sft.front.components.main.transactions.Page
import org.big.pete.sft.front.components.main.{Wallets, Categories}

import java.time.LocalDate


object FullPage extends EffectSyntax {
  case class Props(
      activePage: SftPages,
      isMenuOpen: StateSnapshot[Boolean],

      from: LocalDate,
      to: LocalDate,
      onFromDateChange: LocalDate => CallbackTo[LocalDate],
      onToDateChange: LocalDate => CallbackTo[LocalDate],

      sidenavTop: Sidenav.TopProps,
      sidenavFilters: SidenavFilters.Props,

      accounts: Wallets.Props,
      transactions: Page.Props,
      categories: Categories.Props,
      moneyAccounts: account.Page.Props
  )

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val mainPage = props.activePage match {
        case SftMain.WalletSelectionPage =>
          Wallets.component(props.accounts)
        case SftMain.TransactionsPage(_) =>
          Page.component(props.transactions)
        case SftMain.CategoriesPage(_) =>
          Categories.component(props.categories)
        case SftMain.AccountsPage(_) =>
          account.Page.component(props.moneyAccounts)
      }

      ReactFragment(
        <.header(
          Sidenav.topHeader.apply {
            ReactFragment(
              /// Menu Icon
              MaterialIcon.Icon.withKey("key-menu-icon").apply(
                MaterialIcon.Props(
                  MaterialIcon.span,
                  MaterialIcon.medium,
                  "menu",
                  props.isMenuOpen.modState(s => !s),
                  Set("right hide-on-large-only padme")
                )
              ),

              /// Date range pickers
              ReactDatePicker.DatePicker.withKey("key-date-select-from").apply(
                ReactDatePicker.Props(
                  "date-select-from",
                  "date-select date-select-from",
                  props.onFromDateChange,
                  props.from,
                  isOpened = false,
                  Some(42),
                  ReactDatePicker.ExtendedKeyBindings
                )
              ),
              ReactDatePicker.DatePicker.withKey("key-date-select-to").apply(
                ReactDatePicker.Props(
                  "date-select-to",
                  "date-select date-select-to",
                  props.onToDateChange,
                  props.to,
                  isOpened = false,
                  Some(43),
                  ReactDatePicker.ExtendedKeyBindings
                )
              )
            )
          },
          Sidenav.component(Sidenav.Props(props.isMenuOpen.value, props.sidenavTop, props.sidenavFilters))
        ),
        mainPage
      )
    }.build
}
