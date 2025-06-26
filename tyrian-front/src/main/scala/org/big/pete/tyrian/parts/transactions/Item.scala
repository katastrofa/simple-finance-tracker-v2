package org.big.pete.tyrian.parts.transactions

import org.big.pete.sft.domain.{Account, Category, Currency, Op, Status, Transaction}
import org.big.pete.tyrian.component.ICheckbox
import org.big.pete.tyrian.domain.TransactionExtension.*
import org.big.pete.tyrian.toolz.Views.MBIcon
import org.big.pete.tyrian.toolz.{DateFormat, SmallDateFormat, Views}
import tyrian.{Attr, Elem, Html as <, Html as ^}


object Item {

  private def tdWrapper(attributes: List[Attr[ICheckbox.Msg]])(children: List[Elem[ICheckbox.Msg]]): <[ICheckbox.Msg] =
    <.td(attributes)(children)

  def view(m: Model, t: Transaction, checkboxes: Map[Int, ICheckbox.Model])
    (using currencies: Map[String, Currency], accounts: Map[Int, Account], categories: Map[Int, Category]): <[Msg] =
  {
    <.tr(^.cls := "show-hoverable")(
      ICheckbox.view(checkboxes(t.id), tdWrapper, "check hide-on-med-and-down center-align", 402, s"t-${t.id}", "")
        .map(msg => Msg.TransactionCheckbox(t.id, msg)),
      <.td(^.cls := "date")(
        <.div(^.cls := "hide-on-small-only")(t.date.format(DateFormat)),
        <.div(^.cls := "show-on-small hide-on-med-and-up")(t.date.format(SmallDateFormat))
      ),
      <.td(^.cls := "description", ^.onClick(Msg.TransactionDetails(t.id)))(
        Views.icon("edit", List("show-on-hover", "pointer", "pad-right", "hide-on-med-and-down"), Msg.TransactionEdit(t)), // stopPropagation
        <.text(t.description)
      ),
      <.td(^.cls := s"right-align amount ${amountCls(t)}")(
        t.fAmount + addAmountInfo(t)
      ),
      <.td(^.cls := "category")(
        <.span(^.cls := "category-big")(t.fullCategoryName),
        <.span(^.cls := "category-small")(t.categoryName)
      ),
      <.td(^.cls := "account hide-on-med-and-down")(accounts(t.account).name + addAccountInfo(t)),
      <.td(^.cls := "delete hide-on-med-and-down")(
        Views.icon("delete", List("pointer"), Msg.TransactionDelete(t))
      ),
      <.td(^.cls := "status center-align")(
        Views.icon(StatusToIcon(t.status), List("pointer"), Msg.TransactionStatus(t))
      )
    )
  }

  def viewDetails(m: Model, t: Transaction)
    (using currencies: Map[String, Currency], accounts: Map[Int, Account], categories: Map[Int, Category]): <[Msg] =
  {
    <.tr(^.cls := "details")(
      <.td(^.cls := "details-col", ^.colSpan := m.colSpan)(
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Date:"),
          <.span(^.cls := "col l10 s9")(t.date.format(DateFormat))
        ),
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Description:"),
          <.span(^.cls := "col l10 s9")(t.description)
        ),
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Type: "),
          <.span(^.cls := "col l10 s9")(t.op.toString)
        ),
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Amount: "),
          <.span(^.cls := s"col l10 s9 ${amountCls(t)}")(t.fAmount + addAmountInfo(t))
        ),
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Category: "),
          <.span(^.cls := "col l10 s9")(t.fullCategoryName)
        ),
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Account: "),
          <.span(^.cls := "col l10 s9")(accounts(t.account).name + addAccountInfo(t))
        ),
        <.div(^.cls := "details-item row")(
          <.strong(^.cls := "col l2 s3")("Status: "),
          <.span(^.cls := "col l10 s9")(t.status.toString)
        ),
        <.div(^.cls := "details-item row pad-top")(
          <.div(^.cls := "col l2 s3")(
            Views.niceButton("Edit", 42, Msg.TransactionEdit(t), Some(MBIcon.Other("edit")))
          ),
          <.div(^.cls := "col l2 s3 offset-s6 offset-l8 right-align")(
            Views.niceButton("Delete", 43, Msg.TransactionDelete(t), Some(MBIcon.Other("delete")))
          )
        )
      )
    )
  }

  private def amountCls(t: Transaction): String = {
    t.op match {
      case Op.Income => "green-text text-darken-1"
      case Op.Expense => "red-text text-darken-1"
      case Op.Transfer => "amber-text text-darken-2"
    }
  }

  final private val StatusToIcon = Map[Status, String](
    Status.None -> "horizontal_rule",
    Status.Auto -> "blur_circular",
    Status.Verified -> "check_circle"
  )

  private def addAmountInfo(t: Transaction)(using currencies: Map[String, Currency]): String =
    t.destinationAmount.map(_ => " -> " + t.fDAmount).getOrElse("")

  private def addAccountInfo(t: Transaction)(using accounts: Map[Int, Account]) =
    t.destinationAccount.map(account => " -> " + accounts(account).name).getOrElse("")
}
