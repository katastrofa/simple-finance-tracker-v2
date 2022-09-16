package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.scalajs.dom.html.Form

import java.time.LocalDate
import scala.util.{Failure, Success, Try}


object MoneyAccounts {
  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      accounts: List[EnhancedMoneyAccount],
      currencies: List[Currency],
      publish: (String, BigDecimal, String, LocalDate) => Callback
  )
  case class MoneyAccountProps(account: EnhancedMoneyAccount)

  case class FormProps(
      currencies: List[Currency],
      publish: (String, BigDecimal, String, LocalDate) => Callback,
      close: Callback
  )
  case class FormState(name: String, startAmount: BigDecimal, currency: String, created: LocalDate)

  implicit val formPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept[FormProps]("publish", "close")
  implicit val formStateReuse: Reusability[FormState] = Reusability.derive[FormState]


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
        <.a(^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> $.modState(_ => true),
          MaterialIcon("add"),
          "Add"
        )
      )
    }.build

  val headerComponent: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    <.tr(
      <.th(^.cls := "id hide-on-small-only center-align", "ID"),
      <.th(^.cls := "name", "Name"),
      <.th(^.cls := "currency hide-on-small-only", "Currency"),
      <.th(^.cls := "date hide-on-small-only", "Created"),
      <.th(^.cls := "amount", "Start Amount", <.span("by period")),
      <.th(^.cls := "amount", "End Amount", <.span("by period"))
    )
  }

  val moneyAccountComponent: Component[MoneyAccountProps, CtorType.Props] = ScalaFnComponent.apply[MoneyAccountProps] { props =>
    <.tr(
      <.td(^.cls := "id hide-on-small-only right-align", props.account.id.toString),
      <.td(^.cls := "name", props.account.name),
      <.td(^.cls := "currency hide-on-small-only", s"${props.account.currency.name} (${props.account.currency.symbol})"),
      <.td(^.cls := "date hide-on-small-only", props.account.created.format(DateFormat)),
      <.td(^.cls := "amount", formatAmount(props.account.currency.symbol, props.account.periodStatus.start)),
      <.td(^.cls := "amount", formatAmount(props.account.currency.symbol, props.account.periodStatus.end))
    )
  }

  class FormBackend($: BackendScope[FormProps, FormState]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    def changeStartAmount(e: ReactFormEventFromInput): Callback = $.modState { state =>
      val newAmount = Try(BigDecimal(e.target.value.trim)) match {
        case Failure(_) => state.startAmount
        case Success(value) => value
      }
      state.copy(startAmount = newAmount)
    }

    def clean: Callback =
      $.modState(_.copy(name = "", startAmount = BigDecimal(0), created = LocalDate.now))

    def publish: Callback = for {
      props <- $.props
      state <- $.state
      _ <- props.close
      _ <- props.publish(state.name, state.startAmount, state.currency, state.created)
      _ <- clean
    } yield ()

    def render(props: FormProps, state: FormState): VdomTagOf[Form] = {
      <.form(
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-ma-name", "Name", state.name, changeName, 301, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-ma-amount", "Start Amount", state.startAmount.toString(), changeStartAmount, 302, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          SftMain.dropDownCurrency.component(SftMain.dropDownCurrency.Props(
            "add-ma-currency",
            "Currency",
            props.currencies,
            displayCurrency,
            cur => "ck-" + cur.id,
            cur => $.modState(_.copy(currency = cur.id)),
            props.currencies.find(c => c.id == state.currency),
            303,
            List("col", "s12")
          ))
        ),
        <.div(^.cls := "row",
          ReactDatePicker.DatePicker(ReactDatePicker.Props(
            "add-ma-started",
            "col s12",
            ld => $.modState(_.copy(created = ld)) >> CallbackTo.pure(ld),
            Some(state.created),
            isOpened = false,
            Some(304),
            ReactDatePicker.ExtendedKeyBindings
          ))
        ),
        ModalButtons.add(305, publish, props.close >> clean)
      )
    }
  }

  val moneyAccountForm: Scala.Component[FormProps, FormState, FormBackend, CtorType.Props] = ScalaComponent.builder[FormProps]
    .initialStateFromProps(props => FormState("", BigDecimal(0), props.currencies.head.id, LocalDate.now()))
    .renderBackend[FormBackend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
