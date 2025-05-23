package org.big.pete.tyrian.component

import org.big.pete.tyrian.toolz.Views
import tyrian.{Attr, Elem, Html as <, Html as ^}


case class ICheckboxModel(status: ICheckbox.Status)
enum ICheckboxMsg {
  case Toggle
}

object ICheckbox {
  enum Status {
    case None
    case Checked
    case Partial
  }

  private object Status {
    val statusToIcon: Map[Status, String] = Map(
      None -> "check_box_outline_blank",
      Partial -> "indeterminate_check_box",
      Checked -> "check_box"
    )
    val statusToChecked: Map[Status, Boolean] = Map(
      None -> false,
      Partial -> false,
      Checked -> true
    )

    def fromBoolean(status: Boolean): Status =
      if (status) Checked else None
  }

  type Model = ICheckboxModel
  type Msg = ICheckboxMsg

  def init(status: Status = Status.None): Model =
    ICheckboxModel(status)

  def update(msg: Msg, m: Model): Model = {
    msg match {
      case ICheckboxMsg.Toggle =>
        m.status match {
          case Status.None | Status.Partial => m.copy(Status.Checked)
          case Status.Checked => m.copy(Status.None)
        }
    }
  }

  def view(
      m: Model,
      wrappingTag: (attributes: List[Attr[Msg]]) => (children: List[Elem[Msg]]) => <[Msg],
      classes: Map[String, Boolean],
      value: String,
      text: String
  ): <[Msg] = {
    val cls = ("checkbox" :: classes.filter(_._2).keys.toList).mkString(" ")
    view(m, wrappingTag, cls, value, text)
  }

  def view(
      m: Model,
      wrappingTag: (attributes: List[Attr[Msg]]) => (children: List[Elem[Msg]]) => <[Msg],
      cls: String,
      value: String,
      text: String
  ): <[Msg] = {
    wrappingTag(List(^.cls := cls))(List(
      <.label(
        <.input(^.`type` := "checkbox", ^.value := value, ^.checked(Status.statusToChecked(m.status))),
        Views.icon(Status.statusToIcon(m.status), ICheckboxMsg.Toggle),
        <.span(text)
      )
    ))
  }
}
