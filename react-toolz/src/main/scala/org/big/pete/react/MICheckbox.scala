package org.big.pete.react

import enumeratum.{Enum, EnumEntry}
import japgolly.scalajs.react.{BackendScope, Callback, Children, CtorType, Ref, Reusability, ScalaComponent, UpdateSnapshot}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.component.builder.ComponentBuilder
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import org.scalajs.dom.html.{LI, TableCell}


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
//    val statusToChecked: Map[Status, Boolean] = Map(
//      none -> false,
//      indeterminate -> false,
//      checkedStatus -> true
//    )

    def fromBoolean(status: Boolean): Status =
      if (status) checkedStatus else none
  }

  case class Props(value: String, text: String, status: StateSnapshot[Status], classes: Map[String, Boolean] = Map.empty)

  implicit val checkboxStatusReuse: Reusability[Status] = Reusability.byRefOr_==[Status]
  implicit val stringBoolMapReuse: Reusability[Map[String, Boolean]] = Reusability.map[String, Boolean]
  implicit val miCheckboxPropsReuse: Reusability[Props] = Reusability.derive[Props]

  trait Backend {
    val $: BackendScope[Props, Unit]
    private val inputRef = Ref[html.Input]

    private def cycleState: Callback = $.props.flatMap { props =>
      val newStatus = props.status.value match {
        case Status.`none` | Status.`indeterminate` => Status.checkedStatus
        case Status.`checkedStatus` => Status.none
      }
      props.status.setState(newStatus)
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

    protected def insides(props: Props): TagMod = {
      Seq(
        ^.classSet1M("checkbox", props.classes),
        <.label(
          <.input(^.`type` := "checkbox", ^.value := props.value).withRef(inputRef),
          MaterialIcon(Status.statusToIcon(props.status.value), cycleState),
          <.span(props.text)
        )
      ).toTagMod
    }
  }

  class LiBackend(val $: BackendScope[Props, Unit]) extends Backend {
    def render(props: Props): VdomTagOf[LI] = <.li(insides(props))
  }

  class ThBackend(val $: BackendScope[Props, Unit]) extends Backend {
    def render(props: Props): VdomTagOf[TableCell] = <.th(insides(props))
  }

  class TdBackend(val $: BackendScope[Props, Unit]) extends Backend {
    def render(props: Props): VdomTagOf[TableCell] = <.td(insides(props))
  }


  private val stateless = ScalaComponent.builder[Props].stateless
  private def build[T <: Backend](
      comp: ComponentBuilder.LastStep[Props, Children.None, Unit, T, UpdateSnapshot.None]
  ): Component[Props, Unit, T, CtorType.Props] =
    comp.componentDidMount(fn => fn.backend.setInput(fn.props.status.value))
      .configure(Reusability.shouldComponentUpdate)
      .build

  val liComponent: Component[Props, Unit, LiBackend, CtorType.Props] = build(stateless.renderBackend[LiBackend])
  val thComponent: Component[Props, Unit, ThBackend, CtorType.Props] = build(stateless.renderBackend[ThBackend])
  val tdComponent: Component[Props, Unit, TdBackend, CtorType.Props] = build(stateless.renderBackend[TdBackend])

  def li(value: String, text: String, status: StateSnapshot[Status], classes: Map[String, Boolean] = Map.empty): Unmounted[Props, Unit, LiBackend] =
    liComponent(Props(value, text, status, classes))
  def th(value: String, text: String, status: StateSnapshot[Status], classes: Map[String, Boolean] = Map.empty): Unmounted[Props, Unit, ThBackend] =
    thComponent(Props(value, text, status, classes))
  def td(value: String, text: String, status: StateSnapshot[Status], classes: Map[String, Boolean] = Map.empty): Unmounted[Props, Unit, TdBackend] =
    tdComponent(Props(value, text, status, classes))
}
