package org.big.pete.tyrian.parts.transactions

import cats.effect.IO
import org.big.pete.sft.domain.Transaction
import org.big.pete.tyrian.component.ICheckbox
import org.big.pete.tyrian.domain.SortingOrder.{Asc, Desc}
import org.big.pete.tyrian.domain.{Date, Description, SortingColumn, SortingName}
import org.big.pete.tyrian.toolz.Views
import org.scalajs.dom.{document, window}
import tyrian.{Attr, Cmd, Elem, Html as <, Html as ^, Sub}

import scala.concurrent.duration.DurationInt


final case class Model(
    isModalOpen: Boolean,
    editing: Option[Transaction],
    ordering: List[SortingColumn[?]],

    headerCheckbox: ICheckbox.Model,
    checkboxes: Map[Int, ICheckbox.Model],

    colSpan: Int,
    debouncing: Option[Int]
)
enum Msg {
  case OpenModalAdd

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

  def init(transactions: List[Transaction]): Model =
    Model(
      isModalOpen = false,
      editing = None,
      ordering = List(Date(Desc), Description(Asc)),
      headerCheckbox = ICheckbox.init(),
      checkboxes = transactions.map(t => (t.id, ICheckbox.init())).toMap,
      colSpan = 12,
      debouncing = None
    )

  def update(msg: Msg, m: Model): Model = {
    msg match {
      case Msg.OpenModalAdd =>
        m.copy(isModalOpen = true)

      case Msg.RecalcSpan =>
        m.copy(colSpan = calculateColSpan)
    }
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

  def view(m: Model): <[Msg] = {
    Views.tableWrap(
      "transactions-table",
      <.div(
        ""
      ),
      Header.viewHeader(m),
      viewTransactions(),
      Header.viewHeader(m),
      viewButtons()
    )
  }



  private def viewTransactions(): <[Msg] = {
    <.tr()
  }

  private def viewButtons(): <[Msg] = {
    <.div()
  }



  private def calculateColSpan: Int = {
    document.querySelectorAll("#transactions-table tr:first-child td").filterNot { el =>
      window.getComputedStyle(el).display == "none"
    }.length
  }

  def subscriptions(model: Model): Sub[IO, Msg] = Sub.Batch[IO, Msg](
    tick(model),
    Sub.fromEvent("resize", window)(_ => Some(Msg.RecalcSpan)),
    Sub.animationFrameTick("transactions-page-load")(_ => Msg.RecalcSpan)
  )
}
