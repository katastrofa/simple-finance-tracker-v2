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

final case class DropDownModel[T: DropDownItem](
    id: String,
    selected: Option[T],
    focused: Boolean,
    browsing: Option[T],
    text: String,
    visible: List[T],
    debouncing: Option[Int]
)

sealed abstract class DropDownMsg[T: DropDownItem]


object DropDown extends Base {

  private final case class NoOp[T: DropDownItem]() extends DropDownMsg[T]
  private final case class Activate[T: DropDownItem]() extends DropDownMsg[T]
  private final case class Deactivate[T: DropDownItem]() extends DropDownMsg[T]
  private final case class Move[T: DropDownItem](direction: Int) extends DropDownMsg[T]
  private final case class TextChange[T: DropDownItem](text: String) extends DropDownMsg[T]
  private final case class Select[T: DropDownItem](item: T) extends DropDownMsg[T]
  private final case class TimePassed[T: DropDownItem]() extends DropDownMsg[T]
  private final case class RecalcPosition[T: DropDownItem]() extends DropDownMsg[T]

  final private val DebouncingMillis: Int = 400
  final private val TickInterval: Int = 100

  type Model[T] = DropDownModel[T]
  type Msg[T] = DropDownMsg[T]

  def init[T: DropDownItem](
      id: String,
      items: List[T],
      selected: Option[T]
  ): Model[T] =
    DropDownModel[T](id, selected, false, None, selected.map(_.display).getOrElse(""), items, None)

  def update[T: DropDownItem](items: List[T], msg: Msg[T], m: Model[T]): (Model[T], Cmd[IO, Msg[T]]) = {
    msg match {
      case NoOp() =>
        m -> Cmd.None
      case Activate() =>
        m.copy(focused = true) -> updateUlPosition(m.id, m.visible.length, browsingIndex(m.browsing, m), None)
      case Deactivate() =>
        m.copy(focused = false) -> Cmd.None
      case Move(direction) =>
        handleMove(direction, m)
      case TextChange(text) =>
        val visible = filterItems(text, items)
        m.copy(browsing = None, text = text, visible = visible) ->
          updateUlPosition(m.id, visible.length, browsingIndex(None, m), None)

      case Select[T](item) =>
        m.copy(selected = Some(item), text = item.display, focused = false, browsing = None, visible = items) -> Cmd.None
      case TimePassed() =>
        handleTick(m)
      case RecalcPosition() =>
        m.copy(debouncing = Some(DropDown.DebouncingMillis)) -> Cmd.None
    }
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
        ^.onInput(str => TextChange(str)),
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
          case i => i
        }
        Some(m.visible(newIndex))
      case None =>
        if (direction > 0) m.visible.headOption else m.visible.lastOption
    }

    m.copy(browsing = browsing) -> updateUlPosition(m.id, m.visible.length, browsingIndex(browsing, m), Some(direction))
  }

  private def handleTick[T: DropDownItem](m: Model[T]): (Model[T], Cmd[IO, Msg[T]]) = {
    m.debouncing match {
      case Some(remaining) if remaining <= 0 =>
        m.copy(debouncing = None) -> updateUlPosition(m.id, m.visible.length, browsingIndex(m.browsing, m), None)
      case Some(remaining) =>
        m.copy(debouncing = Some(remaining - DropDown.TickInterval)) -> Cmd.None
      case None =>
        m -> Cmd.None
    }
  }

  private def handleBlur[T: DropDownItem](id: String)(evt: FocusEvent): Msg[T] = {
    evt.relatedTarget match {
      case el: HTMLElement if isChild(el, id) =>
        NoOp()
      case _ =>
        Deactivate()
    }
  }

  private def processKey[T: DropDownItem](model: DropDownModel[T])(e: KeyboardEvent): Msg[T] = {
    e.key match {
      case "ArrowUp" =>
        e.preventDefault()
        Move(-1)
      case "ArrowDown" =>
        e.preventDefault()
        Move(1)
      case "Enter" =>
        e.preventDefault()
        Select(model.browsing.getOrElse(model.visible.head))
      case "Escape" =>
        e.preventDefault()
        Deactivate()
      case _ =>
        NoOp()
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

  private def browsingIndex[T: DropDownItem](browsing: Option[T], model: Model[T]): Option[Int] =
    browsing.flatMap { item =>
      val i = model.visible.indexOf(item)
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
      visibleItems: Int,
      browsing: Option[Int],
      lastDirection: Option[Int]
  ): Cmd.SideEffect[IO, Unit] = Cmd.SideEffect {
    Option(document.getElementById(s"$id-ul")).foreach { ulRaw =>
      val ul = ulRaw.asInstanceOf[HTMLElement]
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
    Option(RecalcPosition())
  }
  private def documentScroll[T: DropDownItem](): Sub[IO, Msg[T]] = Sub.fromEvent("scroll", document) { _ =>
    Option(RecalcPosition())
  }

  private def tick[T: DropDownItem](m: Model[T]): Sub[IO, Msg[T]] = {
    m.debouncing match {
      case Some(_) =>
        Sub.every[IO](DropDown.TickInterval.millis, s"dropdown-${m.id}")
          .map(_ => TimePassed())
      case _ =>
        Sub.None
    }
  }

  def subscriptions[T: DropDownItem](model: Model[T]): Sub[IO, Msg[T]] =
    Sub.Batch[IO, Msg[T]](List(windowResize(), documentScroll(), tick(model)))

}

