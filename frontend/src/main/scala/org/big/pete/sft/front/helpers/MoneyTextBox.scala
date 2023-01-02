package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.{BackendScope, CtorType, ReactFormEventFromInput, Reusability, ScalaComponent}
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import org.big.pete.react.{TextInput, WithFocus}
import org.big.pete.sft.front.components.main.{formatDecimal, parseAmount}


object MoneyTextBox {
  import org.big.pete.react.Implicits._

  case class Props(
      id: String,
      label: String,
      value: BigDecimal,
      onChange: BigDecimal => Callback,
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty,
  )

  case class State(text: String)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onChange", "onEnterHit")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]

  class Backend($: BackendScope[Props, State]) extends WithFocus[TextInput.Props, TextInput.State, TextInput.Backend] {

    private def reflectChange(newText: String)(props: Props): Callback =
      parseAmount(newText).map { newVal =>
        if (newVal != props.value) props.onChange(newVal) else Callback.empty
      }.getOrElse(Callback.empty)

    private def onTextChange(evt: ReactFormEventFromInput): Callback =
      $.props.flatMap(reflectChange(evt.target.value)) >> $.modState(_.copy(evt.target.value))

    private def onLostFocus(text: String): Callback = $.props.flatMap { props =>
      val newValue = parseAmount(text).getOrElse(props.value)
      props.onChange(newValue) >> $.modState(_.copy(formatDecimal(newValue)))
    }

    def render(props: Props, state: State): Unmounted[TextInput.Props, TextInput.State, TextInput.Backend] = {
      TextInput.component.withRef(focusRef).apply(TextInput.Props(
        props.id, props.label, state.text, onTextChange, props.tabIndex, props.classes, props.onEnterHit, onLostFocus
      ))
    }
  }

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(props => State(formatDecimal(props.value)))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build
}
