package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent}
import org.big.pete.react.WithInputFocus
import org.scalajs.dom.html.Label


object SimpleCheckbox {
  case class Props(label: String, checked: StateSnapshot[Boolean], tabIndex: Int)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onChange")

  class Backend extends WithInputFocus {
    def render(props: Props): VdomTagOf[Label] = {
      def handleEvt(evt: ReactFormEventFromInput): Callback =
        props.checked.setState(evt.target.checked)

      <.label(
        <.input(^.`type` := "checkbox", ^.tabIndex := props.tabIndex, ^.checked := props.checked.value, ^.onChange ==> handleEvt).withRef(inputRef),
        <.span(props.label)
      )
    }
  }

  val component: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(label: String, checked: StateSnapshot[Boolean], tabIndex: Int = -1): Unmounted[Props, Unit, Backend] =
    component(Props(label, checked, tabIndex))

  def withRef(ref: ToScalaComponent[Props, Unit, Backend])(label: String, checked: StateSnapshot[Boolean], tabIndex: Int = -1): Unmounted[Props, Unit, Backend] =
    component.withRef(ref).apply(Props(label, checked, tabIndex))
}
