package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.{Callback, CtorType, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{MaterialIcon, WithFocus}
import org.scalajs.dom.html.Div


object ModalButtons {
  case class Props(firstLabel: String, tabIndex: Int, publish: Callback, closeAndClean: Callback)

  implicit val modalButtonsPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("publish", "closeAndClean")

  class Backend extends WithFocus[NiceButton.Props, Unit, NiceButton.Backend] {
    def render(props: Props): VdomTagOf[Div] = {
      <.div(^.cls := "row",
        <.div(^.cls := "col s12 right-align",
          NiceButton.withRef(focusRef, props.tabIndex, props.publish)(TagMod(MaterialIcon("add_task"), props.firstLabel)),
          NiceButton(props.tabIndex + 1, props.closeAndClean)(TagMod(MaterialIcon("highlight_off"), "Close"))
        )
      )
    }
  }

  val comp: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def add(tabIndex: Int, publish: Callback, closeAndClean: Callback): Scala.Unmounted[Props, Unit, Backend] =
    comp(Props("Add", tabIndex, publish, closeAndClean))

  def apply(text: String, tabIndex: Int, publish: Callback, closeAndClean: Callback): Scala.Unmounted[Props, Unit, Backend] =
    comp(Props(text, tabIndex, publish, closeAndClean))
}
