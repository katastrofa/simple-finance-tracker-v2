package org.big.pete.tyrian.toolz

import cats.effect.IO
import org.big.pete.tyrian.{MyMsg, NoOp}
import org.scalajs.dom.{FocusEvent, HTMLElement, document, window, console}
import tyrian.{Cmd, Html, Sub}
import tyrian.Tyrian.KeyboardEvent

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt


trait DropDownItem[T] {
  extension (x: T) def key: String
  extension (x: T) def display: String
}

final case class DropDownModel[T](
    id: String,
    label: String,
    selected: Option[T],
    tabIndex: Int,
    extraClasses: List[String],
    focused: Boolean,
    browsing: Option[T],
    text: String,
    visible: List[T],
    debouncing: Option[Int]
)

enum DropDownMsg extends MyMsg {
  case Activate(id: Int)
  case Deactivate(id: Int)
  case Move(id: Int, direction: Int)
  case TextChange(id: Int, text: String)
  case Select[T: DropDownItem](id: Int, item: T)
  case TimePassed(id: Int)
  case RecalcPosition(id: Int)
}


class DropDown[M, T: DropDownItem](ddId: Int, get: M => DropDownModel[T], set: (M, DropDownModel[T]) => M) {
  private def browsingIndex(browsing: Option[T], model: DropDownModel[T]): Option[Int] =
    browsing.flatMap { item =>
      val i = model.visible.indexOf(item)
      if (i >= 0) Some(i) else None
    }

  def processMsg(items: List[T], model: M): PartialFunction[MyMsg, (M, Cmd[IO, MyMsg])] = {
    case DropDownMsg.Activate(id) if id == ddId =>
      val m = get(model)
      set(model, m.copy(focused = true)) -> updateUlPosition(m.id, m.visible.length, browsingIndex(m.browsing, m), None)

    case DropDownMsg.Deactivate(id) if id == ddId =>
      set(model, get(model).copy(focused = false)) -> Cmd.None

    case DropDownMsg.Move(id, direction) if id == ddId =>
      val m = get(model)
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
      set(model, m.copy(browsing = browsing)) -> updateUlPosition(m.id, m.visible.length, browsingIndex(browsing, m), Some(direction))

    case DropDownMsg.TextChange(id, text) if id == ddId =>
      val visible = filterItems(text, items)
      val m = get(model)
      set(model, m.copy(browsing = None, text = text, visible = visible)) -> updateUlPosition(m.id, visible.length, browsingIndex(None, m), None)

    case DropDownMsg.Select(id, item: T) if id == ddId =>
      set(model, get(model).copy(selected = Some(item), text = item.display, focused = false, browsing = None, visible = items)) -> Cmd.None

    case DropDownMsg.TimePassed(id) if id == ddId =>
      val m = get(model)
      m.debouncing match {
        case Some(remaining) if remaining <= 0 =>
          set(model, m.copy(debouncing = None)) -> updateUlPosition(m.id, m.visible.length, browsingIndex(m.browsing, m), None)
        case Some(remaining) =>
          set(model, m.copy(debouncing = Some(remaining - DropDown.TickInterval))) -> Cmd.None
        case None =>
          model -> Cmd.None
      }

    case DropDownMsg.RecalcPosition(id) if id == ddId =>
      set(model, get(model).copy(debouncing = Some(DropDown.DebouncingMillis))) -> Cmd.None
  }

  private def prepareSearchables(searchString: String): List[String] =
    searchString.trim
      .split("\\s").toList
      .filter(_.nonEmpty)
      .map(_.toLowerCase)

  private def filterItems(text: String, items: List[T]): List[T] = {
    def search(searchables: List[String])(item: (T, String)): Boolean =
      searchables.forall(searchable => item._2.contains(searchable))

    val searchFn = search(prepareSearchables(text))
    items.map(item => item -> item.display.toLowerCase)
      .filter(item => searchFn(item))
      .map(_._1)
  }

  private def isChild(el: HTMLElement, id: String): Boolean = {
    el.id == id || Option(el.parentElement).exists(isChild(_, id))
  }

  private def handleBlur(id: String)(evt: FocusEvent): MyMsg = {
    console.log("handling blur on my own")

    evt.relatedTarget match {
      case el: HTMLElement if isChild(el, id) =>
        console.log("isChild")
        NoOp
      case _ =>
        console.log("blur deactivate")
        DropDownMsg.Deactivate(ddId)
    }
  }

  private def processKey(model: DropDownModel[T])(e: KeyboardEvent): MyMsg = {
    e.key match {
      case "ArrowUp" =>
        e.preventDefault()
        DropDownMsg.Move(ddId, -1)
      case "ArrowDown" =>
        e.preventDefault()
        DropDownMsg.Move(ddId, 1)
      case "Enter" =>
        e.preventDefault()
        DropDownMsg.Select(ddId, model.browsing.getOrElse(model.visible.head))
      case "Escape" =>
        e.preventDefault()
        DropDownMsg.Deactivate(ddId)
      case _ =>
        NoOp
    }
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
  private def splitText(highlights: List[(Int, Int)], text: String, acc: List[(String, Boolean)]): List[(String, Boolean)] = {
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

  private def displayItem(item: T, text: String, browsing: Option[T]): Html[MyMsg] = {
    val displayText = item.display
    val ranges = prepareSearchables(text)
      .map(search(displayText))
      .sortBy(_._1)
    val highlights = if (ranges.nonEmpty) mergeRanges(ranges.head, ranges.tail, List.empty) else List.empty
    val textSplits = splitText(highlights, displayText, List.empty).map {
      case (str, false) => Html.text(str)
      case (str, true) => Html.span(Html.cls := "highlight")(str)
    }

    Html.li(
      Html.cls := (if (browsing.contains(item)) "active" else ""),
      Html.onClick(DropDownMsg.Select(ddId, item))
    )(
      Html.span(textSplits*)
    )
  }

  def view(items: List[T], origModel: M): Html[MyMsg] = {
    val model = get(origModel)
    val ulClasses = (if (model.focused) List("visible") else List.empty[String]) ++ List("dropdown-content", "autocomplete-content")

    Html.div(
      Html.id := model.id,
      Html.`class` := (List("input-field") ++ model.extraClasses).mkString(" ")
    )(
      Html.input(
        Html.`type` := "text",
        Html.value := model.text,
        Html.cls := "autocomplete",
        Html.onFocus(DropDownMsg.Activate(ddId)),
        Html.onInput(str => DropDownMsg.TextChange(ddId, str)),
        Html.onKeyDown(processKey(model)).noPreventDefault.noStopImmediatePropagation.noStopPropagation,
        Html.onEvent[FocusEvent, MyMsg]("blur", handleBlur(model.id))
      ),
      Html.ul(
        Html.id := s"${model.id}-ul",
        Html.`class` := ulClasses.mkString(" "),
        Html.tabIndex := model.tabIndex
      )(
        model.visible.map(item => displayItem(item, model.text, model.browsing))
      )
    )
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

  private val windowResize: Sub[IO, MyMsg] = Sub.fromEvent("resize", window) { _ =>
    Option(DropDownMsg.RecalcPosition(ddId))
  }
  private val documentScroll: Sub[IO, MyMsg] = Sub.fromEvent("scroll", document) { _ =>
    Option(DropDownMsg.RecalcPosition(ddId))
  }
  private def tick(model: M): Sub[IO, MyMsg] = {
    get(model).debouncing match {
      case Some(_) =>
        Sub.every[IO](DropDown.TickInterval.millis, "tick")
          .map(_ => DropDownMsg.TimePassed(ddId))
      case _ =>
        Sub.None
    }
  }

  def subscriptions(model: M): Sub[IO, MyMsg] =
    Sub.Batch[IO, MyMsg](List(windowResize, documentScroll, tick(model)))

}

object DropDown {
  final private val DebouncingMillis: Int = 250
  final private val TickInterval: Int = 50
}
