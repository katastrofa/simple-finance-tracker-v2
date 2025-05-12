package org.big.pete.tyrian.toolz

import tyrian.{Attribute, Html, Html as <, Html as ^}
import tyrian.syntax.*


object Views {
  enum Size(val className: String) {
    case Small extends Size("small")
    case MidMedium extends Size("mid-medium")
    case Medium extends Size("medium")
    case Large extends Size("large")
  }
  
  enum MBIcon(val name: String) {
    case Add extends MBIcon("add_task")
    case Edit extends MBIcon("")
    case Cancel extends MBIcon("highlight_off")
  }

  enum DomType {
    case Span, I
  }

  def modal[T](
    id: String,
    isOpen: Boolean,
    classes: List[String] = List.empty,
    contentClasses: List[String] = List.empty
  )(
    content: Html[T]
  ): Html[T] = {
    val mainClasses = classes ++ List("modal") ++ (if (isOpen) List("open") else List.empty)

    <.div(^.id := id, ^.tabIndex := 0, ^.cls := mainClasses.mkString(" "))(
      <.div(^.cls := (List("modal-content") ++ contentClasses).mkString(" "))(
        content
      )
    )
  }

  def tableWrap[T](
      id: String,
      preTable: Html[T],
      head: Html[T],
      body: Html[T],
      foot: Html[T],
      postTable: Html[T]
  ): Html[T] = {
    <.div(^.cls := "padding")(
      preTable,
      <.table(^.id := id, ^.cls := "striped small sft highlight")(
        <.thead(head),
        body,
        <.tfoot(foot)
      ),
      postTable
    )
  }

  def setClass(classes: Set[(String, Boolean)]): Attribute = {
    val clsString = classes.filter(_._2).map(_._1).mkString(" ")
    ^.cls := clsString
  }
  
  def icon[T](iconName: String): Html[T] = {
    icon(DomType.I, Size.Small, iconName, List.empty)
  }

  def icon[T](iconName: String, classes: List[String]): Html[T] = {
    icon(DomType.I, Size.Small, iconName, classes)
  }

  def icon[T](tag: DomType, size: Size, iconName: String, classes: List[String]): Html[T] = {
    val mainClasses = List("material-icons", size.className) ++ classes

    tag match {
      case DomType.Span => <.span(^.cls := mainClasses.mkString(" "))(iconName)
      case DomType.I => <.i(^.cls := mainClasses.mkString(" "))(iconName)
    }
  }

  def icon[T](tag: DomType, size: Size, iconName: String, classes: List[String], onClick: T): Html[T] = {
    val mainClasses = List("material-icons", size.className) ++ classes

    tag match {
      case DomType.Span => <.span(^.cls := mainClasses.mkString(" "), ^.onClick(onClick))(iconName)
      case DomType.I => <.i(^.cls := mainClasses.mkString(" "), ^.onClick(onClick))(iconName)
    }
  }
  
  def modalButtons[T](firstLabel: String, firstIcon: MBIcon, tabIndex: Int, confirm: T, cancel: T): Html[T] = {
    <.div(^.cls := "row")(
      <.div(^.cls := "col s12 right-align")(
        niceButton(firstLabel, tabIndex, confirm, Some(firstIcon)),
        niceButton("Cancel", tabIndex + 1, cancel, Some(MBIcon.Cancel))
      )
    )
  }
  
  def niceButton[T](label: String, tabIndex: Int, msg: T, iconToUse: Option[MBIcon]): Html[T] = {
    <.button(
      ^.cls := "waves-effect waves-light btn nice",
      ^.`type` := "button",
      ^.tabIndex := tabIndex,
      ^.onClick(msg)
    )(
      iconToUse.map(iconDef => icon(iconDef.name)).orEmpty,
      <.text(label)
    )
  }
}
