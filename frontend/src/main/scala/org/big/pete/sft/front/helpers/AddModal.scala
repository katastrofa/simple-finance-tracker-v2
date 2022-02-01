package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.Scala
import japgolly.scalajs.react.{CtorType, PropsChildren, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Div


object AddModal {
  case class Props(id: String, isOpen: Boolean)

  implicit val modalPropsReuse: Reusability[Props] = Reusability.derive[Props]

  class Backend {
    def render(props: Props, children: PropsChildren): VdomTagOf[Div] = {
      <.div(^.id := props.id,
        ^.tabIndex := 1,
        ^.classSet("modal" -> true, "open" -> props.isOpen),
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
    .configure(Reusability.shouldComponentUpdate)
    .build
}
