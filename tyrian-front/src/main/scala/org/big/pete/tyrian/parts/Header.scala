package org.big.pete.tyrian.parts

import org.big.pete.tyrian.Model
import org.big.pete.tyrian.component.DatePicker
import org.big.pete.tyrian.domain.Msg
import org.big.pete.tyrian.toolz.Views
import tyrian.{Html as <, Html as ^}


object Header {

  def view(m: Model): <[Msg] = {
    <.div(^.cls := "navbar-fixed")(
      <.nav(^.cls := "navbar")(
        <.div(^.cls := "nav-wrapper center-align center")(
          Views.icon(Views.DomType.Span, Views.Size.Medium, "menu", List("right", "hide-on-large-only", "padme"), Msg.MenuClick),
          DatePicker.view(m.from, "from-date", List("date-select", "date-select-from"), 5).map(Msg.FromDate.apply),
          DatePicker.view(m.to, "to-date", List("date-select", "date-select-to"), 10).map(Msg.ToDate.apply),
        )
      )
    )
  }
}
