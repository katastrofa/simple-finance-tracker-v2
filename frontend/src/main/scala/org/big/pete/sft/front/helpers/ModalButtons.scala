package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.ScalaFn.{Component, Unmounted}
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaFnComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon


object ModalButtons {
  case class Props(firstLabel: String, tabIndex: Int, publish: Callback, closeAndClean: Callback)

  implicit val modalPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("publish", "closeAndClean")

  val component: Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { props =>
    <.div(^.cls := "row",
      <.div(^.cls := "col s12 right-align",
        <.button(^.cls := "waves-effect waves-light btn",
          ^.`type` := "button",
          ^.tabIndex := props.tabIndex,
          ^.onClick --> props.publish,
          MaterialIcon("add_task"),
          props.firstLabel
        ),
        <.button(^.cls := "waves-effect waves-light btn",
          ^.`type` := "button",
          ^.tabIndex := props.tabIndex + 1,
          ^.onClick --> props.closeAndClean,
          MaterialIcon("highlight_off"),
          "Close"
        )
      )
    )
  }

  def add(tabIndex: Int, publish: Callback, closeAndClean: Callback): Unmounted[Props] =
    component(Props("Add", tabIndex, publish, closeAndClean))

  def apply(text: String, tabIndex: Int, publish: Callback, closeAndClean: Callback): Unmounted[Props] =
    component(Props(text, tabIndex, publish, closeAndClean))
}
