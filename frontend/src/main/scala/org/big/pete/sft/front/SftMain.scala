package org.big.pete.sft.front

import cats.effect.SyncIO
import japgolly.scalajs.react.extra.router.{BaseUrl, RouterConfigDsl}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.scalajs.dom.document


object SftMain {
  sealed trait SftPages
  case object AccountsSelectionPage extends SftPages
  case class TransactionsPage(account: String) extends SftPages
  case class CategoriesPage(account: String) extends SftPages
  case class MoneyAccountsPage(account: String) extends SftPages

  val routerConfig = RouterConfigDsl[SftPages].buildConfig { dsl =>
    import dsl._

    (emptyRule |
      staticRoute(root, AccountsSelectionPage ~> renderR(rCtl => )))
  }

  def main(args: Array[String]): Unit = {


    val baseUrl = BaseUrl.fromWindowOrigin

  }
}
