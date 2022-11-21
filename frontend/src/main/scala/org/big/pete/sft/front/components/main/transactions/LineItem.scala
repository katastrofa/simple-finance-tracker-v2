package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import org.big.pete.react.{MICheckbox, MaterialIcon}
import org.big.pete.sft.domain.{TransactionTracking, TransactionType}
import org.big.pete.sft.front.components.main.{DateFormat, formatAmount}
import org.big.pete.sft.front.domain.EnhancedTransaction


object LineItem {
  import org.big.pete.sft.front.domain.Implicits._

  final val trackingToIcon = Map[TransactionTracking, String](
    TransactionTracking.None -> "horizontal_rule",
    TransactionTracking.Auto -> "blur_circular",
    TransactionTracking.Verified -> "check_circle"
  )

  case class Props(
      transaction: EnhancedTransaction,
      checkedTransactions: Set[Int],
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback,
      openEditModal: EnhancedTransaction => Callback,
      openDeleteModal: Set[Int] => Callback
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props](
    "checkTransaction", "trackingChanged", "openEditModal", "openDeleteModal"
  )

  val component: Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { props =>
    val amountClass = props.transaction.transactionType match {
      case TransactionType.Income => "green-text text-darken-1"
      case TransactionType.Expense => "red-text text-darken-1"
      case TransactionType.Transfer => "amber-text text-darken-2"
    }
    val additionalAmountInfo = if (props.transaction.destinationAmount.isDefined)
      " -> " + formatAmount(props.transaction.destinationCurrencySymbol.get, props.transaction.destinationAmount.get)
    else ""
    val additionalMoneyAccount = props.transaction.destinationMoneyAccountName.map(name => s" -> $name").getOrElse("")

    <.tr(^.cls := "",
      MICheckbox.component(
        MICheckbox.Props(
          <.td(_: _*),
          Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
          props.transaction.id.toString,
          "",
          if (props.checkedTransactions.contains(props.transaction.id)) MICheckbox.Status.checkedStatus else MICheckbox
            .Status
            .none,
          props.checkTransaction
        )
      ),
      <.td(^.cls := "date", props.transaction.date.format(DateFormat)),
      <.td(
        ^.cls := "description pointer",
        ^.onClick --> props.openEditModal(props.transaction),
        props.transaction.description
      ),
      <.td(
        ^.cls := s"right-align amount $amountClass",
        formatAmount(props.transaction.currencySymbol, props.transaction.amount) + additionalAmountInfo
      ),
      <.td(
        ^.cls := "category hide-on-small-only",
        <.span(^.cls := "show-on-large hide-on-med-and-down", props.transaction.categoryFullName),
        <.span(^.cls := "show-on-medium hide-on-large-only", props.transaction.categoryName)
      ),
      <.td(^.cls := "money-account hide-on-med-and-down", props.transaction.moneyAccountName + additionalMoneyAccount),
      <.td(
        ^.cls := "delete hide-on-med-and-down",
        MaterialIcon.Icon(
          MaterialIcon.Props(
            MaterialIcon.i,
            MaterialIcon.small,
            "delete",
            props.openDeleteModal(Set(props.transaction.id)),
            Set("pointer")
          )
        )
      ),
      <.td(
        ^.cls := "status center-align",
        MaterialIcon.Icon(
          MaterialIcon.Props(
            MaterialIcon.`i`,
            MaterialIcon.`small`,
            trackingToIcon(props.transaction.tracking),
            props.trackingChanged(props.transaction.id, props.transaction.tracking),
            Set("pointer")
          )
        )
      )
    )
  }
}
