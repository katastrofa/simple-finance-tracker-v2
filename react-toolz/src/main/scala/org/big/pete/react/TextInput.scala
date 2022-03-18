package org.big.pete.react

import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount}
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Div


object TextInput {
  case class Props(
      id: String,
      label: String,
      value: String,
      onChange: ReactFormEventFromInput => Callback,
      tabIndex: Int = -1,
      classes: List[String] = List.empty
  )
  case class State(focus: Boolean)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onChange")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]

  class Backend($: BackendScope[Props, State]) extends OnUnmount {
    def isActive(props: Props, state: State): Boolean =
      props.value.nonEmpty || state.focus

    def focusIn: Callback = $.modState(_.copy(true))
    def focusOut: Callback = $.modState(_.copy(false))

    def render(props: Props, state: State): VdomTagOf[Div] = {
      <.div(^.cls := (List("input-field") ++ props.classes).mkString(" "),
        <.input(^.id := props.id, ^.`type` := "text", ^.value := props.value, ^.tabIndex := props.tabIndex, ^.onChange ==> props.onChange),
        <.label(^.`for` := props.id, ^.classSet("active" -> isActive(props, state)), props.label)
      )
    }
  }

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState(State(false))
    .renderBackend[Backend]
    .configure(EventListener.install("focusin", _.backend.focusIn))
    .configure(EventListener.install("focusout", _.backend.focusOut))
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(
      id: String,
      label: String,
      value: String,
      onChange: ReactFormEventFromInput => Callback,
      tabIndex: Int = -1,
      classes: List[String] = List.empty
  ): Unmounted[Props, State, Backend] =
    component.apply(Props(id, label, value, onChange, tabIndex, classes))
}
