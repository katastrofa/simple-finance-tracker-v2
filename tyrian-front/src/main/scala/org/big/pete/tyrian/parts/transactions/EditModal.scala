package org.big.pete.tyrian.parts.transactions

import org.big.pete.sft.domain.{Account, Category, Currency, Op}
import org.big.pete.tyrian.component.inputs.{MoneyTextBox, TextInput}
import org.big.pete.tyrian.component.{DatePicker, DropDown, DropDownItem, ICheckbox}
import org.big.pete.tyrian.domain.Givens.{CatDropDownItem, given}
import org.big.pete.tyrian.toolz.Views.MBIcon
import org.big.pete.tyrian.toolz.{Views, sortCategories}
import tyrian.{Attr, Elem, Html as <, Html as ^}
import tyrian.syntax.*
//import tyrian.{Attr, Elem, Html as <, Html as ^}


object EditModal {
  def view(m: Model)
    (using categories: Map[Int, Category], accounts: Map[Int, Account], currencies: Map[String, Currency]): <[Msg] =
  {
    given catDropDownSupport(using categories: Map[Int, Category]): DropDownItem[Category] = new CatDropDownItem

    val sortedCategories = sortCategories(categories)

    val sortedAccounts = accounts.values.toList.sortBy(_.name)
    val destAccounts = m.editAccount.selected.map { id =>
      if (accounts(id.toInt).currencies.length > 1)
        sortedAccounts
      else
        accounts.filter(_._1 != id.toInt).values.toList.sortBy(_.name)
    }.getOrElse(sortedAccounts)

    val availableCurrencies = filterCurrencies(m.editAccount.selected, m.editDestAccount.selected, m.editDestCurrency.selected)
    val destCurrencies = filterCurrencies(m.editDestAccount.selected, m.editAccount.selected, m.editCurrency.selected)

    val (buttonLabel, buttonIcon) = m.editing.map(_ => "Update" -> MBIcon.Edit).getOrElse("Add" -> MBIcon.Add)

    Views.modal("tx-modal", m.isModalOpen, List.empty, List.empty) {
      <.form(
        <.div(^.cls := "row")(
          DatePicker.view(m.editDate, "tx-edit-date", List("col", "s12"), 401)
            .map(msg => Msg.EditDate(msg))
        ),
        <.div(^.cls := "row")(
          DropDown.view(m.editOp, Op.values.toList, "Operation", 402, List("col", "s12"))
            .map(msg => Msg.EditOp(msg))
        ),
        <.div(^.cls := "row")(
          MoneyTextBox.view(m.editAmount, "tx-edit-amount", "Amount", 403, List("col", "s12"))
            .map(msg => Msg.EditAmount(msg))
        ),
        <.div(^.cls := "row")(
          TextInput.view(m.editDescription, "tx-edit-description", "Description", 404, List("col", "s12"))
            .map(msg => Msg.EditDescription(msg))
        ),
        <.div(^.cls := "row")(
          DropDown.view(m.editCategory, sortedCategories, "Category", 405, List("col", "s12"))
            .map(msg => Msg.EditCategory(msg))
        ),
        <.div(^.cls := "row")(
          DropDown.view(m.editAccount, sortedAccounts, "Account", 406, List("col", "s12"))
            .map(msg => Msg.EditAccount(msg))
        ),
        <.div(^.cls := "row")(
          DropDown.view(m.editCurrency, availableCurrencies, "Currency", 407, List("col", "s12"))
            .map(msg => Msg.EditCurrency(msg))
        ),
        m.editOp.selected.filter(Op.valueOf(_) == Op.Transfer).map { _ =>
          <.div(^.cls := "row")(
            DropDown.view(m.editDestAccount, destAccounts, "Destination Account", 408, List("col", "s12"))
              .map(msg => Msg.EditDestAccount(msg))
          )
        }.orEmpty,
        m.editOp.selected.filter(_ == Op.Transfer.toString).map { _ =>
          <.div(^.cls := "row")(
            DropDown.view(m.editDestCurrency, destCurrencies, "Destination Currency", 409, List("col", "s12"))
              .map(msg => Msg.EditDestCurrency(msg))
          )
        }.orEmpty,
        m.editOp.selected.filter(_ == Op.Transfer.toString).map { _ =>
          <.div(^.cls := "row")(
            MoneyTextBox.view(m.editDestAmount, "tx-edit-dest-amount", "Destination Amount", 410, List("col", "s12"))
              .map(msg => Msg.EditDestAmount(msg))
          )
        }.orEmpty,
        (if (m.editing.isDefined) None else Some("")).map { _ =>
          <.div(^.cls := "row")(
            ICheckbox.view(m.editAddAnother, divWrapper, "col s12", 411, "add-another", "Add Another")
              .map(msg => Msg.EditAddAnother(msg))
          )
        }.orEmpty,
        Views.modalButtons(buttonLabel, buttonIcon, 412, Msg.EditModalConfirm, Msg.EditModalCancel)
      )
    }
  }

  private def divWrapper(attributes: List[Attr[ICheckbox.Msg]])(children: List[Elem[ICheckbox.Msg]]): <[ICheckbox.Msg] =
    <.div(attributes)(children)

  def filterCurrencies(account: Option[String], otherAccount: Option[String], otherCurrency: Option[String])
    (using accounts: Map[Int, Account], currencies: Map[String, Currency]): List[Currency] =
  {
    (account, otherAccount, otherCurrency) match {
      case (Some(id), Some(otherId), Some(curr)) if id == otherId =>
        currenciesByAccount(accounts(id.toInt))
          .filter(_.id == curr)

      case (Some(id), Some(otherId), None) if id == otherId =>
        currenciesByAccount(accounts(id.toInt))

      case (Some(id), _, _) =>
        currenciesByAccount(accounts(id.toInt))

      case _ =>
        List.empty
    }
  }

  private def currenciesByAccount(account: Account)(using accounts: Map[Int, Account], currencies: Map[String, Currency]): List[Currency] = {
    val available = account.currencies.map(_.currency).toSet
    currencies.filter { case (id, _) => available.contains(id) }
      .values
      .toList
      .sortBy(_.name)
  }
}
