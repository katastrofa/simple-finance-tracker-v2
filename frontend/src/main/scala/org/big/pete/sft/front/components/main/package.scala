package org.big.pete.sft.front.components

import japgolly.scalajs.react.vdom.VdomElement
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Element

import java.time.format.DateTimeFormatter


package object main {
  final val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def formatAmount(currency: String, amount: BigDecimal): String =
    "%s%.2f".format(currency, amount)

  def tableWrap(
      preTable: TagMod,
      head: VdomElement,
      body: VdomArray,
      foot: VdomElement,
      postTable: TagMod
  ): VdomTagOf[Element] =
    <.main(
      <.div(^.cls := "padding",
        preTable,
        <.table(^.cls := "striped small sft",
          <.thead(head),
          <.tbody(body),
          <.tfoot(foot)
        ),
        postTable
      )
    )
}
