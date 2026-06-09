package org.big.pete.tyrian.component

import org.big.pete.tyrian.domain.PassModel
import org.big.pete.tyrian.toolz.Views
import tyrian.{Attr, Elem, Html as <, Html as ^}


case class ICheckboxModel(status: ICheckbox.Status) extends PassModel
enum ICheckboxMsg {
  case Toggle
}

object ICheckbox {
  enum Status {
    case None
    case Checked
    case Partial
  }

  private val statusToIcon: Map[Status, String] = Map(
    Status.None -> "check_box_outline_blank",
    Status.Partial -> "indeterminate_check_box",
    Status.Checked -> "check_box"
  )
  private val statusToChecked: Map[Status, Boolean] = Map(
    Status.None -> false,
    Status.Partial -> false,
    Status.Checked -> true
  )

  def fromBoolean(status: Boolean): Status =
    if (status) Status.Checked else Status.None

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
      tabIndex: Int,
      value: String,
      text: String
  ): <[Msg] = {
    val cls = ("checkbox" :: classes.filter(_._2).keys.toList).mkString(" ")
    view(m, wrappingTag, cls, tabIndex, value, text)
  }

  def view(
      m: Model,
      wrappingTag: (attributes: List[Attr[Msg]]) => (children: List[Elem[Msg]]) => <[Msg],
      cls: String,
      tabIndex: Int,
      value: String,
      text: String
  ): <[Msg] = {
    wrappingTag(List(^.cls := cls))(List(
      <.label(
        <.input(^.`type` := "checkbox", ^.value := value, ^.tabIndex := tabIndex, ^.checked(statusToChecked(m.status))),
        Views.icon(statusToIcon(m.status), ICheckboxMsg.Toggle),
        <.span(text)
      )
    ))
  }
}
