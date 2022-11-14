package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Ref, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{HasFocus, TextInput, WithFocus}
import org.big.pete.sft.domain.{EnhancedMoneyAccount, TransactionType}
import org.big.pete.sft.front.SftMain.{dropDownCategoryTree, dropDownMoneyAccount, dropDownTT}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.helpers.{ModalButtons, SimpleCheckbox}
import org.scalajs.dom.html.Form

import java.time.LocalDate


object AddForm {
  import org.big.pete.react.Implicits.bigDecimalReuse
  import org.big.pete.sft.front.domain.Implicits._
  import Page.moneyAccountMapReuse

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
      addNext: Boolean,
      dateChange: LocalDate => CallbackTo[LocalDate],
      ttChange: TransactionType => Callback,
      amountChange: ReactFormEventFromInput => Callback,
      descriptionChange: ReactFormEventFromInput => Callback,
      categoryChange: CategoryTree => Callback,
      maChange: EnhancedMoneyAccount => Callback,
      destinationMAChange: EnhancedMoneyAccount => Callback,
      destinationAmountChange: ReactFormEventFromInput => Callback,
      addNextChange: ReactFormEventFromInput => Callback,
      save: Callback,
      close: Callback
  )

  implicit val formPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props](
    "dateChange", "ttChange", "amountChange", "descriptionChange", "categoryChange", "maChange",
    "destinationMAChange", "destinationAmountChange", "addNextChange", "save", "close"
  )

  class Backend extends WithFocus[ReactDatePicker.Props, ReactDatePicker.State, ReactDatePicker.Backend] {
    private val ref1 = Ref.toScalaComponent(dropDownTT.component)
    private val ref2 = Ref.toScalaComponent(TextInput.component)
    private val ref3 = Ref.toScalaComponent(TextInput.component)
    private val ref4 = Ref.toScalaComponent(dropDownCategoryTree.component)
    private val ref5 = Ref.toScalaComponent(dropDownMoneyAccount.component)
    private val ref6 = Ref.toScalaComponent(dropDownMoneyAccount.component)
    private val ref7 = Ref.toScalaComponent(TextInput.component)
    private val ref8 = Ref.toScalaComponent(SimpleCheckbox.component)
    private val ref9 = Ref.toScalaComponent(ModalButtons.component)

    def shiftFocus(ref: Ref.WithScalaComponent[_, _, _ <: HasFocus, CtorType.Props]): Callback =
      ref.foreachCB(_.backend.focus).async.delayMs(50).toCallback

    def render(props: Props): VdomTagOf[Form] = {
      val refToLast = (if (props.id.isEmpty) ref8 else ref9)
        .asInstanceOf[Ref.WithScalaComponent[Any, Any, _ <: HasFocus, CtorType.Props]]
      val refToNext = (if (props.transactionType == TransactionType.Transfer) ref6 else refToLast)
        .asInstanceOf[Ref.WithScalaComponent[Any, Any, _ <: HasFocus, CtorType.Props]]

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
              shiftFocus(ref1)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownTT.component.withRef(ref1)(
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
              shiftFocus(ref2)
            )
          )
        ),
        <.div(^.cls := "row",
          TextInput.component.withRef(ref2)(TextInput.Props(
            "add-tr-amount", "Amount", props.amount.toString(), props.amountChange, 403, List("col", "s12"), shiftFocus(ref3)
          ))
        ),
        <.div(^.cls := "row",
          TextInput.component.withRef(ref3)(TextInput.Props(
            "add-tr-description", "Description", props.description, props.descriptionChange, 404, List("col", "s12"), shiftFocus(ref4)
          ))
        ),
        <.div(^.cls := "row",
          dropDownCategoryTree.component.withRef(ref4)(
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
              shiftFocus(ref5)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component.withRef(ref5)(
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
              shiftFocus(refToNext)
            )
          )
        ),
        <.div(^.cls := "row",
          dropDownMoneyAccount.component.withRef(ref6)(
            dropDownMoneyAccount.Props(
              "add-tr-ma-dest",
              "Destination Money Account",
              props.moneyAccounts.values.toList,
              _.name,
              ma => s"k-ma-${ma.id}",
              props.destinationMAChange,
              props.destMAId.flatMap(id => props.moneyAccounts.get(id)),
              407,
              List("col", "s12"),
              shiftFocus(ref7)
            )
          )
        ).when(props.transactionType == TransactionType.Transfer),
        <.div(^.cls := "row",
          TextInput.component.withRef(ref7)(
            TextInput.Props(
              "add-tr-amount-dest",
              "Destination Amount",
              props.destAmount.map(_.toString()).getOrElse(""),
              props.destinationAmountChange,
              408,
              List("col", "s12"),
              shiftFocus(refToLast)
            )
          )
        ).when(props.transactionType == TransactionType.Transfer),
        <.div(^.cls := "row",
          SimpleCheckbox.component.withRef(ref8)(SimpleCheckbox.Props("Add another", props.addNext, 409, props.addNextChange))
        ).when(props.id.isEmpty),
        ModalButtons.component.withRef(ref9)(
          ModalButtons.Props(props.id.map(_ => "Save").getOrElse("Add"), 410, props.save, props.close)
        )
      )
    }
  }

  val component: Scala.Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
