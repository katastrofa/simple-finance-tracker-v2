package org.big.pete.sft.front

import japgolly.scalajs.react.extra.router.BaseUrl
import org.big.pete.react.DropDown
import org.big.pete.sft.domain.{Currency, EnhancedMoneyAccount}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.domain.Implicits._
import org.scalajs.dom.document

import java.time.LocalDate
import scala.scalajs.js.annotation.JSExport


object SftMain {
  sealed trait SftPages
  case object AccountsSelectionPage extends SftPages
  case class TransactionsPage(account: String) extends SftPages
  case class CategoriesPage(account: String) extends SftPages
  case class MoneyAccountsPage(account: String) extends SftPages

  val dropDownCategoryTree = new DropDown[CategoryTree]
  val dropDownCurrency = new DropDown[Currency]
  val dropDownMoneyAccount = new DropDown[EnhancedMoneyAccount]

  @JSExport
  def main(args: Array[String]): Unit = {
    val from = LocalDate.now().withDayOfMonth(1)
    val to = LocalDate.now().plusMonths(1L).withDayOfMonth(1).minusDays(1L)
    val baseUrl = (BaseUrl.fromWindowOrigin + "/api").value

    val state = SftState.component.apply(SftState.Props(from, to, baseUrl))
    state.renderIntoDOM(document.getElementById("sft-full"))
    ()
  }
}
