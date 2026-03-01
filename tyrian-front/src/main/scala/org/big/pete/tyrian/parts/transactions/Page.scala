package org.big.pete.tyrian.parts.transactions

import cats.effect.IO
import org.big.pete.sft.domain.{Account, Category, Currency, Op, Transaction}
import org.big.pete.tyrian.component.{DatePicker, DropDown, DropDownItem, ICheckbox}
import org.big.pete.tyrian.component.inputs.{MoneyTextBox, TextInput}
import org.big.pete.tyrian.component.ICheckbox.Status
import org.big.pete.tyrian.domain.SortingOrder
import org.big.pete.tyrian.domain.{Date, Description, SortingColumn, SortingName}
import org.big.pete.tyrian.domain.Givens.{CatDropDownItem, given}
import org.big.pete.tyrian.toolz.{CookieStorage, Views}
import org.big.pete.tyrian.toolz.Views.MBIcon
import org.scalajs.dom.{document, window}
import tyrian.{Cmd, Sub, Html as <}
import tyrian.syntax.*

import scala.concurrent.duration.DurationInt


final case class Model(
    isModalOpen: Boolean,
    isMassEditOpen: Boolean,
    isMassDeleteOpen: Boolean,

    editing: Option[Transaction],
    editDate: DatePicker.Model,
    editOp: DropDown.Model[Op],
    editAmount: MoneyTextBox.Model,
    editDescription: TextInput.Model,
    editCategory: DropDown.Model[Category],
    editAccount: DropDown.Model[Account],
    editCurrency: DropDown.Model[Currency],
    editDestAccount: DropDown.Model[Account],
    editDestCurrency: DropDown.Model[Currency],
    editDestAmount: MoneyTextBox.Model,
    editAddAnother: ICheckbox.Model,

    ordering: List[SortingColumn[?]],
    details: Set[Int],

    wallet: String,
    colSpan: Int,
    debouncing: Option[Int]
)
enum Msg {
  case OpenModal
  case OpenModalMassEdit
  case OpenModalMassDelete

  case EditDate(msg: DatePicker.Msg)
  case EditOp(msg: DropDown.Msg[Op])
  case EditAmount(msg: MoneyTextBox.Msg)
  case EditDescription(msg: TextInput.Msg)
  case EditCategory(msg: DropDown.Msg[Category])
  case EditAccount(msg: DropDown.Msg[Account])
  case EditCurrency(msg: DropDown.Msg[Currency])
  case EditDestAccount(msg: DropDown.Msg[Account])
  case EditDestCurrency(msg: DropDown.Msg[Currency])
  case EditDestAmount(msg: MoneyTextBox.Msg)
  case EditAddAnother(msg: ICheckbox.Msg)
  case EditModalConfirm
  case EditModalCancel

  case TransactionCheckbox(id: Int, msg: ICheckbox.Msg)
  case TransactionDetails(id: Int)
  case TransactionEdit(t: Transaction)
  case TransactionDelete(t: Transaction)
  case TransactionStatus(t: Transaction)

  case MainCheckboxClick(msg: ICheckbox.Msg)
  case SortingClick(column: SortingName)

  case TimePassed
  case RecalcSpan
}

object Page {

  final private val DebouncingMillis: Int = 600
  final private val TickInterval: Int = 200

  def init(
      wallet: String,
      transactions: List[Transaction],
      categories: Map[Int, Category],
      accounts: Map[Int, Account],
      currencies: Map[String, Currency]
  ): Model = {
    given cats: Map[Int, Category] = categories
    given catDropDownSupport(using categories: Map[Int, Category]): DropDownItem[Category] = new CatDropDownItem

    val catsList = categories.values.toList
    val accs = accounts.values.toList
    val currs = currencies.values.toList

    Model(
      isModalOpen = false,
      isMassEditOpen = false,
      isMassDeleteOpen = false,

      editing = None,
      editDate = DatePicker.init(None),
      editOp = DropDown.init[Op]("tx-edit-op", Op.values.toList, None),
      editAmount = MoneyTextBox.init(0, false),
      editDescription = TextInput.init(""),
      editCategory = DropDown.init[Category]("tx-edit-category", catsList, None),
      editAccount = DropDown.init[Account]("tx-edit-account", accs, None),
      editCurrency = DropDown.init[Currency]("tx-edit-currency", currs, None),
      editDestAccount = DropDown.init[Account]("tx-edit-dest-account", accs, None),
      editDestCurrency = DropDown.init[Currency]("tx-edit-dest-currency", currs, None),
      editDestAmount = MoneyTextBox.init(0, false),
      editAddAnother = ICheckbox.init(ICheckbox.Status.None),

      ordering = List(Date(SortingOrder.Desc), Description(SortingOrder.Asc)),
      details = Set.empty,

      wallet = wallet,
      colSpan = 12,
      debouncing = None
    )
  }

  def update(msg: Msg, m: Model)
    (using categories: Map[Int, Category], accounts: Map[Int, Account], currencies: Map[String, Currency]): (Model, Cmd[IO, Msg]) =
  {
    msg match {
      case Msg.OpenModal =>
        given catDropDownSupport(using categories: Map[Int, Category]): DropDownItem[Category] = new CatDropDownItem
        openAddModal(m) -> Cmd.None
      case Msg.OpenModalMassEdit =>
        m.copy(isMassEditOpen = true) -> Cmd.None
      case Msg.OpenModalMassDelete =>
        m.copy(isMassDeleteOpen = true) -> Cmd.None

      case Msg.SortingClick(column) =>
        handleSorting(m, column) -> Cmd.None
      case Msg.TransactionDetails(txId) =>
        val newDetails = if(m.details.contains(txId)) m.details - txId else m.details + txId
        m.copy(details = newDetails) -> Cmd.None
      case Msg.TransactionStatus(_) | Msg.TransactionCheckbox(_, _) | Msg.TransactionDelete(_) | Msg.MainCheckboxClick(_) =>
        m -> Cmd.None

      case Msg.TransactionEdit(tx) =>
        openEditModal(m, tx) -> Cmd.None
      case Msg.EditDate(msg) =>
        m.copy(editDate = DatePicker.update(msg, m.editDate)) -> Cmd.None
      case Msg.EditOp(msg) =>
        val ddUpdate = DropDown.update(msg, m.editOp)
        m.copy(editOp = ddUpdate._1) -> ddUpdate._2.map(Msg.EditOp(_))


      case Msg.TimePassed =>
        handleTick(m) -> Cmd.None
      case Msg.RecalcSpan =>
        m.copy(colSpan = calculateColSpan) -> Cmd.None

      case Msg.EditAmount(_) => ???
      case Msg.EditDescription(_) => ???
      case Msg.EditCategory(_) => ???
      case Msg.EditAccount(_) => ???
      case Msg.EditCurrency(_) => ???
      case Msg.EditDestAccount(_) => ???
      case Msg.EditDestCurrency(_) => ???
      case Msg.EditDestAmount(_) => ???
      case Msg.EditAddAnother(_) => ???
      case Msg.EditModalConfirm => ???
      case Msg.EditModalCancel => ???
    }
  }

  def view(m: Model, transactions: List[Transaction], checkboxes: Map[Int, ICheckbox.Model], headerCheckbox: ICheckbox.Model)
    (using currencies: Map[String, Currency], accounts: Map[Int, Account], categories: Map[Int, Category]): <[Msg] =
  {
    Views.tableWrap(
      "transactions-table",
      <.div(
        EditModal.view(m)
      ),
      Header.viewHeader(m,headerCheckbox),
      viewTransactions(m, transactions, checkboxes),
      Header.viewHeader(m, headerCheckbox),
      viewButtons(m, checkboxes)
    )
  }



  private def viewTransactions(m: Model, transactions: List[Transaction], checkboxes: Map[Int, ICheckbox.Model])
    (using currencies: Map[String, Currency], accounts: Map[Int, Account], categories: Map[Int, Category]): <[Msg] =
  {
    <.tbody(
      transactions.flatMap { t =>
        List(
          Item.view(m, t, checkboxes),
          m.details.find(_ == t.id).map(_ => Item.viewDetails(m, t)).orEmpty
        )
      }
    )
  }

  private def viewButtons(m: Model, checkboxes: Map[Int, ICheckbox.Model]): <[Msg] = {
    val checkedOpt = checkboxes.values.find(_.status != Status.None)

    <.div(
      Views.niceButton("Add", 23, Msg.OpenModal, Some(MBIcon.Other("add"))),
      checkedOpt.map { _ =>
        Views.niceButton("Edit Selected", 24, Msg.OpenModalMassEdit, Some(MBIcon.Other("edit")))
      }.orEmpty,
      checkedOpt.map { _ =>
        Views.niceButton("Delete Selected", 25, Msg.OpenModalMassDelete, Some(MBIcon.Other("delete")))
      }.orEmpty
    )
  }


  private def openAddModal(m: Model)
    (using categories: Map[Int, Category], accounts: Map[Int, Account], currencies: Map[String, Currency]): Model =
  {
    val storedSettings = CookieStorage.getAddTransactionSetup(m.wallet)
    given catDropDownSupport(using categories: Map[Int, Category]): DropDownItem[Category] = new CatDropDownItem
    /// TODO: verify existence of stored things
    m.copy(
      isModalOpen = true,
      editDate = m.editDate.copy(selected = storedSettings.date),
      editOp = m.editOp.copy(selected = Some(storedSettings.operation)),
      editCategory = m.editCategory.copy(selected = storedSettings.category.map(_.toInt).flatMap(categories.get)),
      editAccount = m.editAccount.copy(selected = storedSettings.account.map(_.toInt).flatMap(accounts.get)),
      editCurrency = m.editCurrency.copy(selected = storedSettings.currency.flatMap(currencies.get)),
      editDestAccount = m.editDestAccount.copy(selected = storedSettings.destAccount.map(_.toInt).flatMap(accounts.get)),
      editDestCurrency = m.editDestCurrency.copy(selected = storedSettings.destCurrency.flatMap(currencies.get))
    )
  }

  private def openEditModal(m: Model, tx: Transaction)
    (using categories: Map[Int, Category], accounts: Map[Int, Account], currencies: Map[String, Currency]): Model =
  {
    given catDropDownSupport(using categories: Map[Int, Category]): DropDownItem[Category] = new CatDropDownItem

    m.copy(
      isModalOpen = true,
      editing = Some(tx),
      editDate = m.editDate.copy(selected = tx.date),
      editOp = m.editOp.copy(selected = Some(tx.op)),
      editAmount = MoneyTextBox.init(tx.amount, false),
      editDescription = TextInput.init(tx.description),
      editCategory = m.editCategory.copy(selected = categories.get(tx.category)),
      editAccount = m.editAccount.copy(selected = accounts.get(tx.account)),
      editCurrency = m.editCurrency.copy(selected = currencies.get(tx.currency)),
      editDestAccount = m.editDestAccount.copy(selected = tx.destinationAccount.flatMap(id => accounts.get(id))),
      editDestCurrency = m.editDestCurrency.copy(selected = tx.destinationCurrency.flatMap(curr => currencies.get(curr))),
      editDestAmount = MoneyTextBox.init(tx.destinationAmount.getOrElse(0), false)
    )
  }

  private def handleSorting(m: Model, name: SortingName): Model = {
    val newOrdering = m.ordering.find(_.name == name) match {
      case Some(col) if col.order == SortingOrder.Asc =>
        SortingColumn(name, SortingOrder.Desc) :: m.ordering.filter(_.name != name)
      case Some(col) =>
        m.ordering.filter(_.name != name)
      case None =>
        SortingColumn(name, SortingOrder.Desc) :: m.ordering
    }

    m.copy(ordering = newOrdering)
  }

  private def handleTick(m: Model): Model = {
    m.debouncing match {
      case Some(remaining) if remaining <= 0 =>
        m.copy(debouncing = None, colSpan = calculateColSpan)
      case Some(remaining) =>
        m.copy(debouncing = Some(remaining - TickInterval))
      case None =>
        m
    }
  }

  private def calculateColSpan: Int = {
    document.querySelectorAll("#transactions-table tr:first-child td").filterNot { el =>
      window.getComputedStyle(el).display == "none"
    }.length
  }

  private def tick(m: Model): Sub[IO, Msg] = {
    m.debouncing match {
      case Some(_) =>
        Sub.every[IO](TickInterval.millis, "transactions-page")
          .map(_ => Msg.TimePassed)
      case _ =>
        Sub.None
    }
  }

  def subscriptions(model: Model): Sub[IO, Msg] = Sub.Batch[IO, Msg](
    tick(model),
    Sub.fromEvent("resize", window)(_ => Some(Msg.RecalcSpan)),
    Sub.animationFrameTick("transactions-page-load")(_ => Msg.RecalcSpan)
  )
}
