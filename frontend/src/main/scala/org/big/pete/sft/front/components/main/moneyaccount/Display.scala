package org.big.pete.sft.front.components.main.moneyaccount

import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon
import org.big.pete.sft.domain.EnhancedMoneyAccount
import org.big.pete.sft.front.components.main.{DateFormat, formatAmount}


object Display {
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      account: EnhancedMoneyAccount,
      openEditModal: EnhancedMoneyAccount => Callback,
      openDeleteModal: EnhancedMoneyAccount => Callback
  )

  implicit val maDisplayPropsReuse: Reusability[Props] =
    Reusability.caseClassExcept[Props]("openEditModal", "openDeleteModal")

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.withReuse[Unit] { _ =>
    <.tr(
      <.th(^.cls := "id hide-on-small-only center-align", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "date hide-on-med-and-down", "Created"),
      <.th(^.cls := "currency hide-on-med-and-down", "Currencies"),
      <.th(^.cls := "amount", "Start Amount", <.span(^.cls := "hide-on-med-and-down", "by period")),
      <.th(^.cls := "amount", "End Amount", <.span(^.cls := "hide-on-med-and-down", "by period")),
      <.th(^.cls := "delete hide-on-med-and-down", "")
    )
  }

  val lineComponent: Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { props =>
    <.tr(
      <.td(^.cls := "id hide-on-small-only right-align", props.account.id.toString),
      <.td(
        ^.cls := "name pointer",
        ^.onClick --> props.openEditModal(props.account),
        props.account.name
      ),
      <.td(^.cls := "date hide-on-med-and-down", props.account.created.format(DateFormat)),
      <.td(
        ^.cls := "currency hide-on-med-and-down",
        props.account.status.map { item =>
          <.div(^.key := s"cur-${item.currency.id}-${props.account.id}", s"${item.currency.name} (${item.currency.symbol})")
        }.toVdomArray
      ),
      <.td(
        ^.cls := "amount",
        props.account.status.map { item =>
          <.div(^.key := s"start-${item.currency.id}-${props.account.id}", formatAmount(item.currency.symbol, item.start))
        }.toVdomArray
      ),
      <.td(
        ^.cls := "amount",
        props.account.status.map { item =>
          <.div(^.key := s"end-${item.currency.id}-${props.account.id}", formatAmount(item.currency.symbol, item.end))
        }.toVdomArray
      ),
      <.td(
        ^.cls := "delete hide-on-med-and-down",
        MaterialIcon(MaterialIcon.i, MaterialIcon.small, "delete", props.openDeleteModal(props.account))
      ),
    )
  }
}
