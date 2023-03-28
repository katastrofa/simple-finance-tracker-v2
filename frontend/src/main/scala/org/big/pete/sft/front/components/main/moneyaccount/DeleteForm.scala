package org.big.pete.sft.front.components.main.moneyaccount

import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import japgolly.scalajs.react.component.ScalaFn.{Component, Unmounted}
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.DropDown
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount}
import org.big.pete.sft.front.components.main.displayCurrency
import org.big.pete.sft.front.helpers.ModalButtons

import java.time.LocalDate


object DeleteForm {
  case class DeleteMoneyAccountProps(
      accounts: List[EnhancedMoneyAccount],
      toDelete: Option[EnhancedMoneyAccount],
      shiftTransactionsTo: Map[String, StateSnapshot[Option[EnhancedMoneyAccount]]],
      deleteMoneyAccount: Callback,
      closeDeleteModal: Callback
  )
  case class SingleShiftTransactionsProps(
      availableAccounts: List[EnhancedMoneyAccount],
      shift: StateSnapshot[Option[EnhancedMoneyAccount]],
      currency: Currency,
      tabIndex: Int
  )

  final val NoShiftMoneyAccount =
    EnhancedMoneyAccount(-42, "Do not shift - delete", LocalDate.now(), List.empty, List.empty, None)


  implicit val stringMoneyAccountMap: Reusability[Map[String, StateSnapshot[Option[EnhancedMoneyAccount]]]] =
    Reusability.map[String, StateSnapshot[Option[EnhancedMoneyAccount]]]
  implicit val deleteMoneyAccountPropsReuse: Reusability[DeleteMoneyAccountProps] =
    Reusability.caseClassExcept[DeleteMoneyAccountProps]("deleteMoneyAccount", "closeDeleteModal")
  implicit val singleShiftTransactionsPropsReuse: Reusability[SingleShiftTransactionsProps] =
    Reusability.caseClassExcept[SingleShiftTransactionsProps]("changeShiftTransactions")


  private val deleteMoneyAccountForm: Component[DeleteMoneyAccountProps, CtorType.Props] = ScalaFnComponent.withReuse[DeleteMoneyAccountProps]
    { props =>
      <.form(
        props.toDelete.get.currencies.map { maCurrency =>
          val available = NoShiftMoneyAccount :: props.accounts
            .filter(_.currencies.exists(_.currency.id == maCurrency.currency.id))

          singleShiftTransactions.withKey(s"com-delete-ma-shift-${maCurrency.currency.id}") {
            SingleShiftTransactionsProps(available, props.shiftTransactionsTo(maCurrency.currency.id), maCurrency.currency, 320 + maCurrency.id)
          }
        }.toVdomArray,
        ModalButtons("Delete", 351, props.deleteMoneyAccount, props.closeDeleteModal)
      )
    }

  private val singleShiftTransactions = ScalaFnComponent.withReuse[SingleShiftTransactionsProps] { props =>
    <.div(^.cls := "row",
      DropDown(
        s"delete-ma-shift-${props.currency.id}",
        "Shift Transactions for " + displayCurrency(props.currency),
        props.availableAccounts,
        props.shift,
        props.tabIndex,
        List("col", "s12")
      )
    )
  }

  def apply(
      accounts: List[EnhancedMoneyAccount],
      toDelete: Option[EnhancedMoneyAccount],
      shiftTransactionsTo: Map[String, StateSnapshot[Option[EnhancedMoneyAccount]]],
      deleteMoneyAccount: Callback,
      closeDeleteModal: Callback
  ): Unmounted[DeleteMoneyAccountProps] =
    deleteMoneyAccountForm(DeleteMoneyAccountProps(accounts, toDelete, shiftTransactionsTo, deleteMoneyAccount, closeDeleteModal))
}
