package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.hooks.Hooks
import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.TextInput
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.helpers.AddModal

import java.time.LocalDate
import scala.util.{Failure, Success, Try}


object MoneyAccounts {
//  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      accounts: List[EnhancedMoneyAccount],
      currencies: List[Currency],
      publish: (String, BigDecimal, Int, LocalDate) => Callback
  )
  case class MoneyAccountProps(account: EnhancedMoneyAccount)

  case class FormProps(
      currencies: List[Currency],
      publish: (String, BigDecimal, Int, LocalDate) => Callback,
      close: Callback
  )

  implicit val modalPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept("publish", "close")


  val component: Scala.Component[Props, Boolean, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[Boolean](false)
    .renderPS { ($, props, isOpen) =>
      val moneyAccounts = props.accounts
        .map(ema => moneyAccountComponent.withKey(s"ma-${ema.id}").apply(MoneyAccountProps(ema)))
        .toVdomArray

      tableWrap(
        AddModal.component(AddModal.Props("add-money-account-modal", isOpen))(
          moneyAccountForm(FormProps(props.currencies, props.publish, $.modState(_ => false)))
        ),
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

  val moneyAccountForm: Component[FormProps, CtorType.Props] = ScalaFnComponent.withHooks[FormProps]
    .useState("")
    .useState(BigDecimal.apply(0))
    .useStateBy[Int]((props: FormProps, _: Hooks.UseState[String], _: Hooks.UseState[BigDecimal]) => props.currencies.head.id)
    .useState(LocalDate.now())
    .render { (props: FormProps, name: Hooks.UseState[String], startAmount: Hooks.UseState[BigDecimal], currency: Hooks.UseState[Int], created: Hooks.UseState[LocalDate]) =>
      def changeName(e: ReactFormEventFromInput): Callback =
        name.modState(_ => e.target.value)
      def changeStartAmount(e: ReactFormEventFromInput): Callback = startAmount.modState { amount =>
        Try(BigDecimal(e.target.value.trim)) match {
          case Failure(_) => amount
          case Success(value) => value
        }
      }

      <.form(
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-ma-name", "Name", name.value, changeName, 301, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-ma-amount", "Start Amount", startAmount.value.toString(), changeStartAmount, 302, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          SftMain.dropDownCurrency.component(SftMain.dropDownCurrency.Props(
            "add-ma-currency",
            "Currency",
            props.currencies,
            displayCurrency,
            cur => "ck-" + cur.id.toString,
            cur => currency.modState(_ => cur.id),
            props.currencies.find(c => c.id == currency.value),
            303,
            List("col", "s12")
          ))
        ),
        <.div(^.cls := "row",
          ReactDatePicker.DatePicker(ReactDatePicker.Props(
            "add-ma-started",
            "col s12",
            ld => created.modState(_ => ld) >> CallbackTo.pure(ld),
            Some(created.value),
            isOpened = false,
            ReactDatePicker.ExtendedKeyBindings
          ))
        ),
        <.div(^.cls := "row",
          <.div(^.cls := "col s12 right-align")
        )
      )
    }
}
