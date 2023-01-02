package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Ref, Reusability, ScalaComponent}
import org.big.pete.react.{MICheckbox, MaterialIcon}
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.components.main.{formatAmount, tableWrap}
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction, Order, SortingColumn}
import org.big.pete.sft.front.helpers.{AddModal, ModalButtons}
import org.big.pete.sft.front.state.{AddTransactionSetup, CookieStorage}
import org.scalajs.dom.html.Element
import org.scalajs.dom.window

import java.time.LocalDate


object Page {
  case class Props(
      transactions: List[EnhancedTransaction],
      linearCats: List[CategoryTree],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      checkedTransactions: Set[Int],
      ordering: List[(SortingColumn, Order)],
      clickOrdering: SortingColumn => Callback,
      checkTransaction: (MICheckbox.Status, String) => Callback,
      trackingChanged: (Int, TransactionTracking) => Callback,
      save: (Option[Int], LocalDate, TransactionType, BigDecimal, String, Int, Int, String, Option[BigDecimal], Option[Int], Option[String]) => Callback,
      deleteTransactions: Set[Int] => Callback,
      massEditSave: (Set[Int], Option[Int], Option[Int]) => Callback
  )

  case class State(
      isOpen: Boolean,
      deleteIsOpen: Boolean,
      massEditIsOpen: Boolean,
      visibleDetails: Set[Int],
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
      toDelete: Set[Int],
      massEditCat: Option[Int],
      massEditMA: Option[Int]
  )


  case class ConfirmProps(id: Int, deleteTransaction: Callback, close: Callback)

  implicit val confirmPropsReuse: Reusability[ConfirmProps] =
    Reusability.caseClassExcept[ConfirmProps]("deleteTransaction", "close")


  class Backend($: BackendScope[Props, State]) extends Utilz with OnUnmount {

    private val formRef = Ref.toScalaComponent(AddForm.component)

    def dateChange(date: LocalDate): CallbackTo[LocalDate] = $.modState { state =>
      state.copy(date = date)
    } >> CallbackTo.pure(date)

    def ttChange(tt: TransactionType): Callback =
      $.modState(_.copy(transactionType = tt))

    def amountChange(amount: BigDecimal): Callback =
      $.modState(_.copy(amount = amount))

    def descriptionChange(event: ReactFormEventFromInput): Callback =
      $.modState(_.copy(description = event.target.value))

    def categoryChange(cat: CategoryTree): Callback =
      $.modState(_.copy(categoryId = Some(cat.id)))

    def maChange(ma: EnhancedMoneyAccount): Callback = $.modState { state =>
      val newCurrency = state.currency
        .flatMap(cur => ma.currencies.find(_.currency.id == cur))
        .map(_.currency.id)
      state.copy(moneyAccountId = Some(ma.id), currency = newCurrency)
    }

    def currencyChange(currency: Currency): Callback =
      $.modState(_.copy(currency = Some(currency.id)))

    def destinationMAChange(ma: EnhancedMoneyAccount): Callback = $.modState { state =>
      val newDestCurrency = state.destCurrency
        .flatMap(cur => ma.currencies.find(_.currency.id == cur))
        .map(_.currency.id)
      state.copy(destMAId = Some(ma.id), destCurrency = newDestCurrency)
    }

    def destinationCurrencyChange(currency: Currency): Callback =
      $.modState(_.copy(destCurrency = Some(currency.id)))

    def destinationAmountChange(amount: BigDecimal): Callback =
      $.modState(_.copy(destAmount = Some(amount)))

    def addNextChange(event: ReactFormEventFromInput): Callback = $.modState { state =>
      state.copy(addNext = event.target.checked)
    }

    def save: Callback = for {
      props <- $.props
        state <- $.state
        destAmount = if (state.transactionType == TransactionType.Transfer) state.destAmount else None
        destMA = if (state.transactionType == TransactionType.Transfer) state.destMAId else None
        destCurrency = if (state.transactionType == TransactionType.Transfer) state.destCurrency else None
        _ = CookieStorage.updateAddTransactionSetup(AddTransactionSetup(
          state.date, state.transactionType, state.categoryId, state.moneyAccountId, state.currency,
          state.destMAId, state.destCurrency
        ))
        _ <- props.save(
          state.id, state.date, state.transactionType, state.amount, state.description,
          state.categoryId.get, state.moneyAccountId.get, state.currency.get, destAmount, destMA, destCurrency
        )
        _ <- if (state.addNext) openModalAddNew else close
    } yield ()

    def close: Callback =
      $.modState(_.copy(isOpen = false))

    private def closeDelete: Callback =
      $.modState(_.copy(deleteIsOpen = false))

    private def closeMassEdit: Callback =
      $.modState(_.copy(massEditIsOpen = false))


    def massEditCatChange(cat: CategoryTree): Callback = $.modState { state =>
      state.copy(massEditCat = Some(cat.id))
    }

    def massEditMAChange(ma: EnhancedMoneyAccount): Callback = $.modState { state =>
      state.copy(massEditMA = Some(ma.id))
    }

    def toggleDetails(id: Int): Callback = $.modState { state =>
      val newVisibleDetails = if (state.visibleDetails.contains(id)) state.visibleDetails - id else state.visibleDetails + id
      state.copy(visibleDetails = newVisibleDetails)
    }


    private def openModalAddNew: Callback = $.modState { state =>
      val setup = CookieStorage.getAddTransactionSetup
      state.copy(
        isOpen = true, id = None, amount = BigDecimal(0), destAmount = Some(BigDecimal(0)), description = "",
        date = setup.date, transactionType = setup.transactionType, categoryId = setup.categoryId,
        moneyAccountId = setup.moneyAccountId, destMAId = setup.destMAId, currency = setup.currency, destCurrency = setup.destCurrency
      )
    } >> formRef.foreachCB(_.backend.focus)

    def openEditModal(trans: EnhancedTransaction): Callback = $.modState { state =>
      state.copy(
        isOpen = true, deleteIsOpen = false, massEditIsOpen = false, visibleDetails = state.visibleDetails,
        Some(trans.id), trans.date, trans.transactionType, trans.amount, trans.destinationAmount, trans.description,
        Some(trans.categoryId), Some(trans.moneyAccountId), trans.destinationMoneyAccountId, Some(trans.currency.id),
        trans.destinationCurrency.map(_.id)
      )
    } >> formRef.foreachCB(_.backend.focus)

    private def openMassEditModal: Callback = $.modState { state =>
      state.copy(isOpen = false, deleteIsOpen = false, massEditIsOpen = true, massEditCat = Some(-1), massEditMA = Some(-1))
    }

    def openDeleteModal(ids: Set[Int]): Callback = $.modState { state =>
      state.copy(deleteIsOpen = true, toDelete = ids)
    }

    private def deleteTransaction(): Callback = for {
      props <- $.props
      state <- $.state
      _ <- props.deleteTransactions(state.toDelete)
      _ <- closeDelete
    } yield ()

    private def saveMassEdit: Callback = for {
      props <- $.props
      state <- $.state
      massEditCat = if (state.massEditCat.exists(_ < 0)) None else state.massEditCat
      massEditMA = if (state.massEditMA.exists(_ < 0)) None else state.massEditMA
      _ <- props.massEditSave(props.checkedTransactions, massEditCat, massEditMA)
      _ <- closeMassEdit
    } yield ()

    def render(props: Props, state: State): VdomTagOf[Element] = {
      val colSpan = calculateColSpan
      val reactTransactions = props.transactions.map { transaction =>
        LineItem.component.withKey(s"t-${transaction.id}").apply(LineItem.Props(
          transaction,
          props.checkedTransactions.contains(transaction.id),
          state.visibleDetails.contains(transaction.id),
          props.checkTransaction,
          props.trackingChanged,
          openEditModal,
          openDeleteModal,
          toggleDetails,
          colSpan
        ))
      }.toVdomArray

      def getTransactionsSum(ttype: TransactionType): List[(String, BigDecimal)] = {
        props.transactions
          .filter(trans => props.checkedTransactions.isEmpty || props.checkedTransactions.contains(trans.id))
          .filter(_.transactionType == ttype)
          .map(trans => trans.currency.symbol -> trans.amount)
          .groupBy(_._1)
          .view.mapValues(_.map(_._2).sum)
          .toList
      }

      tableWrap(
        "transactions-table",
        List(
          AddModal.component.withKey("add-transaction-modal-key").apply(AddModal.Props("add-transaction-modal"))(
            AddForm.component.withRef(formRef)(AddForm.Props(
              props.linearCats, props.moneyAccounts,
              state.id, state.date, state.transactionType, state.amount, state.destAmount, state.description, state.categoryId,
              state.moneyAccountId, state.destMAId, state.currency, state.destCurrency, state.addNext,
              dateChange, ttChange, amountChange, descriptionChange, categoryChange, maChange, currencyChange,
              destinationMAChange, destinationAmountChange, destinationCurrencyChange, addNextChange,
              save, close
            ))
          ).when(state.isOpen),

          AddModal.component.withKey("delete-transaction-modal-key").apply(AddModal.Props("delete-transaction-modal")) {
            ModalButtons("Delete", 450, deleteTransaction(), closeDelete)
          }.when(state.deleteIsOpen),

          AddModal.component.withKey("mass-edit-transaction-modal-key").apply(AddModal.Props("mass-edit-transactions-modal")) {
            MassEditModal.component(MassEditModal.Props(
              props.transactions.filter(t => props.checkedTransactions.contains(t.id)),
              props.linearCats, props.moneyAccounts, state.massEditCat, state.massEditMA,
              massEditCatChange, massEditMAChange, saveMassEdit, closeMassEdit
            ))
          }.when(state.massEditIsOpen),

          <.div(^.cls := "row summary", ^.key := "money-summary-key",
            <.div(^.cls := "col xl3 l4 m5 s6",
              <.h6("Income"),
              <.p(^.cls := "green-text text-darken-1",
                getTransactionsSum(TransactionType.Income).map(a => <.span(formatAmount(a._1, a._2))).toTagMod
              )
            ),
            <.div(
              ^.cls := "col xl3 l4 m5 s6",
              <.h6("Expenses"),
              <.p(
                ^.cls := "red-text text-darken-1",
                getTransactionsSum(TransactionType.Expense).map(a => <.span(formatAmount(a._1, a._2))).toTagMod
              )
            )
          )
        ).toTagMod,
        Header.component(
          Header.Props(props.transactions, props.checkedTransactions, props.ordering, props.checkTransaction, props.clickOrdering)
        ),
        reactTransactions,
        Header.component(
          Header.Props(props.transactions, props.checkedTransactions, props.ordering, props.checkTransaction, props.clickOrdering)
        ),
        List(
          <.a(^.cls := "waves-effect waves-light btn nice", ^.key := "add-button-key",
            ^.onClick --> openModalAddNew,
            MaterialIcon("add"),
            "Add"
          ),
          <.a(^.cls := "waves-effect waves-light btn nice", ^.key := "mass-edit-button-key",
            ^.onClick --> openMassEditModal,
            MaterialIcon("edit"),
            "Edit selected"
          ).when(props.checkedTransactions.nonEmpty),
          <.a(^.cls := "waves-effect waves-light btn nice", ^.key := "mass-delete-button-key",
            ^.onClick --> openDeleteModal(props.checkedTransactions),
            MaterialIcon("delete"),
            "Delete selected"
          ).when(props.checkedTransactions.nonEmpty)
        ).toTagMod
      )
    }
  }

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState[State](State(
      isOpen = false, deleteIsOpen = false, massEditIsOpen = false, Set.empty, None, LocalDate.now(),
      TransactionType.Expense, BigDecimal(0), None, "", None, None, None, None, None, addNext = false, Set.empty, None, None
    ))
    .renderBackend[Backend]
    .configure(EventListener.install("resize", _.backend.controlledInvocationOfUpdateColSpanCB, _ => window))
    .build
}
