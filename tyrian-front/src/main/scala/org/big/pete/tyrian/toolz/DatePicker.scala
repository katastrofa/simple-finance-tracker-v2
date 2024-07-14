package org.big.pete.tyrian.toolz

import cats.effect.IO
import org.big.pete.tyrian.{MyMsg, NoOp}
import tyrian.{Cmd, Html as h}
import tyrian.Tyrian.KeyboardEvent

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, LocalDate}


final case class KeyBinding(key: String, modifiers: Set[String] = Set.empty[String])

enum DatePickerMovement {
  case PrevDay, NextDay, PrevMonth, NextMonth, PrevWeek, NextWeek, PrevYear, NextYear
}

type DatePickerBindings = Map[DatePickerMovement, KeyBinding]

final case class DatePickerModel(
    id: String,
    cls: List[String],
    tabIndex: Int,
    active: Boolean,
    selected: LocalDate,
    browsing: Option[LocalDate],
    editing: Option[String]
)

enum DatePickerMsg extends MyMsg {
  case Move(id: Int, move: DatePickerMovement)
  case Select(id: Int, date: LocalDate)
  case TextChange(id: Int, text: String)
}


class DatePicker[M](dpId: Int, get: M => DatePickerModel, set: (M, DatePickerModel) => M){
  private def formatDate(date: LocalDate): String =
    date.format(DatePicker.DateFormat)

  private def fillInput(editing: Option[String], browsing: Option[LocalDate], date: LocalDate): String =
    editing.getOrElse(formatDate(browsing.getOrElse(date)))

  def processMsg(model: M): PartialFunction[MyMsg, (M, Cmd[IO, MyMsg])] = {
    case DatePickerMsg.Move(id, move) if id == dpId =>
      val m = get(model)
      val browsingDate = m.browsing.getOrElse(m.selected)
      val newDate = move match {
        case DatePickerMovement.PrevDay => browsingDate.minusDays(1L)
        case DatePickerMovement.NextDay => browsingDate.plusDays(1L)
        case DatePickerMovement.PrevMonth => browsingDate.minusMonths(1L)
        case DatePickerMovement.NextMonth => browsingDate.plusMonths(1L)
        case DatePickerMovement.PrevWeek => browsingDate.minusWeeks(1L)
        case DatePickerMovement.NextWeek => browsingDate.plusWeeks(1L)
        case DatePickerMovement.PrevYear => browsingDate.minusYears(1L)
        case DatePickerMovement.NextYear => browsingDate.plusYears(1L)
      }
      set(model, m.copy(browsing = Some(newDate), editing = None)) -> Cmd.None

    case DatePickerMsg.Select(id, date) if id == dpId =>
      val m = get(model)
      set(model, m.copy(active = false, selected = date, browsing = None, editing = None)) -> Cmd.None

    case DatePickerMsg.TextChange(id, text) if id == dpId =>
      val m = get(model)
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
      set(model, m.copy(editing = Some(text), browsing = Some(newDate))) -> Cmd.None
  }

  def view(origModel: M): h[MyMsg] = {
    val m = get(origModel)

    h.div(h.cls := (m.cls ++ List("input-field")).mkString(" "))(
      Views.modal(s"modal-${m.id}", m.active, List("datepicker-modal"), List("datepicker-container"))(
        h.div(h.cls := "datepicker-calendar-container", h.id := s"date-picker-container-div-${m.id}")(
          h.div(h.cls := "datepicker-calendar")(
            navigationButtons(m.id, m.browsing.getOrElse(m.selected)),

            h.div(h.cls := "datepicker-table-wrapper")(
              h.table(h.cls := "datepicker-table", h.role := "grid")(
                datesHeader,
                h.tbody(
                  generateCalendar(m.browsing.getOrElse(m.selected))
                )
              )
            )
          )
        )
      ),
      h.input(
        h.id := m.id, h.`type` := "text", h.cls := "datepicker", h.tabIndex := m.tabIndex,
        h.value := fillInput(m.editing, m.browsing, m.selected),
        h.onInput(DatePickerMsg.TextChange(dpId, _)),
        h.onKeyDown(handleBackspace(m)),
        h.onKeyPress(processKey(m))
      )
    )
  }

  private def handleBackspace(model: DatePickerModel)(e: KeyboardEvent): MyMsg = {
    e.key match {
      case "Backspace" =>
        model.editing.getOrElse(formatDate(model.browsing.getOrElse(model.selected))) match {
          case x if (x.length == 5 && "[0-9]{4}-".r.matches(x)) || (x.length == 8 && "[0-9]{4}-[0-9]{2}-".r.matches(x)) =>
            e.preventDefault()
            e.stopPropagation()
            DatePickerMsg.TextChange(dpId, x.dropRight(2))
          case _ =>
            NoOp
        }

      case _ =>
        NoOp
    }
  }

  private def processKey(model: DatePickerModel)(e: KeyboardEvent): MyMsg = {
    e.key match {
      case "Enter" =>
        DatePickerMsg.Select(dpId, model.browsing.getOrElse(model.selected))
      case "Escape" =>
        e.preventDefault()
        DatePickerMsg.Select(dpId, model.selected)
      case key =>
        checkKeyBinding(DatePicker.DefaultKeyBindings, e) match {
          case Some(mov) =>
            e.preventDefault()
            e.stopPropagation()
            DatePickerMsg.Move(dpId, mov)
          case None =>
            NoOp
        }
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

  private def navigationButtons(id: String, titleDate: LocalDate): h[MyMsg] = {
    import DatePickerMsg.Move
    h.div(h.id := s"datepicker-title-$id", h.cls := "datepicker-controls", h.role := "heading")(
      h.button(h.cls := "year-prev month-prev", h.`type` := "button", h.onClick(Move(dpId, DatePickerMovement.PrevYear)))(
        Views.icon("keyboard_double_arrow_left")
      ),
      h.button(h.cls := "month-prev", h.`type` := "button", h.onClick(Move(dpId, DatePickerMovement.PrevMonth)))(
        Views.icon("keyboard_arrow_left")
      ),
      h.div(h.cls := "selects-container")(
        h.h5(s"${titleDate.getYear}-${titleDate.getMonthValue}")
      ),
      h.button(h.cls := "month-next", h.`type` := "button", h.onClick(Move(dpId, DatePickerMovement.NextMonth)))(
        Views.icon("keyboard_arrow_right")
      ),
      h.button(h.cls := "year-next month-next", h.`type` := "button", h.onClick(Move(dpId, DatePickerMovement.NextYear)))(
        Views.icon("keyboard_double_arrow_right")
      )
    )
  }

  private def datesHeader: h[MyMsg] = {
    h.thead(
      h.tr(
        DatePicker.Days.map { case (_, name, abbr) =>
          h.th(h.scope := "col")(h.abbr(h.title := name)(abbr))
        }.toList
      )
    )
  }

  private def generateCalendar(date: LocalDate): List[h[MyMsg]] = {
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

  private def calendarWeekLine(selected: Int)(start: LocalDate): h[MyMsg] = {
    val now = LocalDate.now()
    val days = Range(0, 7).map { i =>
      val date = start.plusDays(((0 - start.getDayOfWeek.getValue) % 7).toLong + i)
      val classes = List(
        "is-today" -> (date.compareTo(now) == 0),
        "is-selected" -> (date.getDayOfMonth == selected)
      ).filter(_._2).map(_._1).mkString(" ")

      if (start.getMonthValue == date.getMonthValue) {
        h.td(h.cls := classes)(
          h.button(
            h.cls := "datepicker-day-button",
            h.`type` := "button",
            h.onClick(DatePickerMsg.Select(dpId, date))
          )(h.text(date.getDayOfMonth.toString))
        )
      } else {
        h.td(h.cls := s"is-empty empty-day-$i")(h.text(""))
      }
    }

    h.tr(h.cls := "datepicker-row")(days*)
  }
}

private object DatePicker {
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
}
