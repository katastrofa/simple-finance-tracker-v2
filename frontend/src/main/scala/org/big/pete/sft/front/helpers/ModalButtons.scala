package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.{Callback, CtorType, Ref, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.{HasFocus, MaterialIcon}
import org.scalajs.dom.html.{Button, Div}


object ModalButtons {
  case class Props(firstLabel: String, tabIndex: Int, publish: Callback, closeAndClean: Callback)

  implicit val modalPropsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("publish", "closeAndClean")

  class Backend extends HasFocus {
    private val buttonRef = Ref.toVdom[Button]

    override def focus: Callback =
      buttonRef.foreach(_.focus())

    def render(props: Props): VdomTagOf[Div] = {
      <.div(
        ^.cls := "row",
        <.div(
          ^.cls := "col s12 right-align",
          <.button(
            ^.cls := "waves-effect waves-light btn",
            ^.`type` := "button",
            ^.tabIndex := props.tabIndex,
            ^.onClick --> props.publish,
            MaterialIcon("add_task"),
            props.firstLabel
          ).withRef(buttonRef),
          <.button(
            ^.cls := "waves-effect waves-light btn",
            ^.`type` := "button",
            ^.tabIndex := props.tabIndex + 1,
            ^.onClick --> props.closeAndClean,
            MaterialIcon("highlight_off"),
            "Close"
          )
        )
      )
    }
  }

  val component: Scala.Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def add(tabIndex: Int, publish: Callback, closeAndClean: Callback): Scala.Unmounted[Props, Unit, Backend] =
    component(Props("Add", tabIndex, publish, closeAndClean))

  def apply(text: String, tabIndex: Int, publish: Callback, closeAndClean: Callback): Scala.Unmounted[Props, Unit, Backend] =
    component(Props(text, tabIndex, publish, closeAndClean))
}
