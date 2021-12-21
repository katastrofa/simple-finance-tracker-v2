package org.big.pete.sft.front.components.main

import cats.effect.SyncIO
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MICheckbox, MaterialIcon}
import org.big.pete.sft.domain.TransactionTracking
import org.big.pete.sft.front.domain.EnhancedTransaction


object Transactions {
  final val trackingToIcon = Map[TransactionTracking, String](
    TransactionTracking.None -> "horizontal_rule",
    TransactionTracking.Auto -> "blur_circular",
    TransactionTracking.Verified -> "check_circle"
  )

  case class Props(
      transactions: List[EnhancedTransaction],
      checkTransaction: (MICheckbox.Status, String) => SyncIO[Unit],
      trackingChanged: (Int, TransactionTracking) => SyncIO[Unit]
  )
  case class HeaderProps(checkTransaction: (MICheckbox.Status, String) => SyncIO[Unit])
  case class TransactionProps(
      transaction: EnhancedTransaction,
      checkTransaction: (MICheckbox.Status, String) => SyncIO[Unit],
      trackingChanged: (Int, TransactionTracking) => SyncIO[Unit]
  )

  val component: Scala.Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val reactTransactions = props.transactions.map { transaction =>
        transactionComponent.withKey(s"t-${transaction.id}")
          .apply(TransactionProps(transaction, props.checkTransaction, props.trackingChanged))
      }.toVdomArray

      tableWrap(
        headerComponent(HeaderProps(props.checkTransaction)),
        reactTransactions,
        headerComponent(HeaderProps(props.checkTransaction))
      )
    }.build

  val headerComponent: Component[HeaderProps, CtorType.Props] = ScalaFnComponent.apply[HeaderProps] { props =>
    <.tr(
      /// TODO: Checked status
      MICheckbox.component(MICheckbox.Props(
        <.th(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        "sft-all",
        "",
        MICheckbox.Status.none,
        props.checkTransaction
      )),
      <.th(^.cls := "date", "Date"),
      <.th(^.cls := "description", "Description"),
      <.th(^.cls := "amount", "Amount"),
      <.th(^.cls := "category", "Category"),
      <.th(^.cls := "money-account", "Account"),
      <.th(^.cls := "status center-align hide-on-med-and-down", "")
    )
  }

  val transactionComponent: Component[TransactionProps, CtorType.Props] = ScalaFnComponent.apply[TransactionProps] { props =>
    <.tr(
      /// TODO: Checked transaction
      MICheckbox.component(MICheckbox.Props(
        <.td(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        props.transaction.toString,
        "",
        MICheckbox.Status.none,
        props.checkTransaction
      )),
      <.td(^.cls := "date", props.transaction.date.format(DateFormat)),
      <.td(^.cls := "description", props.transaction.description),
      <.td(^.cls := "right-align amount", formatAmount(props.transaction.currencySymbol, props.transaction.amount)),
      <.td(^.cls := "category",
        <.span(^.cls := "show-on-large", props.transaction.categoryFullName),
        <.span(^.cls := "show-on-medium-and-down", props.transaction.categoryName)
      ),
      <.td(^.cls := "money-account", props.transaction.moneyAccountName),
      <.td(^.cls := "status center-align hide-on-med-and-down",
        MaterialIcon(
          MaterialIcon.`i`,
          MaterialIcon.`small`,
          trackingToIcon(props.transaction.tracking),
          props.trackingChanged(props.transaction.id, props.transaction.tracking)
        )
      )
    )
  }

}
