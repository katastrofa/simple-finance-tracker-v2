package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.MaterialIcon

import java.time.LocalDate


object TopHeader {
  case class Props(
      from: LocalDate,
      to: LocalDate,
      onFromDateChange: LocalDate => CallbackTo[LocalDate],
      onToDateChange: LocalDate => CallbackTo[LocalDate],
      menuClick: Callback
  )

  val component: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      <.div(
        ^.cls := "navbar-fixed",
        <.nav(
          ^.cls := "navbar",
          <.div(
            ^.cls := "nav-wrapper center-align center",
            List(
              MaterialIcon.Icon(MaterialIcon.Props(
                MaterialIcon.span, MaterialIcon.medium, "menu", props.menuClick, Set("right hide-on-large-only padme")
              )),
              ReactDatePicker.DatePicker.withKey("key-date-select-from").apply(
                ReactDatePicker.Props(
                  "date-select-from", "date-select date-select-from", props.onFromDateChange, props.from, isOpened = false, Some(42),
                  ReactDatePicker.ExtendedKeyBindings, Callback.empty
                )
              ),
              ReactDatePicker.DatePicker.withKey("key-date-select-to").apply(
                ReactDatePicker.Props(
                  "date-select-to", "date-select date-select-to", props.onToDateChange, props.to, isOpened = false, Some(43),
                  ReactDatePicker.ExtendedKeyBindings, Callback.empty
                )
              )
            ).toVdomArray
          )
        )
      )
    }.build
}
