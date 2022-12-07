package org.big.pete.react

import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.{CtorType, ReactMouseEvent, Reusability, ScalaFnComponent}
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

  case class Props(
      dom: DomType,
      size: Size,
      icon: String,
      onClick: Callback,
      additionalClasses: Set[String],
      stopPropagation: Boolean = false
  )

  implicit val domTypeReuse: Reusability[DomType] = Reusability.byRefOr_==[DomType]
  implicit val sizeReuse: Reusability[Size] = Reusability.byRefOr_==[Size]
  implicit val propsReuse: Reusability[Props] = Reusability.caseClassExcept[Props]("onClick")


  val Icon: ScalaFn.Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { props =>
    val sizeClass = props.size match {
      case `small` => "small"
      case `midMedium` => "mid-medium"
      case `medium` => "medium"
      case `large` => "large"
    }
    val classes = (Set("material-icons", sizeClass) ++ props.additionalClasses).mkString(" ")

    val updatedOnClick = (evt: ReactMouseEvent) =>
      (if (props.stopPropagation) evt.stopPropagationCB else Callback.empty) >> props.onClick

    props.dom match {
      case `span` => <.span(^.cls := classes, ^.onClick ==> updatedOnClick, props.icon)
      case `i` => <.i(^.cls := classes, ^.onClick ==> updatedOnClick, props.icon)
    }
  }

  def apply(icon: String): ScalaFn.Unmounted[Props] =
    Icon(Props(i, small, icon, Callback.empty, Set.empty))
  def apply(icon: String, additionalClasses: Set[String]): ScalaFn.Unmounted[Props] =
    Icon(Props(i, small, icon, Callback.empty, additionalClasses))
  def apply(icon: String, onClick: Callback): ScalaFn.Unmounted[Props] =
    Icon(Props(i, small, icon, onClick, Set.empty))
  def apply(dom: DomType, size: Size, icon: String, onClick: Callback): ScalaFn.Unmounted[Props] =
    Icon(Props(dom, size, icon, onClick, Set.empty))
}
