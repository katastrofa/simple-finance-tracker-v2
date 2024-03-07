package org.big.pete.sft.front.components.main.transactions

import japgolly.scalajs.react.component.Scala.{BackendScope, Component}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount, StateSnapshot}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Ref, Reusability, ScalaComponent}
import org.big.pete.react.{MICheckbox, MaterialIcon}
import org.big.pete.sft.domain.{Category, Currency, EnhancedAccount, TransactionTracking, TransactionType}
import org.big.pete.sft.front.components.main.{formatAmount, tableWrap}
import org.big.pete.sft.front.domain.{CategoryTree, EnhancedTransaction, Order, SortingColumn, TransactionEntry}
import org.big.pete.sft.front.helpers.PieChart
import org.big.pete.sft.front.helpers.{FormModal, ModalButtons}
import org.big.pete.sft.front.state.{AddTransactionSetup, CookieStorage}
import org.big.pete.sft.front.utilz.{LegendOptions, TitleOptions}
import org.scalajs.dom.html.Element
import org.scalajs.dom.window

import java.time.LocalDate


object Page {
  import org.big.pete.react.Implicits._
  import org.big.pete.sft.front.domain.Implicits._

  case class Props(
      account: String,
      items: List[TransactionEntry],
      linearCats: List[CategoryTree],
      categories: Map[Int, Category],
      moneyAccounts: Map[Int, EnhancedAccount],
      ordering: List[(SortingColumn, Order)],
      clickOrdering: SortingColumn => Callback,
      checkTransaction: (MICheckbox.Status, EnhancedTransaction) => Callback,
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
      category: Option[CategoryTree],
      moneyAccount: Option[EnhancedAccount],
      destMA: Option[EnhancedAccount],
      currency: Option[Currency],
      destCurrency: Option[Currency],
      addNext: Boolean,
      toDelete: Set[Int],
      massEditCat: Option[Int],
      massEditMA: Option[Int]
  )


  case class ConfirmProps(id: Int, deleteTransaction: Callback, close: Callback)

  implicit val confirmPropsReuse: Reusability[ConfirmProps] =
    Reusability.caseClassExcept[ConfirmProps]("deleteTransaction", "close")


  class Backend($: BackendScope[Props, State]) extends Utilz with OnUnmount {

    private val formRef = Ref.toScalaComponent(EditForm.component)


    private def ssChange[T](update: (T, State) => State)(opt: Option[T], fn: Callback): Callback = opt.map { value =>
      $.modState(state => update(value, state))
    }.getOrElse(Callback.empty) >> fn

    private def dateChange(dateOpt: Option[LocalDate], fn: Callback): Callback =
      ssChange[LocalDate]((date, state) => state.copy(date = date))(dateOpt, fn)

    private def ttChange(ttOpt: Option[TransactionType], fn: Callback): Callback =
      ssChange[TransactionType]((tt, state) => state.copy(transactionType = tt))(ttOpt, fn)

    private def amountChange(amountOpt: Option[BigDecimal], fn: Callback): Callback =
      ssChange[BigDecimal]((amount, state) => state.copy(amount = amount))(amountOpt, fn)

    private def descriptionChange(description: Option[String], fn: Callback): Callback =
      ssChange[String]((desc, state) => state.copy(description = desc))(description, fn)

    private def categoryChange(cat: Option[CategoryTree], fn: Callback): Callback =
      ssChange[CategoryTree]((cat, state) => state.copy(category = Some(cat)))(cat, fn)

    private def accountChange(ma: Option[EnhancedAccount], fn: Callback): Callback =
      ssChange[EnhancedAccount]((ma, state) => {
        val newCurrency = state.currency
          .flatMap(cur => ma.currencies.find(_.currency.id == cur.id))
          .map(_.currency)
        state.copy(moneyAccount = Some(ma), currency = newCurrency)
      })(ma, fn)

    private def currencyChange(currency: Currency): Callback =
      $.modState(_.copy(currency = Some(currency.id)))

    private def destinationMAChange(ma: EnhancedAccount): Callback = $.modState { state =>
      val newDestCurrency = state.destCurrency
        .flatMap(cur => ma.currencies.find(_.currency.id == cur))
        .map(_.currency.id)
      state.copy(destMAId = Some(ma.id), destCurrency = newDestCurrency)
    }

    private def destinationCurrencyChange(currency: Currency): Callback =
      $.modState(_.copy(destCurrency = Some(currency.id)))

    private def destinationAmountChange(amountOpt: Option[BigDecimal], fn: Callback): Callback =
      ssChange[BigDecimal]((destAmount, state) => state.copy(destAmount = Some(destAmount)))(amountOpt, fn)

    private def addNextChange(event: ReactFormEventFromInput): Callback = $.modState { state =>
      state.copy(addNext = event.target.checked)
    }

    def save: Callback = for {
      props <- $.props
      state <- $.state
      destAmount = if (state.transactionType == TransactionType.Transfer) state.destAmount else None
      destMA = if (state.transactionType == TransactionType.Transfer) state.destMAId else None
      destCurrency = if (state.transactionType == TransactionType.Transfer) state.destCurrency else None
      _ = CookieStorage.updateAddTransactionSetup(props.account, AddTransactionSetup(
        state.date, state.transactionType, state.category.map(_.id), state.moneyAccountId, state.currency,
        state.destMAId, state.destCurrency
      ))
      _ <- props.save(
        state.id, state.date, state.transactionType, state.amount, state.description,
        state.category.map(_.id).get, state.moneyAccountId.get, state.currency.get, destAmount, destMA, destCurrency
      )
      _ <- if (state.addNext) openModalAddNew else close
    } yield ()

    def close: Callback =
      $.modState(_.copy(isOpen = false))

    private def closeDelete: Callback =
      $.modState(_.copy(deleteIsOpen = false))

    private def closeMassEdit: Callback =
      $.modState(_.copy(massEditIsOpen = false))


    private def massEditCatChange(cat: CategoryTree): Callback = $.modState { state =>
      state.copy(massEditCat = Some(cat.id))
    }

    private def massEditMAChange(ma: EnhancedAccount): Callback = $.modState { state =>
      state.copy(massEditMA = Some(ma.id))
    }

    private def toggleDetails(transaction: EnhancedTransaction)(detailsOpt: Option[Boolean], fn: Callback): Callback = {
      detailsOpt.map { details =>
        $.modState { state =>
          val newVisibleDetails = if (details) state.visibleDetails + transaction.id else state.visibleDetails - transaction.id
          state.copy(visibleDetails = newVisibleDetails)
        }
      }.getOrElse(Callback.empty) >> fn
    }


    private def openModalAddNew: Callback = $.props.flatMap { props =>
      $.modState { state =>
        val setup = CookieStorage.getAddTransactionSetup(props.account)
        state.copy(
          isOpen = true, id = None, amount = BigDecimal(0), destAmount = Some(BigDecimal(0)), description = "",
          date = setup.date, transactionType = setup.transactionType,
          category = setup.categoryId.flatMap(id => props.linearCats.find(_.id == id)),
          moneyAccountId = setup.accountId, destMAId = setup.destAccountId, currency = setup.currency,
          destCurrency = setup.destCurrency
        )
      } >> formRef.foreachCB(_.backend.focus)
    }

    def openEditModal(trans: EnhancedTransaction): Callback = $.props.flatMap { props =>
      $.modState { state =>
        state.copy(
          isOpen = true, deleteIsOpen = false, massEditIsOpen = false, visibleDetails = state.visibleDetails,
          Some(trans.id), trans.date, trans.transactionType, trans.amount, trans.destinationAmount, trans.description,
          props.linearCats.find(_.id == trans.categoryId), Some(trans.accountId), trans.destinationAccountId,
          Some(trans.currency.id), trans.destinationCurrency.map(_.id)
        )
      }
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
      checkedItems = props.items
        .filter(_.checked == MICheckbox.Status.checkedStatus)
        .map(_.transaction.id)
        .toSet
      _ <- props.massEditSave(checkedItems, massEditCat, massEditMA)
      _ <- closeMassEdit
    } yield ()

    /**
      *
      * def toggleDetails(id: Int): Callback = $.modState { state =>
      * val newVisibleDetails = if (state.visibleDetails.contains(id)) state.visibleDetails - id else state.visibleDetails + id
      * state.copy(visibleDetails = newVisibleDetails)
      * }
      *
      * */

    def render(props: Props, state: State): VdomTagOf[Element] = {
      def checkedChange(transaction: EnhancedTransaction)(statusOpt: Option[MICheckbox.Status], fn: Callback): Callback =
        statusOpt.map(status => props.checkTransaction(status, transaction)).getOrElse(Callback.empty) >> fn

      val colSpan = calculateColSpan
      val reactTransactions = props.items.map { item =>
        LineItem.withKey(s"t-${item.transaction.id}")(
          item.transaction,
          StateSnapshot.withReuse.prepare(checkedChange(item.transaction)).apply(item.checked),
          StateSnapshot.withReuse.prepare(toggleDetails(item.transaction)).apply(state.visibleDetails.contains(item.transaction.id)),
          props.trackingChanged,
          openEditModal,
          openDeleteModal,
          colSpan
        )
      }.toVdomArray

      def getTransactionsPieChartData: List[PieChart.PieChartData] = {
        val checked = props.items.filter(_.checked == MICheckbox.Status.checkedStatus)
        props.items
          .filter(item => checked.isEmpty || item.checked == MICheckbox.Status.checkedStatus)
          .filterNot(_.transaction.transactionType == TransactionType.Transfer)
          .groupBy(item => (item.transaction.transactionType, item.transaction.currency.id))
          .map { case ((ttype, _), items) =>
            val sum = items.map(_.transaction.amount).sum
            PieChart.PieChartData(
              sum.floatValue,
              s"$ttype - ${formatAmount(items.head.transaction.currency.symbol, sum)}",
              if (ttype == TransactionType.Expense) "#E53935" else "#43A047"
            )
          }.toList
      }

      tableWrap(
        "transactions-table",
        List(
          FormModal.component.withKey("add-transaction-modal-key").apply(FormModal.Props("add-transaction-modal"))(
            EditForm.component.withRef(formRef)(EditForm.Props(
              props.linearCats, props.categories, props.moneyAccounts, state.id,
              StateSnapshot.withReuse.prepare(dateChange).apply(state.date),
              StateSnapshot.withReuse.prepare(ttChange).apply(state.transactionType),
              StateSnapshot.withReuse.prepare(amountChange).apply(state.amount),
              StateSnapshot.withReuse.prepare(destinationAmountChange).apply(state.destAmount.getOrElse(BigDecimal(0))),
              StateSnapshot.withReuse.prepare(descriptionChange).apply(state.description),
              StateSnapshot.withReuse.prepare(categoryChange).apply(state.categoryId.flatMap(id => props.categories.get(id))),
              state.categoryId,
              state.moneyAccountId, state.destMAId, state.currency, state.destCurrency, state.addNext,
              dateChange, ttChange, amountChange, descriptionChange, categoryChange, maChange, currencyChange,
              destinationMAChange, destinationAmountChange, destinationCurrencyChange, addNextChange,
              save, close
            ))
          ).when(state.isOpen),

          FormModal.component.withKey("delete-transaction-modal-key").apply(FormModal.Props("delete-transaction-modal")) {
            ModalButtons("Delete", 450, deleteTransaction(), closeDelete)
          }.when(state.deleteIsOpen),

          FormModal.component.withKey("mass-edit-transaction-modal-key").apply(FormModal.Props("mass-edit-transactions-modal")) {
            MassEditModal.component(MassEditModal.Props(
              props.transactions.filter(t => props.checkedTransactions.contains(t.id)),
              props.linearCats, props.moneyAccounts, state.massEditCat, state.massEditMA,
              massEditCatChange, massEditMAChange, saveMassEdit, closeMassEdit
            ))
          }.when(state.massEditIsOpen),

          <.div(^.cls := "row summary", ^.key := "money-summary-key",
            <.div(^.cls := "col s12",
              PieChart.component(PieChart.Props(
                getTransactionsPieChartData,
                Some("#777777"),
                Some("Amount"),
                Some(LegendOptions(display = true, Some(15), useCustomLabels = false, Some(14))),
                Some(TitleOptions(Some(true), Some("Transactions Summary"), Some("#EEEEEE"), Some(18))),
                Some("35%"),
                useHalf = true,
                aspectRatio = Some(2),
                classes = Set("transactions-summary")
              ))
            )
          )
//          <.div(^.cls := "row summary", ^.key := "money-summary-key",
//            <.div(^.cls := "col xl3 l4 m5 s6",
//              <.h6("Income"),
//              <.p(^.cls := "green-text text-darken-1",
//                getTransactionsSum(TransactionType.Income).map(a => <.span(formatAmount(a._1, a._2))).toTagMod
//              )
//            ),
//            <.div(
//              ^.cls := "col xl3 l4 m5 s6",
//              <.h6("Expenses"),
//              <.p(
//                ^.cls := "red-text text-darken-1",
//                getTransactionsSum(TransactionType.Expense).map(a => <.span(formatAmount(a._1, a._2))).toTagMod
//              )
//            )
//          )
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
