package org.big.pete.tyrian.toolz

import cats.effect.IO
import org.big.pete.tyrian.domain.{DropDownItem, ComponentId, Msg}
import org.scalajs.dom.{FocusEvent, HTMLElement, console, document, window}
import tyrian.{Cmd, Html, Sub}
import tyrian.Tyrian.KeyboardEvent

import scala.annotation.tailrec
import scala.concurrent.duration.DurationInt


final case class DropDownModel[T: DropDownItem](
    id: String,
    label: String,
    items: List[T],
    selected: Option[T],
    tabIndex: Int,
    extraClasses: List[String],
    focused: Boolean,
    browsing: Option[T],
    text: String,
    visible: List[T],
    debouncing: Option[Int]
)

class DropDown[M, T: DropDownItem](ddId: ComponentId, get: M => DropDownModel[T], set: (M, DropDownModel[T]) => M) {
  private def browsingIndex(browsing: Option[T], model: DropDownModel[T]): Option[Int] =
    browsing.flatMap { item =>
      val i = model.visible.indexOf(item)
      if (i >= 0) Some(i) else None
    }

  def handleActivate(model: M): (M, Cmd[IO, Msg]) = {
    val m = get(model)
    set(model, m.copy(focused = true)) -> updateUlPosition(m.id, m.visible.length, browsingIndex(m.browsing, m), None)
  }

  def handleDeactivate(model: M): (M, Cmd[IO, Msg]) =
    set(model, get(model).copy(focused = false)) -> Cmd.None

  def handleMove(direction: Int, model: M): (M, Cmd[IO, Msg]) = {
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

    set(model, m.copy(browsing = browsing)) ->
      updateUlPosition(m.id, m.visible.length, browsingIndex(browsing, m), Some(direction))
  }

  def handleTextChange(text: String, model: M): (M, Cmd[IO, Msg]) = {
    val m = get(model)
    val visible = filterItems(text, m.items)
    set(model, m.copy(browsing = None, text = text, visible = visible)) ->
      updateUlPosition(m.id, visible.length, browsingIndex(None, m), None)
  }

  def handleSelect(item: T, model: M): (M, Cmd[IO, Msg]) = {
    val m = get(model)
    set(model, m.copy(selected = Some(item), text = item.display, focused = false, browsing = None, visible = m.items)) -> Cmd.None
  }

  def handleTick(model: M): (M, Cmd[IO, Msg]) = {
    val m = get(model)
    m.debouncing match {
      case Some(remaining) if remaining <= 0 =>
        set(model, m.copy(debouncing = None)) ->
          updateUlPosition(m.id, m.visible.length, browsingIndex(m.browsing, m), None)

      case Some(remaining) =>
        set(model, m.copy(debouncing = Some(remaining - DropDown.TickInterval))) ->
          Cmd.None

      case None =>
        model -> Cmd.None
    }
  }

  def handleRecalcPosition(model: M): (M, Cmd[IO, Msg]) =
    set(model, get(model).copy(debouncing = Some(DropDown.DebouncingMillis))) -> Cmd.None


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

  private def handleBlur(id: String)(evt: FocusEvent): Msg = {
    console.log("handling blur on my own")

    evt.relatedTarget match {
      case el: HTMLElement if isChild(el, id) =>
        console.log("isChild")
        Msg.NoOp
      case _ =>
        console.log("blur deactivate")
        Msg.DdDeactivate(ddId)
    }
  }

  private def processKey(model: DropDownModel[T])(e: KeyboardEvent): Msg = {
    e.key match {
      case "ArrowUp" =>
        e.preventDefault()
        Msg.DdMove(ddId, -1)
      case "ArrowDown" =>
        e.preventDefault()
        Msg.DdMove(ddId, 1)
      case "Enter" =>
        e.preventDefault()
        Msg.DdSelect(ddId, model.browsing.getOrElse(model.visible.head))
      case "Escape" =>
        e.preventDefault()
        Msg.DdDeactivate(ddId)
      case _ =>
        Msg.NoOp
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

  private def displayItem(item: T, text: String, browsing: Option[T]): Html[Msg] = {
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
      Html.onClick(Msg.DdSelect(ddId, item))
    )(
      Html.span(textSplits*)
    )
  }

  def view(origModel: M): Html[Msg] = {
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
        Html.onFocus(Msg.DdActivate(ddId)),
        Html.onInput(str => Msg.DdTextChange(ddId, str)),
        Html.onKeyDown(processKey(model)).noPreventDefault.noStopImmediatePropagation.noStopPropagation,
        Html.onEvent[FocusEvent, Msg]("blur", handleBlur(model.id))
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

  private val windowResize: Sub[IO, Msg] = Sub.fromEvent("resize", window) { _ =>
    Option(Msg.DdRecalcPosition(ddId))
  }
  private val documentScroll: Sub[IO, Msg] = Sub.fromEvent("scroll", document) { _ =>
    Option(Msg.DdRecalcPosition(ddId))
  }
  private def tick(model: M): Sub[IO, Msg] = {
    get(model).debouncing match {
      case Some(_) =>
        Sub.every[IO](DropDown.TickInterval.millis, "tick")
          .map(_ => Msg.DdTimePassed(ddId))
      case _ =>
        Sub.None
    }
  }

  def subscriptions(model: M): Sub[IO, Msg] =
    Sub.Batch[IO, Msg](List(windowResize, documentScroll, tick(model)))

}

object DropDown {
  final private val DebouncingMillis: Int = 250
  final private val TickInterval: Int = 50
}
