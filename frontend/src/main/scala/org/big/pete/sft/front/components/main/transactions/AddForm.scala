package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Ref, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{HasFocus, TextInput, WithFocus}
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, TransactionType}
import org.big.pete.sft.front.SftMain.{dropDownCategoryTree, dropDownCurrency, dropDownMoneyAccount, dropDownTT}
import org.big.pete.sft.front.components.main.displayCurrency
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.helpers.{ModalButtons, SimpleCheckbox}
import org.scalajs.dom.html.Form

import java.time.LocalDate


object AddForm {
  import org.big.pete.react.Implicits.bigDecimalReuse
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      linearCats: List[CategoryTree],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      id: Option[Int],
      date: LocalDate,
      transactionType: TransactionType,
      amount: BigDecimal,
      destAmount: Option[BigDecimal],
      description: String,
      categoryId: Option[Int],
      moneyAccountId: Option[Int],
      destMAId: Option[Int],
      currency: Option[String],
      destCurrency: Option[String],
      addNext: Boolean,
      dateChange: LocalDate => CallbackTo[LocalDate],
      ttChange: TransactionType => Callback,
      amountChange: ReactFormEventFromInput => Callback,
      descriptionChange: ReactFormEventFromInput => Callback,
      categoryChange: CategoryTree => Callback,
      maChange: EnhancedMoneyAccount => Callback,
      currencyChange: Currency => Callback,
      destinationMAChange: EnhancedMoneyAccount => Callback,
      destinationAmountChange: ReactFormEventFromInput => Callback,
      destinationCurrencyChange: Currency => Callback,
      addNextChange: ReactFormEventFromInput => Callback,
      save: Callback,
      close: Callback
  )

  implicit val formPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props](
    "dateChange", "ttChange", "amountChange", "descriptionChange", "categoryChange", "maChange", "currencyChange",
    "destinationMAChange", "destinationAmountChange", "destinationCurrencyChange", "addNextChange", "save", "close"
  )

  class Backend extends WithFocus[ReactDatePicker.Props, ReactDatePicker.State, ReactDatePicker.Backend] {
    private val refType = Ref.toScalaComponent(dropDownTT.component)
    private val refAmount = Ref.toScalaComponent(TextInput.component)
    private val refDescription = Ref.toScalaComponent(TextInput.component)
    private val refCategory = Ref.toScalaComponent(dropDownCategoryTree.component)
    private val refMoneyAccount = Ref.toScalaComponent(dropDownMoneyAccount.component)
    private val refCurrency = Ref.toScalaComponent(dropDownCurrency.component)
    private val refDestMoneyAccount = Ref.toScalaComponent(dropDownMoneyAccount.component)
    private val refDestCurrency = Ref.toScalaComponent(dropDownCurrency.component)
    private val refDestAmount = Ref.toScalaComponent(TextInput.component)
    private val refAddNext = Ref.toScalaComponent(SimpleCheckbox.component)
    private val refButtons = Ref.toScalaComponent(ModalButtons.comp)

    def shiftFocus(ref: Ref.WithScalaComponent[_, _, _ <: HasFocus, CtorType.Props]): Callback =
      ref.foreachCB(_.backend.focus).async.delayMs(50).toCallback

    def render(props: Props): VdomTagOf[Form] = {
      def getAvailableCurrencies(maId: Option[Int]): List[Currency] =
        maId.map(id => props.moneyAccounts(id).status.map(_.currency))
          .getOrElse(List.empty)

      val refToLast = (if (props.id.isEmpty) refAddNext else refButtons)
        .asInstanceOf[Ref.WithScalaComponent[Any, Any, _ <: HasFocus, CtorType.Props]]
      val refToNext = (if (props.transactionType == TransactionType.Transfer) refDestMoneyAccount else refToLast)
        .asInstanceOf[Ref.WithScalaComponent[Any, Any, _ <: HasFocus, CtorType.Props]]

      val mainAccountCurrencies = getAvailableCurrencies(props.moneyAccountId)
      val destAccountCurrencies = getAvailableCurrencies(props.destMAId)

      <.form(
        <.div(^.cls := "row",
          ReactDatePicker.DatePicker.withRef(focusRef)(
            ReactDatePicker.Props(
              "add-tr-date",
              "col s12",
              props.dateChange,
              props.date,
              isOpened = false,
              Some(401),
              ReactDatePicker.ExtendedKeyBindings,
              shiftFocus(refType)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownTT.component.withRef(refType)(
            dropDownTT.Props(
              "add-tr-tt",
              "Transaction Type",
              TransactionType.values.toList,
              _.toString,
              _.toString,
              props.ttChange,
              Some(props.transactionType),
              402,
              List("col", "s12"),
              shiftFocus(refAmount)
            )
          )
        ),
        <.div(^.cls := "row",
          TextInput.component.withRef(refAmount)(TextInput.Props(
            "add-tr-amount", "Amount", props.amount.toString(), props.amountChange, 403, List("col", "s12"), shiftFocus(refDescription)
          ))
        ),
        <.div(^.cls := "row",
          TextInput.component.withRef(refDescription)(TextInput.Props(
            "add-tr-description", "Description", props.description, props.descriptionChange, 404, List("col", "s12"), shiftFocus(refCategory)
          ))
        ),
        <.div(^.cls := "row",
          dropDownCategoryTree.component.withRef(refCategory)(
            dropDownCategoryTree.Props(
              "add-tr-category",
              "Category",
              props.linearCats,
              CategoryTree.name,
              cat => s"k-cat-${cat.id}",
              props.categoryChange,
              props.categoryId.flatMap(id => props.linearCats.find(_.id == id)),
              405,
              List("col", "s12"),
              shiftFocus(refMoneyAccount)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component.withRef(refMoneyAccount)(
            dropDownMoneyAccount.Props(
              "add-tr-ma",
              "Money Account",
              props.moneyAccounts.values.toList,
              _.name,
              ma => s"k-ma-${ma.id}",
              props.maChange,
              props.moneyAccountId.flatMap(id => props.moneyAccounts.get(id)),
              406,
              List("col", "s12"),
              shiftFocus(refCurrency)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownCurrency.component.withRef(refCurrency)(
            dropDownCurrency.Props(
              "add-tr-currency",
              "Currency",
              mainAccountCurrencies,
              displayCurrency,
              cur => s"ck-${cur.id}",
              props.currencyChange,
              props.currency.flatMap(currencyId => mainAccountCurrencies.find(_.id == currencyId)),
              407,
              List("col", "s12"),
              shiftFocus(refToNext)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component.withRef(refDestMoneyAccount)(
            dropDownMoneyAccount.Props(
              "add-tr-ma-dest",
              "Destination Money Account",
              props.moneyAccounts.values.toList,
              _.name,
              ma => s"k-ma-${ma.id}",
              props.destinationMAChange,
              props.destMAId.flatMap(id => props.moneyAccounts.get(id)),
              408,
              List("col", "s12"),
              shiftFocus(refDestCurrency)
            )
          )
        ).when(props.transactionType == TransactionType.Transfer),
        <.div(^.cls := "row",
          dropDownCurrency.component.withRef(refDestCurrency)(
            dropDownCurrency.Props(
              "add-tr-currency-dest",
              "Destination Currency",
              destAccountCurrencies,
              displayCurrency,
              cur => s"ck-dest-${cur.id}",
              props.destinationCurrencyChange,
              props.destCurrency.flatMap(currencyId => destAccountCurrencies.find(_.id == currencyId)),
              409,
              List("col", "s12"),
              shiftFocus(refDestAmount)
            )
          )
        ).when(props.transactionType == TransactionType.Transfer),
        <.div(^.cls := "row",
          TextInput.component.withRef(refDestAmount)(
            TextInput.Props(
              "add-tr-amount-dest",
              "Destination Amount",
              props.destAmount.map(_.toString()).getOrElse(""),
              props.destinationAmountChange,
              410,
              List("col", "s12"),
              shiftFocus(refToLast)
            )
          )
        ).when(props.transactionType == TransactionType.Transfer),
        <.div(^.cls := "row",
          SimpleCheckbox.component.withRef(refAddNext)(SimpleCheckbox.Props("Add another", props.addNext, 411, props.addNextChange))
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
