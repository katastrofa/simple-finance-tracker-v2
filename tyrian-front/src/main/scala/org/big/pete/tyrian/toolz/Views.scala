package org.big.pete.tyrian.toolz

import tyrian.{Html => h}


object Views {
  enum Size(val className: String) {
    case Small extends Size("small")
    case MidMedium extends Size("mid-medium")
    case Medium extends Size("medium")
    case Large extends Size("large")
  }

  enum DomType {
    case Span, I
  }

  def modal[T](
    id: String,
    isOpen: Boolean,
    classes: List[String],
    contentClasses: List[String]
  )(
    content: h[T]
  ): h[T] = {
    val mainClasses = classes ++ List("modal") ++ (if (isOpen) List("open") else List.empty)

    h.div(h.id := id, h.tabIndex := 0, h.cls := mainClasses.mkString(" "))(
      h.div(h.cls := (List("modal-content") ++ contentClasses).mkString(" "))(
        content
      )
    )
  }

  def icon[T](iconName: String): h[T] = {
    icon(DomType.I, Size.Small, iconName, List.empty)
  }

  def icon[T](iconName: String, classes: List[String]): h[T] = {
    icon(DomType.I, Size.Small, iconName, classes)
  }

  def icon[T](tag: DomType, size: Size, iconName: String, classes: List[String]): h[T] = {
    val mainClasses = List("material-icons", size.className) ++ classes

    tag match {
      case DomType.Span => h.span(h.cls := mainClasses.mkString(" "))(iconName)
      case DomType.I => h.i(h.cls := mainClasses.mkString(" "))(iconName)
    }
  }

  def icon[T](tag: DomType, size: Size, iconName: String, classes: List[String], onClick: T): h[T] = {
    val mainClasses = List("material-icons", size.className) ++ classes

    tag match {
      case DomType.Span => h.span(h.cls := mainClasses.mkString(" "), h.onClick(onClick))(iconName)
      case DomType.I => h.i(h.cls := mainClasses.mkString(" "), h.onClick(onClick))(iconName)
    }
  }
}
