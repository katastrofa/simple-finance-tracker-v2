package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.{BackendScope, CtorType, Reusability, ScalaComponent}
import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.extra.StateSnapshot
import org.big.pete.react.{TextInput, WithFocus}
import org.big.pete.sft.front.components.main.{formatDecimal, parseAmount}


object MoneyTextBox {
  import org.big.pete.react.Implicits._

  case class Props(
      id: String,
      label: String,
      value: StateSnapshot[BigDecimal],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty
  )

  case class State(text: String)

  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onEnterHit")
  implicit val stateReuse: Reusability[State] = Reusability.derive[State]


  class Backend($: BackendScope[Props, State]) extends WithFocus[TextInput.Props, TextInput.State, TextInput.Backend] {

    private def reflectChange(newText: String)(props: Props): Callback =
      parseAmount(newText).map { newVal =>
        if (newVal != props.value.value) props.value.setState(newVal) else Callback.empty
      }.getOrElse(Callback.empty)

    private def onTextChange(text: Option[String], fn: Callback): Callback =
      $.props.flatMap(reflectChange(text.getOrElse(""))) >> $.modState(_.copy(text.getOrElse(""))) >> fn

    private def onLostFocus(text: String): Callback = $.props.flatMap { props =>
      val newValue = parseAmount(text).getOrElse(props.value.value)
      props.value.setState(newValue) >> $.modState(_.copy(formatDecimal(newValue)))
    }

    def render(props: Props, state: State): Unmounted[TextInput.Props, TextInput.State, TextInput.Backend] = {
      val text = StateSnapshot.withReuse.prepare(onTextChange)(state.text)
      TextInput.withRef(focusRef)(props.id, props.label, text, props.tabIndex, props.classes, props.onEnterHit, onLostFocus)
    }
  }

  val component: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialStateFromProps(props => State(formatDecimal(props.value.value)))
    .renderBackend[Backend]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(
      id: String,
      label: String,
      value: StateSnapshot[BigDecimal],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty
  ): Unmounted[Props, State, Backend] =
    component.apply(Props(id, label, value, tabIndex, classes, onEnterHit))

  def withRef(ref: ToScalaComponent[Props, State, Backend])(
      id: String,
      label: String,
      value: StateSnapshot[BigDecimal],
      tabIndex: Int = -1,
      classes: List[String] = List.empty,
      onEnterHit: Callback = Callback.empty
  ): Unmounted[Props, State, Backend] =
    component.withRef(ref).apply(Props(id, label, value, tabIndex, classes, onEnterHit))
}
