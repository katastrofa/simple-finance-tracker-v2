package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import org.big.pete.react.WithButtonFocus
import org.scalajs.dom.html.Button


object NiceButton {
  case class Props(label: TagMod, tabIndex: Int, onClick: Callback)

  class Backend extends WithButtonFocus {
    def render(props: Props): VdomTagOf[Button] = {
      <.button(^.cls := "waves-effect waves-light btn nice", ^.`type` := "button", ^.tabIndex := props.tabIndex,
        ^.onClick --> props.onClick, props.label
      )
    }
  }

  val comp: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .build

  def apply(tabIndex: Int, onClick: Callback)(label: TagMod): Unmounted[Props, Unit, Backend] =
    comp(Props(label, tabIndex, onClick))

  def withRef(ref: ToScalaComponent[Props, Unit, Backend], tabIndex: Int, onClick: Callback)(label: TagMod): Unmounted[Props, Unit, Backend] =
    comp.withRef(ref).apply(Props(label, tabIndex, onClick))
}
