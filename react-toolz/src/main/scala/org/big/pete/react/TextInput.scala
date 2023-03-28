package org.big.pete.react

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import japgolly.scalajs.react.extra.{EventListener, OnUnmount, StateSnapshot}
import japgolly.scalajs.react.{Callback, CtorType, ReactFormEventFromInput, ReactKeyboardEventFromInput, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Div


object TextInput {
  case class Props(
      id: String,
      label: String,
      text: StateSnapshot[String],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty,
      onLostFocus: String => Callback = _ => Callback.empty
  )
  case class State(focus: Boolean)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onEnterHit", "onLostFocus")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]

  class Backend($: BackendScope[Props, State]) extends OnUnmount with WithInputFocus {
    def isActive(props: Props, state: State): Boolean =
      props.text.value.nonEmpty || state.focus

    def focusIn: Callback =
      $.modState(_.copy(true)) >> inputRef.foreach(_.select())
    def focusOut: Callback =
      $.modState(_.copy(false)) >> $.props.flatMap(props => props.onLostFocus(props.text.value))

    private def keyPress(onEnterHit: Callback)(evt: ReactKeyboardEventFromInput): Callback =
      if (evt.key == "Enter") onEnterHit else Callback.empty


    def render(props: Props, state: State): VdomTagOf[Div] = {
      def onChange(evt: ReactFormEventFromInput): Callback =
        props.text.setState(evt.target.value)

      <.div(^.cls := (List("input-field") ++ props.classes).mkString(" "),
        <.input(
          ^.id := props.id,
          ^.`type` := "text",
          ^.value := props.text.value,
          ^.tabIndex := props.tabIndex,
          ^.onChange ==> onChange,
          ^.onKeyPress ==> keyPress(props.onEnterHit)
        ).withRef(inputRef),
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
      text: StateSnapshot[String],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty,
      onLostFocus: String => Callback = _ => Callback.empty
  ): Unmounted[Props, State, Backend] =
    component.apply(Props(id, label, text, tabIndex, classes, onEnterHit, onLostFocus))

  def withRef(ref: ToScalaComponent[Props, State, Backend])(
      id: String,
      label: String,
      text: StateSnapshot[String],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty,
      onLostFocus: String => Callback = _ => Callback.empty
  ): Unmounted[Props, State, Backend] =
    component.withRef(ref).apply(Props(id, label, text, tabIndex, classes, onEnterHit, onLostFocus))
}
