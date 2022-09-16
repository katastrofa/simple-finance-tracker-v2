package org.big.pete.sft.front.components.header

import japgolly.scalajs.react.callback.CallbackTo
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}
import org.big.pete.datepicker.ReactDatePicker

import java.time.LocalDate


object TopHeader {
  case class Props(
      initialFromDate: Option[LocalDate],
      initialToDate: Option[LocalDate],
      onFromDateChange: LocalDate => CallbackTo[LocalDate],
      onToDateChange: LocalDate => CallbackTo[LocalDate]
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
              ReactDatePicker.DatePicker.withKey("key-date-select-from").apply(
                ReactDatePicker.Props(
                  "date-select-from", "date-select date-select-from", props.onFromDateChange, props.initialFromDate, isOpened = false, Some(42),
                  ReactDatePicker.ExtendedKeyBindings
                )
              ),
              ReactDatePicker.DatePicker.withKey("key-date-select-to").apply(
                ReactDatePicker.Props(
                  "date-select-to", "date-select date-select-to", props.onToDateChange, props.initialToDate, isOpened = false, Some(43),
                  ReactDatePicker.ExtendedKeyBindings
                )
              )
            ).toVdomArray
          )
        )
      )
    }.build
}
