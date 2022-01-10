package org.big.pete.react

import enumeratum.{Enum, EnumEntry}
import japgolly.scalajs.react.{Callback, CtorType, Ref, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html


object MICheckbox {
  sealed trait Status extends EnumEntry
  object Status extends Enum[Status] {
    case object none extends Status
    case object indeterminate extends Status
    case object checkedStatus extends Status

    val values: IndexedSeq[Status] = findValues

    val statusToIcon: Map[Status, String] = Map(
      none -> "check_box_outline_blank",
      indeterminate -> "indeterminate_check_box",
      checkedStatus -> "check_box"
    )
    val statusToChecked: Map[Status, Boolean] = Map(
      none -> false,
      indeterminate -> false,
      checkedStatus -> true
    )

    def fromBoolean(status: Boolean): Status =
      if (status) checkedStatus else none
  }

  case class Props(
      wrappingElement: Seq[TagMod] => VdomElement,
      classes: Map[String, Boolean],
      value: String,
      text: String,
      status: Status,
      statusChange: (Status, String) => Callback
  )

  class Backend {
    private val inputRef = Ref[html.Input]

    def cycleState(props: Props): Callback = {
      val newStatus = props.status match {
        case Status.`none` | Status.`indeterminate` => Status.checkedStatus
        case Status.`checkedStatus` => Status.none
      }
      props.statusChange(newStatus, props.value)
    }

    def setInput(status: Status): Callback = status match {
      case Status.`none` => inputRef.foreach { ref =>
        ref.checked = false
        ref.indeterminate = false
      }
      case Status.`indeterminate` => inputRef.foreach { ref =>
        ref.checked = false
        ref.indeterminate = true
      }
      case Status.`checkedStatus` => inputRef.foreach { ref =>
        ref.checked = true
        ref.indeterminate = false
      }
    }

    def render(props: Props): VdomElement = {
      props.wrappingElement { Seq(
        ^.classSet1M("checkbox", props.classes),
        <.label(
          <.input(^.`type` := "checkbox", ^.value := props.value).withRef(inputRef),
          MaterialIcon(Status.statusToIcon(props.status), cycleState(props)),
          <.span(props.text)
        )
      )}
    }
  }

  val component: Component[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent.builder[Props]
      .stateless
      .renderBackend[Backend]
      .componentDidMount(fn => fn.backend.setInput(fn.props.status))
      .build

  def apply(
      wrappingElement: Seq[TagMod] => VdomElement,
      value: String,
      text: String,
      statusChange: (Status, String) => Callback
  ): Unmounted[Props, Unit, Backend] =
    component(Props(wrappingElement, Map.empty, value, text, Status.none, statusChange))
}
