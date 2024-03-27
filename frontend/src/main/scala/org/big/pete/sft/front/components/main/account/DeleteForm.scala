package org.big.pete.sft.front.components.main.account

import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import japgolly.scalajs.react.component.ScalaFn.{Component, Unmounted}
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.DropDown
import org.big.pete.sft.domain.{Currency, EnhancedAccount}
import org.big.pete.sft.front.components.main.displayCurrency
import org.big.pete.sft.front.helpers.ModalButtons

import java.time.LocalDate


object DeleteForm {
  case class DeleteAccountProps(
      accounts: List[EnhancedAccount],
      toDelete: Option[EnhancedAccount],
      shiftTransactionsTo: Map[String, StateSnapshot[Option[EnhancedAccount]]],
      deleteAccount: Callback,
      closeModal: Callback
  )
  case class SingleShiftTransactionsProps(
      availableAccounts: List[EnhancedAccount],
      shift: StateSnapshot[Option[EnhancedAccount]],
      currency: Currency,
      tabIndex: Int
  )

  final val NoShiftAccount =
    EnhancedAccount(-42, "Do not shift - delete", LocalDate.now(), List.empty, List.empty, None)


  implicit val stringAccountMap: Reusability[Map[String, StateSnapshot[Option[EnhancedAccount]]]] =
    Reusability.map[String, StateSnapshot[Option[EnhancedAccount]]]
  implicit val deleteAccountPropsReuse: Reusability[DeleteAccountProps] =
    Reusability.caseClassExcept[DeleteAccountProps]("deleteAccount", "closeModal")
  implicit val singleShiftTransactionsPropsReuse: Reusability[SingleShiftTransactionsProps] =
    Reusability.derive[SingleShiftTransactionsProps]


  private val deleteAccountForm: Component[DeleteAccountProps, CtorType.Props] = ScalaFnComponent.withReuse[DeleteAccountProps]
    { props =>
      <.form(
        props.toDelete.get.currencies.map { currency =>
          val available = NoShiftAccount :: props.accounts
            .filter(_.currencies.exists(_.currency.id == currency.currency.id))

          singleShiftTransactions.withKey(s"com-delete-account-shift-${currency.currency.id}") {
            SingleShiftTransactionsProps(available, props.shiftTransactionsTo(currency.currency.id), currency.currency, 320 + currency.id)
          }
        }.toVdomArray,
        ModalButtons("Delete", 351, props.deleteAccount, props.closeModal)
      )
    }

  private val singleShiftTransactions = ScalaFnComponent.withReuse[SingleShiftTransactionsProps] { props =>
    <.div(^.cls := "row",
      DropDown(
        s"delete-account-shift-${props.currency.id}",
        "Shift Transactions for " + displayCurrency(props.currency),
        props.availableAccounts,
        props.shift,
        props.tabIndex,
        List("col", "s12")
      )
    )
  }

  def apply(
      accounts: List[EnhancedAccount],
      toDelete: Option[EnhancedAccount],
      shiftTransactionsTo: Map[String, StateSnapshot[Option[EnhancedAccount]]],
      deleteAccount: Callback,
      closeModal: Callback
  ): Unmounted[DeleteAccountProps] =
    deleteAccountForm(DeleteAccountProps(accounts, toDelete, shiftTransactionsTo, deleteAccount, closeModal))
}
