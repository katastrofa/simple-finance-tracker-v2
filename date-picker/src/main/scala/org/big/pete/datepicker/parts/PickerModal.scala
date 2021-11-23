package org.big.pete.datepicker.parts

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.react.MaterialIcon
import org.scalajs.dom.html.TableCell


object PickerModal {
  case class Props(id: String)
  case class State(year: Int, month: Int, day: Int)

  final val Days = Seq(
    "Sunday" -> "S",
    "Monday" -> "M",
    "Tuesday" -> "T",
    "Wednesday" -> "W",
    "Thursday" -> "T",
    "Friday" -> "F",
    "Saturday" -> "S"
  )

  class Backend($: BackendScope[Props, State]) {
    def expandHeader(day: (String, String)): VdomTagOf[TableCell] =
      <.th(^.scope := "col", <.abbr(^.title := day._1), day._2)

    def getDays(year: Int, month: Int) = {

    }

    def render(prop: Props, state: State) = {
      val titleClass = s"dpick-title-${prop.id}"

      <.div(^.cls := "dpick-calendar-container",
        <.div(^.cls := "dpick-calendar",
          <.div(^.id := titleClass, ^.cls := "dpick-controls", ^.role.heading, ^.aria.live.assertive,
            <.button(^.cls := "year-prev grey-text text-lighten-4", ^.`type` := "button", MaterialIcon("keyboard_double_arrow_left")),
            <.button(^.cls := "month-prev grey-text text-lighten-4", ^.`type` := "button", MaterialIcon("keyboard_arrow_left")),
            <.div(^.cls := "selects-container", <.h5(s"${state.year}-${state.month}")),
            <.button(^.cls := "month-next grey-text text-lighten-4", ^.`type` := "button", MaterialIcon("keyboard_arrow_right")),
            <.button(^.cls := "year-next grey-text text-lighten-4", ^.`type` := "button", MaterialIcon("keyboard_double_arrow_right"))
          ),
          <.div(^.cls := "dpick-table-wrapper",
            <.table(^.cls := "dpick-table", ^.role.grid, ^.aria.labelledBy := titleClass,
              <.thead(<.tr(Days.map(expandHeader): _*)),
              <.tbody()
            )
          )
        )
      )
    }
  }
}
