package org.big.pete.sft.front.components.header

import cats.effect.SyncIO
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}
import org.big.pete.datepicker.ReactDatePicker

import java.time.LocalDate


object TopHeader {
  case class Props(
      initialFromDate: Option[LocalDate],
      initialToDate: Option[LocalDate],
      onFromDateChange: LocalDate => SyncIO[Unit],
      onToDateChange: LocalDate => SyncIO[Unit]
  )

  val Obj: Component[Props, Unit, Unit, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .render_P { props =>
      <.div(
        ^.cls := "navbar-fixed",
        <.nav(
          ^.cls := "navbar",
          <.div(
            ^.cls := "nav-wrapper center-align center",
            List(
              /// TODO: Hook the date change, load initial date
              ReactDatePicker.DatePicker.withKey("key-date-select-from").apply(
                ReactDatePicker.Props(
                  "date-select-from", props.onFromDateChange, props.initialFromDate, isOpened = false,
                  ReactDatePicker.ExtendedKeyBindings
                )
              ),
              /// TODO: Hook the date change, load initial date
              ReactDatePicker.DatePicker.withKey("key-date-select-to").apply(
                ReactDatePicker.Props(
                  "date-select-to", props.onToDateChange, props.initialToDate, isOpened = false,
                  ReactDatePicker.ExtendedKeyBindings
                )
              )
            ).toVdomArray
          )
        )
      )
    }.build
}
