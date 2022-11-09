package org.big.pete.sft.front.components.main

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.BackendScope
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.vdom.html_<^
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Ref, Reusability, ScalaComponent, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.{HasFocus, MICheckbox, MaterialIcon, TextInput, WithFocus}
import org.big.pete.sft.domain.{EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.{dropDownCategoryTree, dropDownMoneyAccount, dropDownTT}
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction, Order, SortingColumn}
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons, SimpleCheckbox}
import org.big.pete.sft.front.state.{AddTransactionSetup, CheckAllId, CookieStorage}
import org.scalajs.dom.console
import org.scalajs.dom.html.{Element, Form}

import java.time.LocalDate


object Transactions {
  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  final val trackingToIcon = Map[TransactionTracking, String](
    TransactionTracking.None -> "horizontal_rule",
    TransactionTracking.Auto -> "blur_circular",
    TransactionTracking.Verified -> "check_circle"
  )

  case class Props(
      transactions: List[EnhancedTransaction],
      linearCats: List[CategoryTree],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      checkedTransactions: Set[Int],
      ordering: List[(SortingColumn, Order)],
      clickOrdering: SortingColumn => Callback,
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback,
      save: (Option[Int], LocalDate, TransactionType, BigDecimal, String, Int, Int, Option[BigDecimal], Option[Int]) => Callback,
      deleteTransaction: Int => Callback
  )
  case class State(
      isOpen: Boolean,
      deleteIsOpen: Boolean,
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
      toDelete: Option[Int] = None
  )
  case class HeaderProps(
      transactions: List[EnhancedTransaction],
      checkedTransactions: Set[Int],
      ordering: List[(SortingColumn, Order)],
      checkTransaction: (MICheckbox.Status, String) => Callback,
      clickOrdering: SortingColumn => Callback
  )
  case class TransactionProps(
      transaction: EnhancedTransaction,
      checkedTransactions: Set[Int],
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback,
      openEditModal: EnhancedTransaction => Callback,
      openDeleteModal: Int => Callback
  )

  case class FormProps(
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
  case class ConfirmProps(id: Int, deleteTransaction: Callback, close: Callback)


  implicit val moneyAccountMapReuse: Reusability[Map[Int, EnhancedMoneyAccount]] = Reusability.map[Int, EnhancedMoneyAccount]
  implicit val formPropsReuse: Reusability[FormProps] = Reusability.caseClassExcept[FormProps](
    "dateChange", "ttChange", "amountChange", "descriptionChange", "categoryChange", "maChange",
    "destinationMAChange", "destinationAmountChange", "addNextChange", "save", "close"
  )
  implicit val confirmPropsReuse: Reusability[ConfirmProps] =
    Reusability.caseClassExcept[ConfirmProps]("deleteTransaction", "close")


  class Backend($: BackendScope[Props, State]) {

    private val formRef = Ref.toScalaComponent(formComponent)

    def dateChange(date: LocalDate): CallbackTo[LocalDate] = $.modState { state =>
      state.copy(date = date)
    } >> CallbackTo.pure(date)

    def ttChange(tt: TransactionType): Callback =
      $.modState(_.copy(transactionType = tt))

    def amountChange(event: ReactFormEventFromInput): Callback = $.modState { state =>
      state.copy(amount = parseAmount(event.target.value, state.amount))
    }

    def descriptionChange(event: ReactFormEventFromInput): Callback =
      $.modState(_.copy(description = event.target.value))

    def categoryChange(cat: CategoryTree): Callback =
      $.modState(_.copy(categoryId = Some(cat.id)))

    def maChange(ma: EnhancedMoneyAccount): Callback =
      $.modState(_.copy(moneyAccountId = Some(ma.id)))

    def destinationMAChange(ma: EnhancedMoneyAccount): Callback =
      $.modState(_.copy(destMAId = Some(ma.id)))

    def destinationAmountChange(event: ReactFormEventFromInput): Callback = $.modState { state =>
      state.copy(destAmount = Some(parseAmount(event.target.value, state.destAmount.getOrElse(BigDecimal(0)))))
    }

    def addNextChange(event: ReactFormEventFromInput): Callback = $.modState { state =>
      state.copy(addNext = event.target.checked)
    }

    def save: Callback = for {
      props <- $.props
      state <- $.state
      destAmount = if (state.transactionType == TransactionType.Transfer) state.destAmount else None
      destMA = if (state.transactionType == TransactionType.Transfer) state.destMAId else None
      _ = CookieStorage.updateAddTransactionSetup(
        AddTransactionSetup(state.date, state.transactionType, state.categoryId, state.moneyAccountId, state.destMAId)
      )
      _ <- props.save(state.id, state.date, state.transactionType, state.amount, state.description, state.categoryId.get, state.moneyAccountId.get, destAmount, destMA)
      _ <- if (state.addNext) openModalAddNew else close
    } yield ()

    def close: Callback =
      $.modState(_.copy(isOpen = false))

    def closeDelete: Callback =
      $.modState(_.copy(deleteIsOpen = false))

    def openModalAddNew: Callback = $.modState { state =>
      val setup = CookieStorage.getAddTransactionSetup
      state.copy(
        isOpen = true, id = None, amount = BigDecimal(0), destAmount = Some(BigDecimal(0)), description = "",
        date = setup.date, transactionType = setup.transactionType, categoryId = setup.categoryId,
        moneyAccountId = setup.moneyAccountId, destMAId = setup.destMAId
      )
    } >> formRef.foreachCB(_.backend.focus)

    def openEditModal(trans: EnhancedTransaction): Callback = $.modState { state =>
      state.copy(
        isOpen = true,
        deleteIsOpen = false,
        Some(trans.id),
        trans.date,
        trans.transactionType,
        trans.amount,
        trans.destinationAmount,
        trans.description,
        Some(trans.categoryId),
        Some(trans.moneyAccountId),
        trans.destinationMoneyAccountId
      )
    } >> formRef.foreachCB(_.backend.focus)

    def openDeleteModal(id: Int): Callback = $.modState { state =>
      state.copy(deleteIsOpen = true, toDelete = Some(id))
    }

    def deleteTransaction(): Callback = for {
      props <- $.props
      state <- $.state
      _ <- props.deleteTransaction(state.toDelete.get)
      _ <- closeDelete
    } yield ()

    def render(props: Props, state: State): html_<^.VdomTagOf[Element] = {
      val reactTransactions = props.transactions.map { transaction =>
        transactionComponent.withKey(s"t-${transaction.id}")
          .apply(TransactionProps(transaction, props.checkedTransactions, props.checkTransaction, props.trackingChanged, openEditModal, openDeleteModal))
      }.toVdomArray

      tableWrap(
        List(
          AddModal.component(AddModal.Props("add-transaction-modal", state.isOpen))(
            formComponent.withRef(formRef)(FormProps(
              props.linearCats,
              props.moneyAccounts,
              state.id,
              state.date,
              state.transactionType,
              state.amount,
              state.destAmount,
              state.description,
              state.categoryId,
              state.moneyAccountId,
              state.destMAId,
              state.addNext,
              dateChange,
              ttChange,
              amountChange,
              descriptionChange,
              categoryChange,
              maChange,
              destinationMAChange,
              destinationAmountChange,
              addNextChange,
              save,
              close
            ))
          ),
          AddModal.component(AddModal.Props("delete-transaction-modal", state.deleteIsOpen)) {
            ModalButtons("Delete", 450, deleteTransaction(), closeDelete)
          }
        ).toTagMod,
        headerComponent(HeaderProps(props.transactions, props.checkedTransactions, props.ordering, props.checkTransaction, props.clickOrdering)),
        reactTransactions,
        headerComponent(HeaderProps(props.transactions, props.checkedTransactions, props.ordering, props.checkTransaction, props.clickOrdering)),
        <.a(
          ^.cls := "waves-effect waves-light btn nice",
          ^.onClick --> openModalAddNew,
          MaterialIcon("add"),
          "Add"
        )
      )
    }
  }

  val component: Scala.Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[State](State(isOpen = false, deleteIsOpen = false, None, LocalDate.now(), TransactionType.Expense, BigDecimal(0), None, "", None, None, None, addNext = false))
    .renderBackend[Backend]
    .build

  val headerComponent: Component[HeaderProps, CtorType.Props] = ScalaFnComponent.apply[HeaderProps] { props =>
    def orderingIcon(column: SortingColumn): String = {
      props.ordering.find(_._1 == column) match {
        case Some((_, Order.Asc)) => "arrow_drop_up"
        case Some((_, Order.Desc)) => "arrow_drop_down"
        case None => "sort"
      }
    }

    val checkedStatus =
      if (props.transactions.map(_.id).toSet == props.checkedTransactions)
        MICheckbox.Status.checkedStatus
      else if(props.checkedTransactions.isEmpty)
        MICheckbox.Status.none
      else
        MICheckbox.Status.indeterminate

    <.tr(
      MICheckbox.component(MICheckbox.Props(
        <.th(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        CheckAllId,
        "",
        checkedStatus,
        props.checkTransaction
      )),
      <.th(^.cls := "date", "Date",
        MaterialIcon(MaterialIcon.i, MaterialIcon.small, orderingIcon(SortingColumn.Date), props.clickOrdering(SortingColumn.Date))
      ),
      <.th(^.cls := "description", "Description",
        MaterialIcon(MaterialIcon.i, MaterialIcon.small, orderingIcon(SortingColumn.Description), props.clickOrdering(SortingColumn.Description))
      ),
      <.th(^.cls := "amount right-align", "Amount",
        MaterialIcon(MaterialIcon.i, MaterialIcon.small, orderingIcon(SortingColumn.Amount), props.clickOrdering(SortingColumn.Amount))
      ),
      <.th(^.cls := "category", "Category"),
      <.th(^.cls := "money-account", "Account"),
      <.th(^.cls := "delete", ""),
      <.th(^.cls := "status center-align hide-on-med-and-down", "")
    )
  }

  val transactionComponent: Component[TransactionProps, CtorType.Props] = ScalaFnComponent.apply[TransactionProps] { props =>
    val amountClass = props.transaction.transactionType match {
      case TransactionType.Income => "green-text text-darken-1"
      case TransactionType.Expense => "red-text text-darken-1"
      case TransactionType.Transfer => "amber-text text-darken-2"
    }
    val additionalAmountInfo = if (props.transaction.destinationAmount.isDefined)
      " -> " + formatAmount(props.transaction.destinationCurrencySymbol.get, props.transaction.destinationAmount.get)
    else ""

    <.tr(
      MICheckbox.component(MICheckbox.Props(
        <.td(_: _*),
        Map("check" -> true, "hide-on-med-and-down" -> true, "center-align" -> true),
        props.transaction.id.toString,
        "",
        if (props.checkedTransactions.contains(props.transaction.id)) MICheckbox.Status.checkedStatus else MICheckbox.Status.none,
        props.checkTransaction
      )),
      <.td(^.cls := "date", props.transaction.date.format(DateFormat)),
      <.td(
        ^.cls := "description pointer",
        ^.onClick --> props.openEditModal(props.transaction),
        props.transaction.description
      ),
      <.td(^.cls := s"right-align amount $amountClass",
        formatAmount(props.transaction.currencySymbol, props.transaction.amount) + additionalAmountInfo
      ),
      <.td(^.cls := "category",
        <.span(^.cls := "show-on-large hide-on-med-and-down", props.transaction.categoryFullName),
        <.span(^.cls := "show-on-medium-and-down hide-on-large-only", props.transaction.categoryName)
      ),
      <.td(^.cls := "money-account", props.transaction.moneyAccountName),
      <.td(^.cls := "delete",
        MaterialIcon(MaterialIcon.i, MaterialIcon.small, "delete", props.openDeleteModal(props.transaction.id))
      ),
      <.td(^.cls := "status center-align hide-on-med-and-down",
        MaterialIcon(
          MaterialIcon.`i`,
          MaterialIcon.`small`,
          trackingToIcon(props.transaction.tracking),
          props.trackingChanged(props.transaction.id, props.transaction.tracking)
        )
      )
    )
  }

  class FormBackend
    extends WithFocus[ReactDatePicker.Props, ReactDatePicker.State, ReactDatePicker.Backend]
  {
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
      Callback.log("shiftFocus") >> ref.foreachCB { comp =>
        console.log("inside")
        comp.backend.focus
      }.async.delayMs(50).toCallback

    def render(props: FormProps): VdomTagOf[Form] = {
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
          TextInput.component.withRef(ref2)(
            TextInput.Props("add-tr-amount", "Amount", props.amount.toString(), props.amountChange, 403, List("col", "s12"), shiftFocus(ref3))
          )
        ),
        <.div(^.cls := "row",
          TextInput.component.withRef(ref3)(
            TextInput.Props("add-tr-description", "Description", props.description, props.descriptionChange, 404, List("col", "s12"), shiftFocus(ref4))
          )
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

  val formComponent: Scala.Component[FormProps, Unit, FormBackend, CtorType.Props] = ScalaComponent.builder[FormProps]
    .stateless
    .renderBackend[FormBackend]
    .configure(Reusability.shouldComponentUpdate)
    .build

}
