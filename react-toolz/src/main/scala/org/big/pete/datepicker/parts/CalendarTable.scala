package org.big.pete.datepicker.parts

import cats.effect.SyncIO
import japgolly.scalajs.react.{CtorType, ReactMouseEventFromHtml, ScalaFnComponent}
import japgolly.scalajs.react.component.ScalaFn.Component
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.TableCell

import java.time.{DayOfWeek, LocalDate}


object CalendarTable {
  final val YearAttr = VdomAttr[Int]("data-year")
  final val MonthAttr = VdomAttr[Int]("data-month")
  final val DayAttr = VdomAttr[Int]("data-day")
  final val Days = Seq(
    (DayOfWeek.SUNDAY, "Sunday", "S"),
    (DayOfWeek.MONDAY, "Monday", "M"),
    (DayOfWeek.TUESDAY, "Tuesday", "T"),
    (DayOfWeek.WEDNESDAY, "Wednesday", "W"),
    (DayOfWeek.THURSDAY, "Thursday", "T"),
    (DayOfWeek.FRIDAY, "Friday", "F"),
    (DayOfWeek.SATURDAY, "Saturday", "S")
  )

  case class LineProps(start: LocalDate, selected: Int, onSelectDate: ReactMouseEventFromHtml => SyncIO[Unit])
  case class Props(id: String, selected: LocalDate, onSelectDate: ReactMouseEventFromHtml => SyncIO[Unit])

  val CalendarTableHeader: Component[Unit, CtorType.Nullary] = ScalaFnComponent.apply[Unit] { _ =>
    def expandHeader(day: (DayOfWeek, String, String)): VdomTagOf[TableCell] =
      <.th(^.key := s"ch-${day._1.getValue}", ^.scope := "col", <.abbr(^.title := day._2, day._3))

    <.thead(<.tr(^.key := "header-line", Days.map(expandHeader).toVdomArray))
  }

  val CalendarWeekLine: Component[LineProps, CtorType.Props] = ScalaFnComponent.apply[LineProps] { prop =>
    def buttonGen(date: LocalDate) =
      <.button(^.cls := "datepicker-day-button", ^.`type` := "button",
        YearAttr := date.getYear, MonthAttr := date.getMonthValue, DayAttr := date.getDayOfMonth,
        ^.onClick ==> prop.onSelectDate,
        date.getDayOfMonth.toString
      )

    def tdGen(date: LocalDate, isToday: Boolean) =
      <.td(
        ^.key := s"day-${date.getDayOfMonth}",
        ^.aria.selected := (date.getDayOfMonth == prop.selected),
        ^.classSet("is-today" -> isToday, "is-selected" -> (date.getDayOfMonth == prop.selected)),
        buttonGen(date)
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

  val CalendarTable: Component[Props, CtorType.Props] = ScalaFnComponent.apply[Props] { prop =>
    def generateCalendar(selected: LocalDate): VdomArray = {
      val start = LocalDate.of(selected.getYear, selected.getMonthValue, 1)

      Iterator.iterate(start) { date =>
        if (date.getDayOfWeek == DayOfWeek.SUNDAY)
          date.plusDays(7L)
        else
          date.plusDays(7L - date.getDayOfWeek.getValue)
      }.takeWhile(_.getMonthValue == start.getMonthValue)
        .map { date =>
          CalendarWeekLine.withKey(s"lk-${date.getDayOfYear}")
            .apply(LineProps(date, selected.getDayOfMonth, prop.onSelectDate))
        }.toVdomArray
    }

    <.div(^.cls := "datepicker-table-wrapper",
      <.table(^.cls := "datepicker-table", ^.role.grid, ^.aria.labelledBy := s"datepicker-title-${prop.id}",
        CalendarTableHeader(),
        <.tbody(generateCalendar(prop.selected))
      )
    )
  }
}
