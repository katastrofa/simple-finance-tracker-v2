package org.big.pete.sft.front.components.main.moneyaccount

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CallbackTo, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent, ScalaFnComponent}
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{MaterialIcon, TextInput}
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, MoneyAccountOptionalCurrency}
import org.big.pete.sft.front.SftMain
import org.big.pete.sft.front.components.main.displayCurrency
import org.big.pete.sft.front.helpers.{ModalButtons, MoneyTextBox}

import java.time.LocalDate


object Forms {
  import org.big.pete.sft.front.domain.Implicits._

  case class FormProps(
      currencies: Map[String, Currency],
      id: Option[Int],
      name: String,
      created: LocalDate,
      editCurrencies: Map[Int, MoneyAccountOptionalCurrency],
      changeName: ReactFormEventFromInput => Callback,
      changeAmount: Int => BigDecimal => Callback,
      changeCurrency: Int => Currency => Callback,
      changeCreated: LocalDate => CallbackTo[LocalDate],
      addEditCurrency: Callback,
      removeEditCurrency: Int => Callback,
      save: Callback,
      close: Callback
  )

  case class CurrencyEditProps(
      availableCurrencies: Map[String, Currency],
      maCurrency: MoneyAccountOptionalCurrency,
      hasNextButton: Boolean,
      hasDeleteButton: Boolean,
      tabIndex: Int,
      changeAmount: BigDecimal => Callback,
      changeCurrency: Currency => Callback,
      addNext: Callback,
      remove: Callback
  )

  case class DeleteMoneyAccountProps(
      accounts: List[EnhancedMoneyAccount],
      toDelete: Option[EnhancedMoneyAccount],
      shiftTransactionsTo: Map[String, Int],
      changeShiftTransactionsForCurrency: String => EnhancedMoneyAccount => Callback,
      deleteMoneyAccount: Callback,
      closeDeleteModal: Callback
  )

  case class SingleShiftTransactionsProps(
      availableAccounts: List[EnhancedMoneyAccount],
      shift: Int,
      currency: Currency,
      tabIndex: Int,
      changeShiftTransactions: EnhancedMoneyAccount => Callback
  )

  implicit val formPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept[FormProps](
    "changeName", "changeAmount", "changeCurrency", "changeCreated", "addEditCurrency", "removeEditCurrency", "save", "close"
  )
  implicit val currencyEditPropsReuse: Reusability[CurrencyEditProps] = Reusability.caseClassExcept[CurrencyEditProps](
    "changeAmount", "changeCurrency", "addNext", "remove"
  )
  implicit val deleteMoneyAccountPropsReuse: Reusability[DeleteMoneyAccountProps] =
    Reusability.caseClassExcept[DeleteMoneyAccountProps]("changeShiftTransactionsForCurrency", "deleteMoneyAccount", "closeDeleteModal")
  implicit val singleShiftTransactionsPropsReuse: Reusability[SingleShiftTransactionsProps] =
    Reusability.caseClassExcept[SingleShiftTransactionsProps]("changeShiftTransactions")

  final val NoShiftMoneyAccount =
    EnhancedMoneyAccount(-42, "Do not shift - delete", LocalDate.now(), List.empty, List.empty, None)


  val editForm: Scala.Component[FormProps, Unit, Unit, CtorType.Props] = ScalaComponent.builder[FormProps]
    .stateless
    .render_P { props =>
      val availableCurrencies = props.currencies.filterNot { case (id, _) => props.editCurrencies.exists(_._2.currency.exists(_ == id)) }
      val editCurrencyIds = props.editCurrencies.keys.toList.sorted
      val firstId = editCurrencyIds.head
      val lastId = editCurrencyIds.last

      val editCurrenciesDOM = editCurrencyIds.map { id =>
        val editCurrency = props.editCurrencies(id)
        val available = if (editCurrency.currency.isDefined)
          availableCurrencies + (editCurrency.currency.get -> props.currencies(editCurrency.currency.get))
        else
          availableCurrencies

        currencyEditComponent.withKey(s"add-ma-ec-$id").apply(
          CurrencyEditProps(
            available,
            editCurrency,
            id == lastId,
            id != firstId,
            303 + id * 2,
            props.changeAmount(id),
            props.changeCurrency(id),
            props.addEditCurrency,
            props.removeEditCurrency(id)
          )
        )
      }.toVdomArray

      <.form(
        <.div(^.cls := "row",
          TextInput.component(TextInput.Props("add-ma-name", "Name", props.name, props.changeName, 301, List("col", "s12")))
        ),
        <.div(^.cls := "row",
          ReactDatePicker.DatePicker(
            ReactDatePicker.Props(
              "add-ma-started",
              "col s12",
              props.changeCreated,
              props.created,
              isOpened = false,
              Some(302),
              ReactDatePicker.ExtendedKeyBindings,
              Callback.empty
            )
          )
        ),
        editCurrenciesDOM,
        ModalButtons(props.id.map(_ => "Save").getOrElse("Add"), 395, props.save, props.close)
      )
    }
    .configure(Reusability.shouldComponentUpdate)
    .build

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
          SftMain.dropDownCurrency.component(
            SftMain.dropDownCurrency.Props(
              s"add-ma-currency-${props.maCurrency.id}",
              "Currency",
              props.availableCurrencies.values.toList,
              displayCurrency,
              cur => s"ck-${props.maCurrency.id}-" + cur.id,
              props.changeCurrency,
              props.maCurrency.currency.flatMap(props.availableCurrencies.get),
              props.tabIndex,
              List("col", "s12")
            )
          )
        ),
        <.div(^.cls := "row valign-wrapper",
          MoneyTextBox.component(MoneyTextBox.Props(
            s"add-ma-amount-${props.maCurrency.id}",
            "Start Amount",
            props.maCurrency.startAmount,
            props.changeAmount,
            props.tabIndex + 1,
            List("col", columns)
          )),
          MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.midMedium, "delete", props.remove, iconClasses)).when(props.hasDeleteButton),
          MaterialIcon.Icon(MaterialIcon.Props(MaterialIcon.i, MaterialIcon.midMedium, "add", props.addNext, iconClasses)).when(props.hasNextButton)
        )
      )
    }


  val deleteMoneyAccountForm: Component[DeleteMoneyAccountProps, CtorType.Props] = ScalaFnComponent.withReuse[DeleteMoneyAccountProps] { props =>
    <.form(
      props.toDelete.get.currencies.map { maCurrency =>
        val available = NoShiftMoneyAccount :: props.accounts.filter(_.currencies.exists(_.currency.id == maCurrency.currency.id))
        singleShiftTransactions.withKey(s"com-delete-ma-shift-${maCurrency.currency.id}").apply(SingleShiftTransactionsProps(
          available,
          props.shiftTransactionsTo(maCurrency.currency.id),
          maCurrency.currency,
          320 + maCurrency.id,
          props.changeShiftTransactionsForCurrency(maCurrency.currency.id)
        ))
      }.toVdomArray,
      ModalButtons("Delete", 351, props.deleteMoneyAccount, props.closeDeleteModal)
    )
  }

  private val singleShiftTransactions = ScalaFnComponent.withReuse[SingleShiftTransactionsProps] { props =>
    <.div(^.cls := "row",
      SftMain.dropDownMoneyAccount.component(SftMain.dropDownMoneyAccount.Props(
        s"delete-ma-shift-${props.currency.id}",
        "Shift Transactions for " + displayCurrency(props.currency),
        props.availableAccounts,
        _.name,
        ma => s"d-ma-${ma.id}",
        props.changeShiftTransactions,
        props.availableAccounts.find(_.id == props.shift),
        props.tabIndex,
        List("col", "s12")
      ))
    )
  }
}
