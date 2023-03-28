package org.big.pete.datepicker.parts

import japgolly.scalajs.react.{CtorType, Reusability, ScalaFnComponent}
import japgolly.scalajs.react.component.ScalaFn.{Component, Unmounted}
import japgolly.scalajs.react.extra.StateSnapshot
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.TableCell

import java.time.{DayOfWeek, LocalDate}


object CalendarTable {
  private final val Days = Seq(
    (DayOfWeek.SUNDAY, "Sunday", "S"),
    (DayOfWeek.MONDAY, "Monday", "M"),
    (DayOfWeek.TUESDAY, "Tuesday", "T"),
    (DayOfWeek.WEDNESDAY, "Wednesday", "W"),
    (DayOfWeek.THURSDAY, "Thursday", "T"),
    (DayOfWeek.FRIDAY, "Friday", "F"),
    (DayOfWeek.SATURDAY, "Saturday", "S")
  )

  case class LineProps(start: LocalDate, selected: StateSnapshot[LocalDate])
  case class Props(id: String, selected: StateSnapshot[LocalDate])

  implicit val calendarLinePropsReuse: Reusability[LineProps] = Reusability.derive[LineProps]
  implicit val calendarTablePropsReuse: Reusability[Props] = Reusability.derive[Props]


  private val CalendarTableHeader: Component[Unit, CtorType.Nullary] = ScalaFnComponent.withReuse[Unit] { _ =>
    def expandHeader(day: (DayOfWeek, String, String)): VdomTagOf[TableCell] =
      <.th(^.key := s"ch-${day._1.getValue}", ^.scope := "col", <.abbr(^.title := day._2, day._3))

    <.thead(<.tr(^.key := "header-line", Days.map(expandHeader).toVdomArray))
  }

  private val CalendarWeekLine: Component[LineProps, CtorType.Props] = ScalaFnComponent.withReuse[LineProps] { prop =>
    def tdGen(date: LocalDate, isToday: Boolean) =
      <.td(
        ^.key := s"day-${date.getDayOfMonth}",
        ^.aria.selected := (date == prop.selected.value),
        ^.classSet("is-today" -> isToday, "is-selected" -> (date == prop.selected.value)),
        <.button(
          ^.cls := "datepicker-day-button",
          ^.`type` := "button",
          ^.onClick --> prop.selected.setState(date),
          date.getDayOfMonth.toString
        )
      )
    def emptyDay(i: Int) =
      <.td(^.key := s"empty-day-$i", ^.cls := s"is-empty empty-day-$i")

    val now = LocalDate.now()
    val days = Range(0, 7).map { i =>
      val date = prop.start.plusDays(((0 - prop.start.getDayOfWeek.getValue) % 7).toLong + i)
      val isToday = date.compareTo(now) == 0
      if (prop.start.getMonthValue == date.getMonthValue) tdGen(date, isToday) else emptyDay(i)
    }.toVdomArray

    <.tr(^.cls := s"datepicker-row", days)
  }

  val CalendarTable: Component[Props, CtorType.Props] = ScalaFnComponent.withReuse[Props] { prop =>
    def generateCalendar(selected: StateSnapshot[LocalDate]): VdomArray = {
      val start = LocalDate.of(selected.value.getYear, selected.value.getMonthValue, 1)

      Iterator.iterate(start) { date =>
        if (date.getDayOfWeek == DayOfWeek.SUNDAY)
          date.plusDays(7L)
        else
          date.plusDays(7L - date.getDayOfWeek.getValue)
      }.takeWhile(_.getMonthValue == start.getMonthValue)
        .map { date =>CalendarWeekLine.withKey(s"lk-${date.getDayOfYear}")(LineProps(date, selected))}.toVdomArray
    }

    <.div(^.cls := "datepicker-table-wrapper",
      <.table(^.cls := "datepicker-table", ^.role.grid, ^.aria.labelledBy := s"datepicker-title-${prop.id}",
        CalendarTableHeader(),
        <.tbody(generateCalendar(prop.selected))
      )
    )
  }

  def apply(id: String, selected: StateSnapshot[LocalDate]): Unmounted[Props] =
    CalendarTable(Props(id, selected))
}
