package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.{CtorType, PropsChildren, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html

object FormModal {
  case class Props(id: String)

  class Backend {
    def render(props: Props, children: PropsChildren): VdomTagOf[html.Div] = {
      <.div(^.id := props.id, ^.tabIndex := 1, ^.cls := "modal open",
        <.div(^.cls := "modal-content",
          <.div(^.cls := "container",
            children
          )
        )
      )
    }
  }

  val component: Scala.Component[Props, Unit, Backend, CtorType.PropsAndChildren] = ScalaComponent.builder[Props]
    .stateless
    .renderBackendWithChildren[Backend]
    .build
}
