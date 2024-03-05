package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Ref, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{DropDown, HasFocus, TextInput, WithFocus}
import org.big.pete.sft.domain.{Category, Currency, EnhancedAccount, TransactionType}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.helpers.{ModalButtons, MoneyTextBox, SimpleCheckbox}
import org.scalajs.dom.html.Form

import java.time.LocalDate


object EditForm {
  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      linearCats: List[CategoryTree],
      categories: Map[Int, Category],
      moneyAccounts: Map[Int, EnhancedAccount],
      id: Option[Int],
      date: StateSnapshot[LocalDate],
      transactionType: StateSnapshot[Option[TransactionType]],
      amount: StateSnapshot[BigDecimal],
      destAmount: StateSnapshot[BigDecimal],
      description: StateSnapshot[String],
      category: StateSnapshot[Option[CategoryTree]],
      moneyAccount: StateSnapshot[Option[EnhancedAccount]],
      destMoneyAccount: StateSnapshot[Option[EnhancedAccount]],
      currency: StateSnapshot[Option[Currency]],
      destCurrency: StateSnapshot[Option[Currency]],
      addNext: StateSnapshot[Boolean],
      save: Callback,
      close: Callback
  )

  implicit val formPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("save", "close")

  class Backend extends WithFocus[ReactDatePicker.Props, ReactDatePicker.State, ReactDatePicker.Backend] {
    private val refType = Ref.toScalaComponent(DropDown.component)
    private val refAmount = Ref.toScalaComponent(MoneyTextBox.component)
    private val refDescription = Ref.toScalaComponent(TextInput.component)
    private val refCategory = Ref.toScalaComponent(DropDown.component)
    private val refMoneyAccount = Ref.toScalaComponent(DropDown.component)
    private val refCurrency = Ref.toScalaComponent(DropDown.component)
    private val refDestMoneyAccount = Ref.toScalaComponent(DropDown.component)
    private val refDestCurrency = Ref.toScalaComponent(DropDown.component)
    private val refDestAmount = Ref.toScalaComponent(MoneyTextBox.component)
    private val refAddNext = Ref.toScalaComponent(SimpleCheckbox.component)
    private val refButtons = Ref.toScalaComponent(ModalButtons.comp)

    private def shiftFocus(ref: Ref.WithScalaComponent[_, _, _ <: HasFocus, CtorType.Props]): Callback =
      ref.foreachCB(_.backend.focus).async.delayMs(50).toCallback

    def render(props: Props): VdomTagOf[Form] = {
      def getAvailableCurrencies(maId: Option[Int]): List[Currency] =
        maId.map(id => props.moneyAccounts(id).status.map(_.currency))
          .getOrElse(List.empty)

      val refToLast = (if (props.id.isEmpty) refAddNext else refButtons)
        .asInstanceOf[Ref.WithScalaComponent[Any, Any, _ <: HasFocus, CtorType.Props]]
      val refToNext = (if (props.transactionType.value.contains(TransactionType.Transfer)) refDestMoneyAccount else refToLast)
        .asInstanceOf[Ref.WithScalaComponent[Any, Any, _ <: HasFocus, CtorType.Props]]

      val mainAccountCurrencies = getAvailableCurrencies(props.moneyAccount.value.map(_.id))
      val destAccountCurrencies = getAvailableCurrencies(props.destMoneyAccount.value.map(_.id))

      <.form(
        <.div(^.cls := "row",
          ReactDatePicker.withRef(focusRef)("add-tr-date", props.date, Set("col", "s12"), 401, onEnterHit = shiftFocus(refType))
        ),
        <.div(^.cls := "row",
          DropDown.withRef(refType)(
            "add-tr-tt",
            "Transaction Type",
            TransactionType.values.toList,
            props.transactionType,
            402,
            List("col", "s12"),
            shiftFocus(refAmount)
          )
        ),
        <.div(^.cls := "row",
          MoneyTextBox.withRef(refAmount)(
            "add-tr-amount", "Amount", props.amount, 403, List("col", "s12"), shiftFocus(refDescription)
          )
        ),
        <.div(^.cls := "row",
          TextInput.withRef(refDescription)(
            "add-tr-description", "Description", props.description, 404, List("col", "s12"), shiftFocus(refCategory)
          )
        ),
        <.div(^.cls := "row",
          DropDown.withRef(refCategory)(
            "add-tr-category",
            "Category",
            props.linearCats,
            props.category,
            405,
            List("col", "s12"),
            shiftFocus(refMoneyAccount)
          )
        ),
        <.div(^.cls := "row",
          DropDown.withRef(refMoneyAccount)(
            "add-tr-ma",
            "Money Account",
            props.moneyAccounts.values.toList,
            props.moneyAccount,
            406,
            List("col", "s12"),
            shiftFocus(refCurrency)
          )
        ),
        <.div(^.cls := "row",
          DropDown.withRef(refCurrency)(
            "add-tr-currency",
            "Currency",
            mainAccountCurrencies,
            props.currency,
            407,
            List("col", "s12"),
            shiftFocus(refToNext)
          )
        ),
        <.div(^.cls := "row",
          DropDown.withRef(refDestMoneyAccount)(
            "add-tr-ma-dest",
            "Destination Money Account",
            props.moneyAccounts.values.toList,
            props.destMoneyAccount,
            408,
            List("col", "s12"),
            shiftFocus(refDestCurrency)
          )
        ).when(props.transactionType.value.contains(TransactionType.Transfer)),
        <.div(^.cls := "row",
          DropDown.withRef(refDestCurrency)(
            "add-tr-currency-dest",
            "Destination Currency",
            destAccountCurrencies,
            props.destCurrency,
            409,
            List("col", "s12"),
            shiftFocus(refDestAmount)
          )
        ).when(props.transactionType.value.contains(TransactionType.Transfer)),
        <.div(^.cls := "row",
          MoneyTextBox.withRef(refDestAmount)(
            "add-tr-amount-dest", "Destination Amount", props.destAmount, 410, List("col", "s12"), shiftFocus(refToLast)
          )
        ).when(props.transactionType.value.contains(TransactionType.Transfer)),
        <.div(^.cls := "row",
          SimpleCheckbox.withRef(refAddNext)("Add another", props.addNext, 411)
        ).when(props.id.isEmpty),
        ModalButtons.comp.withRef(refButtons)(
          ModalButtons.Props(props.id.map(_ => "Save").getOrElse("Add"), 412, props.save, props.close)
        )
      )
    }
  }

  val component: Scala.Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .componentDidMount(_.backend.focus)
    .configure(Reusability.shouldComponentUpdate)
    .build
}
