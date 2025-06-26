package org.big.pete.tyrian.component

import cats.effect.IO
//import org.big.pete.tyrian.component.Base
import org.scalajs.dom.{FocusEvent, HTMLElement, document, window}
import tyrian.Tyrian.KeyboardEvent
import tyrian.{Cmd, Html as <, Html as ^, Sub}

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt


trait DropDownItem[T] {
  extension (x: T) def key: String
  extension (x: T) def display: String
}

object DropDown extends Base {

  case class Model[T: DropDownItem](
      id: String,
      items: List[T],
      focused: Boolean,
      text: String,
      selected: Option[T],
      browsing: Option[T],
      visible: List[T],
      debouncing: Option[Int]
  )

  sealed trait Msg[T]
  case class NoOp[T]() extends Msg[T]
  case class Activate[T]() extends Msg[T]
  case class Deactivate[T]() extends Msg[T]
  case class Move[T](direction: Int) extends Msg[T]
  case class TextChange[T](text: String) extends Msg[T]
  case class Select[T](item: T) extends Msg[T]
  case class TimePassed[T]() extends Msg[T]
  case class RecalcPosition[T]() extends Msg[T]
  
  final private val DebouncingMillis: Int = 400
  final private val TickInterval: Int = 100


  def init[T: DropDownItem](
      id: String,
      items: List[T],
      selected: Option[T]
  ): Model[T] =
    Model(id, items, false, selected.map(_.display).getOrElse(""), selected, None, items, None)

  def update[T: DropDownItem](msg: Msg[T], m: Model[T]): (Model[T], Cmd[IO, Msg[T]]) = {
    msg match {
      case NoOp() =>
        m -> Cmd.None
      case Activate() =>
        m.copy(focused = true) -> updateUlPosition(m.id, browsingIndex(m.browsing, m), None)
      case Deactivate() =>
        m.copy(focused = false) -> Cmd.None
      case Move(direction) =>
        handleMove(direction, m)
      case TextChange(text) =>
        val visible = filterItems(text, m.items)
        m.copy(browsing = None, text = text, visible = visible) ->
          updateUlPosition(m.id, browsingIndex(None, m), None)

      case Select(item) =>
        m.copy(
          selected = Some(item),
          text = item.display,
          focused = false,
          browsing = None
        ) -> Cmd.None
      case TimePassed() =>
        handleTick(m)
      case RecalcPosition() =>
        m.copy(debouncing = Some(DropDown.DebouncingMillis)) -> Cmd.None
    }
  }
  
  def updateItems[T: DropDownItem](items: List[T], m: Model[T]): (Model[T], Cmd[IO, Msg[T]]) = {
    val selected = m.selected.flatMap(item => items.find(_ == item))
    val browsing = m.browsing.flatMap(item => items.find(_ == item))
    val (visible, text) = (selected, m.selected, m.text) match {
      case (None, Some(item), text) if item.display == text =>
        items -> ""
      case (_, _, text) =>
        filterItems(text, items) -> text
    }
    
    m.copy(items = items, selected = selected, browsing = browsing, text = text) ->
      updateUlPosition(m.id, browsingIndex(browsing, m), None)
  }

  def view[T: DropDownItem](m: Model[T], label: String, tabIndex: Int, extraClasses: List[String]): <[Msg[T]] = {
    val ulClasses = (if (m.focused) List("visible") else List
      .empty[String]) ++ List("dropdown-content", "autocomplete-content")
    
    <.div(
      ^.id := m.id,
      ^.cls := (List("input-field") ++ extraClasses).mkString(" ")
    )(
      <.input(
        ^.`type` := "text",
        ^.value := m.text,
        ^.cls := "autocomplete",
        ^.onFocus(Activate[T]()),
        ^.onInput(str => TextChange[T](str)),
        ^.onKeyDown(processKey(m)).noPreventDefault.noStopImmediatePropagation.noStopPropagation,
        ^.onEvent[FocusEvent, Msg[T]]("blur", handleBlur[T](m.id))
      ),
      <.ul(
        ^.id := s"${m.id}-ul",
        ^.cls := ulClasses.mkString(" "),
        ^.tabIndex := tabIndex
      )(
        m.visible.map(item => displayItem(item, m.text, m.browsing))
      ),
      <.label(^.`for` := m.id, setClass(Set("active" -> (m.text.nonEmpty || m.focused))))(label)
    )
  }


  private def handleMove[T: DropDownItem](direction: Int, m: Model[T]): (Model[T], Cmd[IO, Msg[T]]) = {
    val browsing = m.browsing match {
      case Some(item) =>
        val index = m.visible.indexOf(item) + direction
        val newIndex = index match {
          case i if i < 0 => m.visible.length - 1
          case i if i >= m.visible.length => 0
          case _ => index
        }
        Some(m.visible(newIndex))
      case None =>
        if (direction > 0) m.visible.headOption else m.visible.lastOption
    }

    m.copy(browsing = browsing) -> updateUlPosition(m.id, browsingIndex(browsing, m), Some(direction))
  }

  private def handleTick[T: DropDownItem](m: Model[T]): (Model[T], Cmd[IO, Msg[T]]) = {
    m.debouncing match {
      case Some(remaining) if remaining <= 0 =>
        m.copy(debouncing = None) -> updateUlPosition(m.id, browsingIndex(m.browsing, m), None)
      case Some(remaining) =>
        m.copy(debouncing = Some(remaining - DropDown.TickInterval)) -> Cmd.None
      case None =>
        m -> Cmd.None
    }
  }

  private def handleBlur[T: DropDownItem](id: String)(evt: FocusEvent): Msg[T] = {
    evt.relatedTarget match {
      case el: HTMLElement if isChild(el, id) =>
        NoOp[T]()
      case _ =>
        Deactivate[T]()
    }
  }

  private def processKey[T: DropDownItem](m: Model[T])(e: KeyboardEvent): Msg[T] = {
    e.key match {
      case "ArrowUp" =>
        e.preventDefault()
        Move[T](-1)
      case "ArrowDown" =>
        e.preventDefault()
        Move[T](1)
      case "Enter" =>
        e.preventDefault()
        Select(m.browsing.getOrElse(m.items.head))
      case "Escape" =>
        e.preventDefault()
        Deactivate[T]()
      case _ =>
        NoOp[T]()
    }
  }

  /// Filter items in the list
  private def filterItems[T: DropDownItem](text: String, items: List[T]): List[T] = {
    def search(searchables: List[String])(item: (T, String)): Boolean =
      searchables.forall(searchable => item._2.contains(searchable))

    val searchFn = search(prepareSearchables(text))
    items.map(item => item -> item.display.toLowerCase)
      .filter(item => searchFn(item))
      .map(_._1)
  }

  private def prepareSearchables(searchString: String): List[String] =
    searchString.trim
      .split("\\s").toList
      .filter(_.nonEmpty)
      .map(_.toLowerCase)

  private def browsingIndex[T: DropDownItem](browsing: Option[T], m: Model[T]): Option[Int] =
    browsing.flatMap { item =>
      val i = m.visible.indexOf(item)
      if (i >= 0) Some(i) else None
    }


  private def displayItem[T: DropDownItem](item: T, text: String, browsing: Option[T]): <[Msg[T]] = {
    val displayText = item.display
    val ranges = prepareSearchables(text)
      .map(search(displayText))
      .sortBy(_._1)
    val highlights = if (ranges.nonEmpty) mergeRanges(ranges.head, ranges.tail, List.empty) else List.empty
    val textSplits = splitText(highlights, displayText, List.empty).map {
      case (str, false) => <.text(str)
      case (str, true) => <.span(^.cls := "highlight")(str)
    }

    <.li(
      ^.cls := (if (browsing.contains(item)) "active" else ""),
      ^.dataAttr("key", item.key),
      ^.onClick(Select(item))
    )(
      <.span(textSplits *)
    )
  }

  private def search(text: String)(str: String): (Int, Int) = {
    val index = text.toLowerCase.indexOf(str)
    index -> (index + str.length)
  }

  @tailrec
  private def mergeRanges(current: (Int, Int), rest: List[(Int, Int)], acc: List[(Int, Int)]): List[(Int, Int)] = {
    rest match {
      case Nil => acc ++ List(current)
      case head :: tail =>
        if (current._2 < head._1)
          mergeRanges(head, tail, acc ++ List(current))
        else
          mergeRanges((current._1, Math.max(current._2, head._2)), tail, acc)
    }
  }

  @tailrec
  private def splitText(
      highlights: List[(Int, Int)],
      text: String,
      acc: List[(String, Boolean)]
  ): List[(String, Boolean)] = {
    highlights match {
      case Nil => if (text.isEmpty) acc else acc ++ List(text -> false)
      case head :: tail =>
        val parts = if (head._1 > 0)
          List(text.slice(0, head._1) -> false, text.slice(head._1, head._2) -> true)
        else
          List(text.slice(0, head._2) -> true)
        splitText(tail.map(r => (r._1 - head._2) -> (r._2 - head._2)), text.slice(head._2, text.length), acc ++ parts)
    }
  }


  private def updateUlPosition(
      id: String,
      browsing: Option[Int],
      lastDirection: Option[Int]
  ): Cmd.SideEffect[IO, Unit] = Cmd.SideEffect {
    Option(document.getElementById(s"$id-ul")).foreach { ulRaw =>
      val ul = ulRaw.asInstanceOf[HTMLElement]
      val visibleItems = ul.children.length
      val itemHeight = ul.children.headOption
        .map(_.getBoundingClientRect().height.toInt)
        .filter(_ > 0)
        .getOrElse(38)

      val requestedHeight = visibleItems * itemHeight
      Option(document.getElementById(id)).foreach { el =>
        val rect = el.getBoundingClientRect()

        val aboveSize = rect.top.toInt
        val belowTop = rect.top.toInt + rect.height.toInt - 7
        val belowSize = window.innerHeight.toInt - belowTop

        val (top, height) = (requestedHeight, belowSize, aboveSize) match {
          case (rh, bs, _) if bs >= rh =>
            belowTop -> requestedHeight
          case (rh, bs, as) if bs >= as =>
            belowTop -> bs
          case (rh, _, as) =>
            if (as >= rh) rect.top.toInt - rh -> rh else 0 -> as
        }

        ul.style.left = s"${rect.x.toInt}px"
        ul.style.top = s"${top}px"
        ul.style.width = s"${rect.width.toInt}px"
        ul.style.height = s"${height}px"

        if (browsing.isDefined && requestedHeight > height) {
          if (ul.scrollTop > browsing.get * itemHeight || ul.scrollTop + height < (browsing.get + 1) * itemHeight) {
            val topScrollPosition = Math.min(browsing.get * itemHeight, requestedHeight - height)
            val bottomScrollPosition = Math.max(0, (browsing.get + 1) * itemHeight - height)
            ul.scrollTop = lastDirection match {
              case Some(1) => bottomScrollPosition
              case _ => topScrollPosition
            }
          }
        }
      }
    }
  }


  private def windowResize[T: DropDownItem](): Sub[IO, Msg[T]] = Sub.fromEvent("resize", window) { _ =>
    Option(RecalcPosition[T]())
  }
  private def documentScroll[T: DropDownItem](): Sub[IO, Msg[T]] = Sub.fromEvent("scroll", document) { _ =>
    Option(RecalcPosition[T]())
  }

  private def tick[T: DropDownItem](m: Model[T]): Sub[IO, Msg[T]] = {
    m.debouncing match {
      case Some(_) =>
        Sub.every[IO](DropDown.TickInterval.millis, s"dropdown-${m.id}")
          .map(_ => TimePassed[T]())
      case _ =>
        Sub.None
    }
  }

  def subscriptions[T: DropDownItem](model: Model[T]): Sub[IO, Msg[T]] =
    Sub.Batch[IO, Msg[T]](List(windowResize(), documentScroll(), tick(model)))

}

