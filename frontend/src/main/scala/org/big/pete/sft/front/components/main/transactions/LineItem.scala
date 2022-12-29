package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import org.big.pete.react.{MICheckbox, MaterialIcon}
import org.big.pete.sft.domain.{TransactionTracking, TransactionType}
import org.big.pete.sft.front.components.main.{DateFormat, formatAmount}
import org.big.pete.sft.front.domain.EnhancedTransaction
import org.big.pete.sft.front.helpers.NiceButton


object LineItem {
  import org.big.pete.sft.front.domain.Implicits._

  final val trackingToIcon = Map[TransactionTracking, String](
    TransactionTracking.None -> "horizontal_rule",
    TransactionTracking.Auto -> "blur_circular",
    TransactionTracking.Verified -> "check_circle"
  )

  case class Props(
      transaction: EnhancedTransaction,
      isChecked: Boolean,
      isDetailsVisible: Boolean,
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback,
      openEditModal: EnhancedTransaction => Callback,
      openDeleteModal: Set[Int] => Callback,
      toggleDetails: Int => Callback,
      colSpan: Int
  )

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props](
    "checkTransaction", "trackingChanged", "openEditModal", "openDeleteModal", "toggleDetails", "colSpan"
  )

  val component: Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { props =>
    val amountClass = props.transaction.transactionType match {
      case TransactionType.Income => "green-text text-darken-1"
      case TransactionType.Expense => "red-text text-darken-1"
      case TransactionType.Transfer => "amber-text text-darken-2"
    }
    val additionalAmountInfo = if (props.transaction.destinationAmount.isDefined)
      " -> " + formatAmount(props.transaction.destinationCurrency.get.symbol, props.transaction.destinationAmount.get)
    else ""
    val additionalMoneyAccount = props.transaction.destinationMoneyAccountName.map(name => s" -> $name").getOrElse("")

    val mainTr = <.tr(^.cls := "show-hoverable", ^.key := s"line-${props.transaction.id}",
      MICheckbox.component(MICheckbox.Props(
        <.td(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        props.transaction.id.toString,
        "",
        if (props.isChecked) MICheckbox.Status.checkedStatus else MICheckbox.Status.none,
        props.checkTransaction
      )),
      <.td(^.cls := "date", props.transaction.date.format(DateFormat)),
      <.td(
        ^.cls := "description",
        ^.onClick --> props.toggleDetails(props.transaction.id),
        MaterialIcon.Icon(MaterialIcon.Props(
          MaterialIcon.i, MaterialIcon.small, "edit", props.openEditModal(props.transaction),
          Set("show-on-hover pointer pad-right hide-on-med-and-down"), stopPropagation = true
        )),
        props.transaction.description
      ),
      <.td(
        ^.cls := s"right-align amount $amountClass",
        formatAmount(props.transaction.currency.symbol, props.transaction.amount) + additionalAmountInfo
      ),
      <.td(
        ^.cls := "category",
        <.span(^.cls := "category-big", props.transaction.categoryFullName),
        <.span(^.cls := "category-small", props.transaction.categoryName)
      ),
      <.td(^.cls := "money-account hide-on-med-and-down", props.transaction.moneyAccountName + additionalMoneyAccount),
      <.td(
        ^.cls := "delete hide-on-med-and-down",
        MaterialIcon.Icon(MaterialIcon.Props(
          MaterialIcon.i,
          MaterialIcon.small,
          "delete",
          props.openDeleteModal(Set(props.transaction.id)),
          Set("pointer")
        ))
      ),
      <.td(
        ^.cls := "status center-align",
        MaterialIcon.Icon(MaterialIcon.Props(
          MaterialIcon.`i`,
          MaterialIcon.`small`,
          trackingToIcon(props.transaction.tracking),
          props.trackingChanged(props.transaction.id, props.transaction.tracking),
          Set("pointer")
        ))
      )
    )

    val detailsTr = if (!props.isDetailsVisible) EmptyVdom else <.tr(^.cls := "details", ^.key := s"line-details-${props.transaction.id}",
      <.td(^.cls := "details-col", ^.colSpan := props.colSpan,
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Date:"),
          <.span(^.cls := "col l10 s9", props.transaction.date.format(DateFormat))
        ),
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Description:"),
          <.span(^.cls := "col l10 s9", props.transaction.description)
        ),
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Type: "),
          <.span(^.cls := "col l10 s9", props.transaction.transactionType.toString)
        ),
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Amount: "),
          <.span(^.cls := s"col l10 s9 $amountClass", formatAmount(props.transaction.currency.symbol, props.transaction.amount) + additionalAmountInfo)
        ),
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Category: "),
          <.span(^.cls := "col l10 s9", props.transaction.categoryFullName)
        ),
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Account: "),
          <.span(^.cls := "col l10 s9", props.transaction.moneyAccountName + additionalMoneyAccount)
        ),
        <.div(^.cls := "details-item row",
          <.strong(^.cls := "col l2 s3", "Status: "),
          <.span(^.cls := "col l10 s9", props.transaction.tracking.toString)
        ),
        <.div(^.cls := "details-item row pad-top",
          <.div(^.cls := "col l2 s3",
            NiceButton(42, props.openEditModal(props.transaction))(TagMod(MaterialIcon("edit"), "Edit"))
          ),
          <.div(^.cls := "col l2 s3 offset-s6 offset-l8 right-align",
            NiceButton(43, props.openDeleteModal(Set(props.transaction.id)))(TagMod(MaterialIcon("delete"), "Delete"))
          )
        )
      )
    )

    List(mainTr, detailsTr).toVdomArray
  }
}
