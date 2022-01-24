package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{CtorType, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.sft.domain.EnhancedMoneyAccount


object MoneyAccounts {
  case class Props(accounts: List[EnhancedMoneyAccount])
  case class MoneyAccountProps(account: EnhancedMoneyAccount)

  val component: Scala.Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      val moneyAccounts = props.accounts
        .map(ema => moneyAccountComponent.withKey(s"ma-${ema.id}").apply(MoneyAccountProps(ema)))
        .toVdomArray

      tableWrap(
        TagMod.empty,
        headerComponent(),
        moneyAccounts,
        headerComponent(),
        TagMod.empty
      )
    }.build

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    <.tr(
      <.th(^.cls := "id hide-on-small-only center-align", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "currency", "Currency"),
      <.th(^.cls := "date", "Created"),
      <.th(^.cls := "amount", "Start Amount", <.span("by period")),
      <.th(^.cls := "amount", "End Amount", <.span("by period"))
    )
  }

  val moneyAccountComponent: Component[MoneyAccountProps, CtorType.Props] = ScalaFnComponent.apply[MoneyAccountProps] { props =>
    <.tr(
      <.td(^.cls := "id hide-on-small-only right-align", props.account.id.toString),
      <.td(^.cls := "name", props.account.name),
      <.td(^.cls := "currency", s"${props.account.currency.name} (${props.account.currency.symbol})"),
      <.td(^.cls := "date", props.account.created.format(DateFormat)),
      <.td(^.cls := "amount", formatAmount(props.account.currency.symbol, props.account.periodStatus.start)),
      <.td(^.cls := "amount", formatAmount(props.account.currency.symbol, props.account.periodStatus.end))
    )
  }
}
