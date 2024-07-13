package org.big.pete.tyrian.toolz

import org.big.pete.tyrian.{MyMsg, NoOp}
import tyrian.Html as h
import tyrian.Tyrian.KeyboardEvent

import java.time.format.DateTimeFormatter
import java.time.{DayOfWeek, LocalDate}


final case class KeyBinding(key: String, modifiers: List[String] = List.empty[String])

final case class KeyBindings(
    prevDay: KeyBinding,
    nextDay: KeyBinding,
    prevMonth: KeyBinding,
    nextMonth: KeyBinding,
    prevWeek: Option[KeyBinding] = None,
    nextWeek: Option[KeyBinding] = None,
    prevYear: Option[KeyBinding] = None,
    nextYear: Option[KeyBinding] = None
)

final case class DatePickerModel(
    id: String,
    cls: List[String],
    tabIndex: Int,
    keyBindings: KeyBindings,
    active: Boolean,
    selected: LocalDate,
    browsing: Option[LocalDate],
    editing: Option[String]
)

enum DatePickerMsg extends MyMsg {
  case Move(id: Int, months: Int, days: Int)
  case Select(id: Int, date: LocalDate)
  case TextChange(id: Int, text: String)
}


class DatePicker[M](dpId: Int, get: M => DatePickerModel, set: (M, DatePickerModel) => M){
  private def formatDate(date: LocalDate): String =
    date.format(DatePicker.DateFormat)

  private def fillInput(editing: Option[String], date: LocalDate): String =
    editing.getOrElse(formatDate(date))

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
        h.value := fillInput(m.editing, m.selected),
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
    
  }

  private def navigationButtons(id: String, titleDate: LocalDate): h[MyMsg] = {
    import DatePickerMsg.Move
    h.div(h.id := s"datepicker-title-$id", h.cls := "datepicker-controls", h.role := "heading")(
      h.button(h.cls := "year-prev month-prev", h.`type` := "button", h.onClick(Move(dpId, -12, 0)))(
        Views.icon("keyboard_double_arrow_left")
      ),
      h.button(h.cls := "month-prev", h.`type` := "button", h.onClick(Move(dpId, -1, 0)))(
        Views.icon("keyboard_arrow_left")
      ),
      h.div(h.cls := "selects-container")(
        h.h5(s"${titleDate.getYear}-${titleDate.getMonthValue}")
      ),
      h.button(h.cls := "month-next", h.`type` := "button", h.onClick(Move(dpId, 1, 0)))(
        Views.icon("keyboard_arrow_right")
      ),
      h.button(h.cls := "year-next month-next", h.`type` := "button", h.onClick(Move(dpId, 12, 0)))(
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
//            h.dataAttr("year", date.getYear.toString),
//            h.dataAttr("month", date.getMonthValue.toString),
//            h.dataAttr("day", date.getDayOfMonth.toString)
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

object DatePicker {
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
}
