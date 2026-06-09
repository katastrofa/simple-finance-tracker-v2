package org.big.pete.tyrian.component

import org.big.pete.tyrian.domain.PassModel
import org.big.pete.tyrian.toolz.Views
import org.scalajs.dom.{FocusEvent, HTMLElement, console}
import tyrian.{Html as <, Html as ^}
import tyrian.Tyrian.KeyboardEvent

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, LocalDate}


enum DatePickerMovement {
  case PrevDay
  case NextDay
  case PrevMonth
  case NextMonth
  case PrevWeek
  case NextWeek
  case PrevYear
  case NextYear
}

final case class KeyBinding(key: String, modifiers: Set[String] = Set.empty[String])

type DatePickerBindings = Map[DatePickerMovement, KeyBinding]

final case class DatePickerModel(
    active: Boolean,
    selected: LocalDate,
    browsing: Option[LocalDate],
    editing: Option[String]
) extends PassModel

enum DatePickerMsg {
  case NoOp
  case StopPropagation
  case Activate
  case Deactivate
  case Move(move: DatePickerMovement)
  case Select(date: LocalDate)
  case TextChange(text: String)
}


object DatePicker extends Base {

  type Model = DatePickerModel
  type Msg = DatePickerMsg

  private final val Days = Seq(
    (DayOfWeek.SUNDAY, "Sunday", "S"),
    (DayOfWeek.MONDAY, "Monday", "M"),
    (DayOfWeek.TUESDAY, "Tuesday", "T"),
    (DayOfWeek.WEDNESDAY, "Wednesday", "W"),
    (DayOfWeek.THURSDAY, "Thursday", "T"),
    (DayOfWeek.FRIDAY, "Friday", "F"),
    (DayOfWeek.SATURDAY, "Saturday", "S")
  )
  private final val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private final val AllModifiers = Set("Alt", "Control", "Meta", "Shift")

  private final val DefaultKeyBindings: DatePickerBindings = Map(
    DatePickerMovement.PrevDay -> KeyBinding("ArrowLeft"),
    DatePickerMovement.NextDay -> KeyBinding("ArrowRight"),
    DatePickerMovement.PrevMonth -> KeyBinding("ArrowLeft", Set("Shift")),
    DatePickerMovement.NextMonth -> KeyBinding("ArrowRight", Set("Shift")),
    DatePickerMovement.PrevWeek -> KeyBinding("ArrowUp"),
    DatePickerMovement.NextWeek -> KeyBinding("ArrowDown"),
    DatePickerMovement.PrevYear -> KeyBinding("ArrowLeft", Set("Alt")),
    DatePickerMovement.NextYear -> KeyBinding("ArrowRight", Set("Alt"))
  )

  def init(selected: Option[LocalDate]): Model =
    DatePickerModel(false, selected.getOrElse(LocalDate.now()), None, None)

  def update(msg: Msg, m: Model): Model = {
    msg match {
      case DatePickerMsg.Activate =>
        m.copy(active = true)
      case DatePickerMsg.Deactivate =>
        m.copy(active = false, browsing = None, editing = None)
      case DatePickerMsg.Move(direction) =>
        handleMove(direction, m)
      case DatePickerMsg.Select(date) =>
        m.copy(active = false, selected = date, browsing = None, editing = None)
      case DatePickerMsg.TextChange(text) =>
        handleTextChange(text, m)
      case DatePickerMsg.NoOp =>
        m
      case DatePickerMsg.StopPropagation =>
        m
    }
  }

  def view(m: Model, id: String, cls: List[String], tabIndex: Int): <[Msg] = {
    <.div(^.id := wrapId(id), ^.cls := (cls ++ List("input-field")).mkString(" "))(
      Views.modal(s"modal-$id", m.active, List("datepicker-modal"), List("datepicker-container"))(
        <.div(^.cls := "datepicker-calendar-container", ^.id := s"date-picker-container-div-$id")(
          <.div(^.cls := "datepicker-calendar")(
            navigationButtons(id, m.browsing.getOrElse(m.selected)),

            <.div(^.cls := "datepicker-table-wrapper")(
              <.table(^.cls := "datepicker-table", ^.role := "grid")(
                datesHeader,
                <.tbody(
                  generateCalendar(m.browsing.getOrElse(m.selected))
                )
              )
            )
          )
        )
      ),
      <.input(
        ^.id := id, ^.`type` := "text", ^.cls := "datepicker", ^.tabIndex := tabIndex,
        ^.value := fillInput(m.editing, m.browsing, m.selected),
        ^.onFocus(DatePickerMsg.Activate),
        ^.onInput(DatePickerMsg.TextChange(_)),
        ^.onKeyDown(handleBackspace(m)).noPreventDefault.noStopPropagation.noStopImmediatePropagation,
        ^.onKeyPress(processKey(m)).noPreventDefault.noStopPropagation.noStopImmediatePropagation,
        ^.onEvent[FocusEvent, Msg]("blur", handleBlur(wrapId(id)))
      )
    )
  }

  private def wrapId(id: String): String =
    s"dp-wrap-$id"

  private def formatDate(date: LocalDate): String =
    date.format(DatePicker.DateFormat)

  private def fillInput(editing: Option[String], browsing: Option[LocalDate], date: LocalDate): String =
    editing.getOrElse(formatDate(browsing.getOrElse(date)))

  private def handleMove(direction: DatePickerMovement, m: Model): Model = {
    val browsingDate = m.browsing.getOrElse(m.selected)
    val newDate = direction match {
      case DatePickerMovement.PrevDay => browsingDate.minusDays(1L)
      case DatePickerMovement.NextDay => browsingDate.plusDays(1L)
      case DatePickerMovement.PrevMonth => browsingDate.minusMonths(1L)
      case DatePickerMovement.NextMonth => browsingDate.plusMonths(1L)
      case DatePickerMovement.PrevWeek => browsingDate.minusWeeks(1L)
      case DatePickerMovement.NextWeek => browsingDate.plusWeeks(1L)
      case DatePickerMovement.PrevYear => browsingDate.minusYears(1L)
      case DatePickerMovement.NextYear => browsingDate.plusYears(1L)
    }
    m.copy(browsing = Some(newDate), editing = None)
  }

  private def handleTextChange(text: String, m: Model): Model = {
    val newDate = text match {
      case x if "[0-9]{4}".r.matches(x) =>
        LocalDate.parse(x + "-01-01")
      case x if "[0-9]{4}-[0-9]{2}".r.matches(x) =>
        LocalDate.parse(x + "-01")
      case x if "[0-9]{4}-[0-9]{2}-[0-9]{2}".r.matches(x) =>
        LocalDate.parse(x)
      case _ =>
        m.browsing.getOrElse(m.selected)
    }
    m.copy(editing = Some(text), browsing = Some(newDate))
  }

  private def handleBlur(id: String)(evt: FocusEvent): Msg = {
    evt.relatedTarget match {
      case el: HTMLElement if isChild(el, id) =>
        DatePickerMsg.NoOp
      case _ =>
        DatePickerMsg.Deactivate
    }
  }

  private def handleBackspace(m: Model)(e: KeyboardEvent): Msg = {
    val msg = e.key match {
      case "Backspace" =>
        m.editing.getOrElse(formatDate(m.browsing.getOrElse(m.selected))) match {
          case x if (x.length == 5 && "[0-9]{4}-".r.matches(x)) || (x.length == 8 && "[0-9]{4}-[0-9]{2}-".r.matches(x)) =>
            DatePickerMsg.TextChange(x.dropRight(2))
          case _ =>
            DatePickerMsg.NoOp
        }

      case "Escape" =>
        DatePickerMsg.Deactivate

      case key =>
        checkKeyBinding(DatePicker.DefaultKeyBindings, e) match {
          case Some(mov) =>
            DatePickerMsg.Move(mov)
          case None =>
            DatePickerMsg.NoOp
        }
    }

    stopPropagation(msg, e)
  }

  private def processKey(model: DatePickerModel)(e: KeyboardEvent): Msg = {
    console.log("processKey")
    val msg = e.key match {
      case "Enter" =>
        DatePickerMsg.Select(model.browsing.getOrElse(model.selected))

      case key =>
        if (key.matches("[0-9-]")) DatePickerMsg.NoOp else DatePickerMsg.StopPropagation
    }

    stopPropagation(msg, e)
  }

  private def stopPropagation(msg: Msg, e: KeyboardEvent): Msg = {
    msg match {
      case DatePickerMsg.NoOp =>
        msg
      case _ =>
        e.preventDefault()
        e.stopPropagation()
        e.stopImmediatePropagation()
        msg
    }
  }

  private def checkKeyBinding(bindings: DatePickerBindings, e: KeyboardEvent): Option[DatePickerMovement] = {
    bindings.find { case (mov, binding) =>
      binding.key == e.key && matchModifiers(binding.modifiers, e)
    }.map(_._1)
  }

  private def matchModifiers(modifiers: Set[String], e: KeyboardEvent): Boolean = {
    val eventModifiers = List("Alt" -> e.altKey, "Control" -> e.ctrlKey, "Meta" -> e.metaKey, "Shift" -> e.shiftKey)
      .filter(_._2)
      .map(_._1)
      .toSet
    modifiers == eventModifiers
  }

  private def navigationButtons(id: String, titleDate: LocalDate): <[Msg] = {
    import DatePickerMsg.Move
    <.div(^.id := s"datepicker-title-$id", ^.cls := "datepicker-controls", ^.role := "heading")(
      <.button(^.cls := "year-prev month-prev", ^.`type` := "button", ^.onClick(Move(DatePickerMovement.PrevYear)))(
        Views.icon("keyboard_double_arrow_left")
      ),
      <.button(^.cls := "month-prev", ^.`type` := "button", ^.onClick(Move(DatePickerMovement.PrevMonth)))(
        Views.icon("keyboard_arrow_left")
      ),
      <.div(^.cls := "selects-container")(
        <.h5(s"${titleDate.getYear}-${titleDate.getMonthValue}")
      ),
      <.button(^.cls := "month-next", ^.`type` := "button", ^.onClick(Move(DatePickerMovement.NextMonth)))(
        Views.icon("keyboard_arrow_right")
      ),
      <.button(^.cls := "year-next month-next", ^.`type` := "button", ^.onClick(Move(DatePickerMovement.NextYear)))(
        Views.icon("keyboard_double_arrow_right")
      )
    )
  }

  private def datesHeader: <[Msg] = {
    <.thead(
      <.tr(
        DatePicker.Days.map { case (_, name, abbr) =>
          <.th(^.scope := "col")(<.abbr(^.title := name)(abbr))
        }.toList
      )
    )
  }

  private def generateCalendar(date: LocalDate): List[<[Msg]] = {
    def step(date: LocalDate): LocalDate = {
      if (date.getDayOfWeek == DayOfWeek.SUNDAY) date.plusDays(7L)
      else date.plusDays(7L - date.getDayOfWeek.getValue)
    }

    val start = LocalDate.of(date.getYear, date.getMonthValue, 1)
    Iterator.iterate(start)(step)
      .takeWhile(_.getMonthValue == start.getMonthValue)
      .map(calendarWeekLine(date.getDayOfMonth))
      .toList
  }

  private def calendarWeekLine(selected: Int)(start: LocalDate): <[Msg] = {
    val now = LocalDate.now()
    val days = Range(0, 7).map { i =>
      val date = start.plusDays(((0 - start.getDayOfWeek.getValue) % 7).toLong + i)
      val classes = List(
        "is-today" -> (date.compareTo(now) == 0),
        "is-selected" -> (date.getDayOfMonth == selected)
      ).filter(_._2).map(_._1).mkString(" ")

      if (start.getMonthValue == date.getMonthValue) {
        <.td(^.cls := classes)(
          <.button(
            ^.cls := "datepicker-day-button",
            ^.`type` := "button",
            ^.onClick(DatePickerMsg.Select(date))
          )(<.text(date.getDayOfMonth.toString))
        )
      } else {
        <.td(^.cls := s"is-empty empty-day-$i")(<.text(""))
      }
    }

    <.tr(^.cls := "datepicker-row")(days*)
  }
}
