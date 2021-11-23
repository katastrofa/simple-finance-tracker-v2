package org.big.pete.react

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.html_<^._

object MaterialIcon {
  sealed trait DomType
  case object span extends DomType
  case object i extends DomType

  sealed trait Size
  case object small extends Size
  case object midMedium extends Size
  case object medium extends Size
  case object large extends Size

  case class Props(dom: DomType, size: Size, icon: String)

  val Icon: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props].stateless.noBackend
    .render_P { props =>
      val classes = props.size match {
        case small => "small material-icons"
        case midMedium => "mid-medium material-icons"
        case medium => "medium material-icons"
        case large => "large material-icons"
      }

      props.dom match {
        case span => <.span(^.cls := classes, props.icon)
        case i => <.i(^.cls := classes, props.icon)
      }
    }.build

  def apply(icon: String): Unmounted[Props, Unit, Unit] = Icon(Props(i, small, icon))
  def apply(dom: DomType, size: Size, icon: String): Unmounted[Props, Unit, Unit] = Icon(Props(dom, size, icon))
}
