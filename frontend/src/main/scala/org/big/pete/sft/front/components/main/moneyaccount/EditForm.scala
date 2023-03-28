package org.big.pete.sft.front.components.main.moneyaccount

import japgolly.scalajs.react.component.ScalaFn.{Component, Unmounted}
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{DropDown, MaterialIcon, TextInput}
import org.big.pete.sft.domain.Currency
import org.big.pete.sft.front.components.main.moneyaccount.Page.{CurrencyAmount, availableCurrencies}
import org.big.pete.sft.front.helpers.{ModalButtons, MoneyTextBox}

import java.time.LocalDate


object EditForm {
  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  case class FormProps(
      currencies: Map[String, Currency],
      id: Option[Int],
      name: StateSnapshot[String],
      created: StateSnapshot[LocalDate],
      currencyAmounts: Map[Int, StateSnapshot[CurrencyAmount]],
      addNext: Callback,
      remove: Int => Callback,
      save: Callback,
      close: Callback
  )

  case class CurrencyEditProps(
      id: Int,
      availableCurrencies: Map[String, Currency],
      startAmount: StateSnapshot[BigDecimal],
      maCurrency: StateSnapshot[Option[Currency]],
      hasNextButton: Boolean,
      hasDeleteButton: Boolean,
      tabIndex: Int,
      addNext: Callback,
      remove: Callback
  )


  implicit val formPropsReuse: Reusability[FormProps] =
    Reusability.caseClassExcept[FormProps]("addNext", "remove", "save", "close")
  implicit val currencyEditPropsReuse: Reusability[CurrencyEditProps] =
    Reusability.caseClassExcept[CurrencyEditProps]("addNext", "remove")


  val component: Component[FormProps, CtorType.Props] = ScalaFnComponent.withReuse[FormProps] { props =>
    def currenciesComponents: List[Unmounted[CurrencyEditProps]] = {
      val availableCurs = availableCurrencies(props.currencies, props.currencyAmounts.values.map(_.value))
      val ids = props.currencyAmounts.keys.toList.sorted
      val firstId = ids.head
      val lastId = ids.last

      val currencySS = StateSnapshot.withReuse.zoom(CurrencyAmount.currency.get)(CurrencyAmount.currency.replace)
      val amountSS = StateSnapshot.withReuse.zoom(CurrencyAmount.amount.get)(CurrencyAmount.amount.replace)

      ids.map { id =>
        val curAmount = props.currencyAmounts(id)
        val available = curAmount.value.currency.map(cur => availableCurs + (cur.id -> cur))
          .getOrElse(availableCurs)

        val currencyState = currencySS.prepare(curAmount.toModStateFn).apply(curAmount.value)
        val amountState = amountSS.prepare(curAmount.toModStateFn).apply(curAmount.value)

        currencyEditComponent.withKey(s"add-ma-ec-$id").apply(
          CurrencyEditProps(
            id, available, amountState, currencyState, id == lastId, id != firstId, 303 + id * 2, props.addNext, props.remove(id)
          )
        )
      }
    }

    <.form(
      <.div(^.cls := "row", TextInput("add-ma-name", "Name", props.name, 301, List("col", "s12"))),
      <.div(^.cls := "row", ReactDatePicker("add-ma-started", props.created, Set("col", "s12"), 302)),
      currenciesComponents.toVdomArray,
      ModalButtons(props.id.map(_ => "Save").getOrElse("Add"), 395, props.save, props.close)
    )
  }

  def apply(
      currencies: Map[String, Currency],
      id: Option[Int],
      name: StateSnapshot[String],
      created: StateSnapshot[LocalDate],
      currencyAmounts: Map[Int, StateSnapshot[CurrencyAmount]],
      addNext: Callback,
      remove: Int => Callback,
      save: Callback,
      close: Callback
  ): Unmounted[FormProps] =
    component.apply(FormProps(currencies, id, name, created, currencyAmounts, addNext, remove, save, close))


  private val currencyEditComponent: Component[CurrencyEditProps, CtorType.Props] = ScalaFnComponent.withReuse[CurrencyEditProps]
    { props =>
      val columns = (props.hasNextButton, props.hasDeleteButton) match {
        case (true, true) => "s8"
        case (false, true) => "s10"
        case (true, false) => "s10"
        case (false, false) => "s12"
      }
      val iconClasses = Set("col", "s2", "edit-icon", "center-align", "pointer")

      ReactFragment(
        <.div(^.cls := "row",
          DropDown(
            s"add-ma-currency-${props.id}",
            "Currency",
            props.availableCurrencies.values.toList,
            props.maCurrency,
            props.tabIndex,
            List("col", "s12")
          ),
        ),
        <.div(^.cls := "row valign-wrapper",
          MoneyTextBox(s"add-ma-amount-${props.id}", "Start Amount", props.startAmount, props.tabIndex + 1, List("col", columns)),
          MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.midMedium, "delete", props.remove, iconClasses)).when(props.hasDeleteButton),
          MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.midMedium, "add", props.addNext, iconClasses)).when(props.hasNextButton)
        )
      )
    }

}
