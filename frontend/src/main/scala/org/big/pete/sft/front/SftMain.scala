package org.big.pete.sft.front

import japgolly.scalajs.react.extra.router.BaseUrl
import org.big.pete.react.DropDown
import org.big.pete.sft.domain.{Currency, EnhancedAccount, SimpleUser, TransactionType}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.domain.Implicits._
import org.scalajs.dom.document

import scala.scalajs.js.annotation.JSExport


object SftMain {
  sealed trait SftPages
  case object WalletSelectionPage extends SftPages
  case class TransactionsPage(wallet: String) extends SftPages
  case class CategoriesPage(wallet: String) extends SftPages
  case class AccountsPage(wallet: String) extends SftPages


  @JSExport
  def main(args: Array[String]): Unit = {
    val baseUrl = (BaseUrl.fromWindowOrigin + "/api").value

    val route = Routing.component.apply(Routing.Props(baseUrl))
    route.renderIntoDOM(document.getElementById("sft-full"))
    ()
  }
}
