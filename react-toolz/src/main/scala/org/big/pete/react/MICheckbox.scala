package org.big.pete.react

import cats.effect.SyncIO
import enumeratum.{Enum, EnumEntry}
import japgolly.scalajs.react.{CtorType, Ref, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{BackendScope, Component, Unmounted}
import japgolly.scalajs.react.util.EffectCatsEffect.syncIO
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
      element: Seq[TagMod] => VdomElement,
      value: String,
      text: String,
      initialStatus: Status,
      onStateChange: (Status, String) => SyncIO[Unit]
  )
  case class State(status: Status)

  class Backend($: BackendScope[Props, State]) {
    private val inputRef = Ref[html.Input]

    def cycleState(props: Props): SyncIO[Unit] = $.state.flatMap { state =>
      val (cb, newStatus) = state.status match {
        case Status.`none` | Status.`indeterminate` =>
          val cb = inputRef.foreach { ref =>
            ref.indeterminate = false
            ref.checked = true
          }
          cb -> Status.checkedStatus
        case Status.`checkedStatus` =>
          val cb = inputRef.foreach(_.checked = false)
          cb -> Status.none
      }

      cb >> $.setState(state.copy(status = newStatus)) >> props.onStateChange(newStatus, props.value)
    }

    def setInput(status: Status): SyncIO[Unit] = status match {
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

    def render(prop: Props, state: State): VdomElement = {
      prop.element { Seq(
        ^.cls := "checkbox",
        <.label(
          <.input(^.`type` := "checkbox", ^.value := prop.value).withRef(inputRef),
          MaterialIcon(Status.statusToIcon(state.status), cycleState(prop)),
          <.span(prop.text)
        )
      )}
    }
  }

  val component: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent.builder[Props]
      .initialStateFromProps(props => State(props.initialStatus))
      .renderBackend[Backend]
      .componentDidMount(fn => fn.backend.setInput(fn.state.status))
      .build

  def apply(element: Seq[TagMod] => VdomElement, value: String, text: String): Unmounted[Props, State, Backend] =
    component.apply(Props(element, value, text, Status.`none`, (_, _) => SyncIO.unit))

  def apply(
      element: Seq[TagMod] => VdomElement,
      value: String,
      text: String,
      initialStatus: Status,
      key: String
  ): Unmounted[Props, State, Backend] =
    apply(element, value, text, initialStatus, (_, _) => SyncIO.unit, key)

  def apply(
      element: Seq[TagMod] => VdomElement,
      value: String,
      text: String,
      initialStatus: Status,
      onStateChange: (Status, String) => SyncIO[Unit],
      key: String
  ): Unmounted[Props, State, Backend] =
    component.withKey(key).apply(Props(element, value, text, initialStatus, onStateChange))
}
