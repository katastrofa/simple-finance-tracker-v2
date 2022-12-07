package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent}
import org.big.pete.react.WithInputFocus
import org.scalajs.dom.html.Label


object SimpleCheckbox {
  case class Props(label: String, checked: Boolean, tabIndex: Int, onChange: ReactFormEventFromInput => Callback)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onChange")

  class Backend extends WithInputFocus {
    def render(props: Props): VdomTagOf[Label] = {
      <.label(
        <.input(^.`type` := "checkbox", ^.tabIndex := props.tabIndex, ^.checked := props.checked, ^.onChange ==> props.onChange).withRef(inputRef),
        <.span(props.label)
      )
    }
  }

  val component: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
