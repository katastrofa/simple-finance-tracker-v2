package org.big.pete.sft.front.components

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.Mathjs
import org.big.pete.sft.domain.Currency
import org.scalajs.dom.html.Element

import java.time.format.DateTimeFormatter
import scala.util.Try


package object main {
  final val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def formatAmount(currency: String, amount: BigDecimal): String =
    "%s%.2f".format(currency, amount)

  def formatDecimal(amount: BigDecimal): String =
    "%.2f".format(amount)

  def displayCurrency(currency: Currency): String =
    s"${currency.name} (${currency.symbol})"

  def parseAmount(newAmount: String): Option[BigDecimal] = {
    Try(Mathjs.evaluate(newAmount))
      .toOption
      .map { value =>
        val bd = BigDecimal.decimal(value)
        bd.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
      }
  }

  def tableWrap(
      id: String,
      preTable: TagMod,
      head: VdomElement,
      body: VdomArray,
      foot: VdomElement,
      postTable: TagMod
  ): VdomTagOf[Element] =
    <.main(
      <.div(^.cls := "padding",
        preTable,
        <.table(^.id := id, ^.cls := "striped small sft highlight",
          <.thead(head),
          <.tbody(body),
          <.tfoot(foot)
        ),
        postTable
      )
    )
}
