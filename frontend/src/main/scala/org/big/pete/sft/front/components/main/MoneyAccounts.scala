package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.scalajs.dom.console
import org.scalajs.dom.html.Element

import java.time.LocalDate
import scala.util.{Failure, Success, Try}


object MoneyAccounts {
  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      accounts: List[EnhancedMoneyAccount],
      currencies: List[Currency],
      save: (Option[Int], String, BigDecimal, String, LocalDate) => Callback
  )
  case class MoneyAccountProps(
      account: EnhancedMoneyAccount,
      openEditModal: EnhancedMoneyAccount => Callback
  )
  case class State(
      isOpen: Boolean,
      id: Option[Int],
      name: String,
      startAmount: BigDecimal,
      currency: Option[String],
      created: LocalDate
  )

  case class FormProps(
      currencies: List[Currency],
      id: Option[Int],
      name: String,
      startAmount: BigDecimal,
      currency: Option[String],
      created: LocalDate,
      changeName: ReactFormEventFromInput => Callback,
      changeAmount: ReactFormEventFromInput => Callback,
      changeCurrency: Currency => Callback,
      changeCreated: LocalDate => CallbackTo[LocalDate],
      save: Callback,
      close: Callback
  )

  implicit val formPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept[FormProps](
    "changeName",
    "changeAmount",
    "changeCurrency",
    "changeCreated",
    "save",
    "close"
  )


  class Backend($: BackendScope[Props, State]) {
    def changeName(e: ReactFormEventFromInput): Callback =
      $.modState(_.copy(name = e.target.value))

    def changeAmount(e: ReactFormEventFromInput): Callback = $.modState { state =>
      val newAmount = Try(BigDecimal(e.target.value.trim)) match {
        case Failure(_) => state.startAmount
        case Success(value) => value
      }
      console.log(s"newAmount: $newAmount")
      state.copy(startAmount = newAmount)
    }

    def changeCurrency(cur: Currency): Callback =
      $.modState(_.copy(currency = Some(cur.id)))

    def changeCreated(date: LocalDate): CallbackTo[LocalDate] =
      $.modState(_.copy(created = date)) >> CallbackTo.pure(date)

    def closeModal: Callback =
      $.modState(_.copy(isOpen = false))

    def saveModal: Callback = {
      for {
        props <- $.props
        state <- $.state
        _ <- props.save(state.id, state.name, state.startAmount, state.currency.get, state.created)
        _ <- closeModal
      } yield ()
    }

    def openAddNew: Callback = $.modState { state =>
      state.copy(isOpen = true, None, "", BigDecimal(0))
    }

    def openEditModal(account: EnhancedMoneyAccount): Callback = $.modState { state =>
      state.copy(isOpen = true, Some(account.id), account.name, account.startAmount, Some(account.currency.id), account.created)
    }


    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      val moneyAccounts = props.accounts
        .map(ema => moneyAccountComponent.withKey(s"ma-${ema.id}").apply(MoneyAccountProps(ema, openEditModal)))
        .toVdomArray

      tableWrap(
        AddModal.component(AddModal.Props("add-money-account-modal", state.isOpen))(moneyAccountForm(FormProps(
          props.currencies,
          state.id,
          state.name,
          state.startAmount,
          state.currency,
          state.created,
          changeName,
          changeAmount,
          changeCurrency,
          changeCreated,
          saveModal,
          closeModal
        ))),
        headerComponent(),
        moneyAccounts,
        headerComponent(),
        <.a(
          ^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> openAddNew,
          MaterialIcon("add"),
          "Add"
        )
      )
    }
  }

  val component: Scala.Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState(State(isOpen = false, None, "", BigDecimal(0), None, LocalDate.now()))
    .renderBackend[Backend]
    .build

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
      <.td(
        ^.cls := "name",
        ^.onClick --> props.openEditModal(props.account),
        props.account.name
      ),
      <.td(^.cls := "currency hide-on-small-only", s"${props.account.currency.name} (${props.account.currency.symbol})"),
      <.td(^.cls := "date hide-on-small-only", props.account.created.format(DateFormat)),
      <.td(^.cls := "amount", formatAmount(props.account.currency.symbol, props.account.periodStatus.start)),
      <.td(^.cls := "amount", formatAmount(props.account.currency.symbol, props.account.periodStatus.end))
    )
  }

  val moneyAccountForm: Scala.Component[FormProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[FormProps]
    .stateless
    .render_P { props =>
      <.form(
        <.div(
          ^.cls := "row",
          TextInput.component(TextInput.Props("add-ma-name", "Name", props.name, props.changeName, 301, List("col", "s12")))
        ),
        <.div(
          ^.cls := "row",
          TextInput.component(
            TextInput.Props(
              "add-ma-amount", "Start Amount", props.startAmount.toString(), props.changeAmount, 302, List("col", "s12")
            )
          )
        ),
        <.div(
          ^.cls := "row",
          SftMain.dropDownCurrency.component(
            SftMain.dropDownCurrency.Props(
              "add-ma-currency",
              "Currency",
              props.currencies,
              displayCurrency,
              cur => "ck-" + cur.id,
              props.changeCurrency,
              props.currencies.find(c => props.currency.contains(c.id)),
              303,
              List("col", "s12")
            )
          )
        ),
        <.div(
          ^.cls := "row",
          ReactDatePicker.DatePicker(
            ReactDatePicker.Props(
              "add-ma-started",
              "col s12",
              props.changeCreated,
              Some(props.created),
              isOpened = false,
              Some(304),
              ReactDatePicker.ExtendedKeyBindings
            )
          )
        ),
        ModalButtons(props.id.map(_ => "Save").getOrElse("Add"), 305, props.save, props.close)
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build
}
