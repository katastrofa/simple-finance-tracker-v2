package org.big.pete.sft.front

import org.scalajs.dom.document

import java.time.LocalDate
import scala.scalajs.js.annotation.JSExport


object SftMain {
  sealed trait SftPages
  case object AccountsSelectionPage extends SftPages
  case class TransactionsPage(account: String) extends SftPages
  case class CategoriesPage(account: String) extends SftPages
  case class MoneyAccountsPage(account: String) extends SftPages


  @JSExport
  def main(args: Array[String]): Unit = {
    val from = LocalDate.now().withDayOfMonth(1)
    val to = LocalDate.now().plusMonths(1L).withDayOfMonth(1).minusDays(1L)
    val state = SftState.component.apply(SftState.Props(from, to))
    state.renderIntoDOM(document.getElementById("sft-full"))
    ()
  }
}
