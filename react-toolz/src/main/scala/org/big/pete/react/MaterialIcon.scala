package org.big.pete.react

import cats.effect.SyncIO
import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
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

  case class Props(dom: DomType, size: Size, icon: String, onClick: SyncIO[Unit], additionalClasses: Set[String])

  val Icon: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props].stateless.noBackend
    .render_P { props =>
      val sizeClass = props.size match {
        case `small` => "small"
        case `midMedium` => "mid-medium"
        case `medium` => "medium"
        case `large` => "large"
      }
      val classes = (Set("material-icons", sizeClass) ++ props.additionalClasses).mkString(" ")

      props.dom match {
        case `span` => <.span(^.cls := classes, ^.onClick --> props.onClick, props.icon)
        case `i` => <.i(^.cls := classes, ^.onClick --> props.onClick, props.icon)
      }
    }.build

  def apply(icon: String): Unmounted[Props, Unit, Unit] =
    Icon(Props(i, small, icon, SyncIO.unit, Set.empty))
  def apply(icon: String, additionalClasses: Set[String]) =
    Icon(Props(i, small, icon, SyncIO.unit, additionalClasses))
  def apply(icon: String, onClick: SyncIO[Unit]): Unmounted[Props, Unit, Unit] =
    Icon(Props(i, small, icon, onClick, Set.empty))
  def apply(dom: DomType, size: Size, icon: String, onClick: SyncIO[Unit]): Unmounted[Props, Unit, Unit] =
    Icon(Props(dom, size, icon, onClick, Set.empty))
}
