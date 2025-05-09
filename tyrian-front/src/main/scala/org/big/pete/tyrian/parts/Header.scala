package org.big.pete.tyrian.parts

import org.big.pete.tyrian.Model
import org.big.pete.tyrian.component.DatePicker
import org.big.pete.tyrian.domain.Msg
import org.big.pete.tyrian.toolz.Views
import tyrian.Html as h


object Header {

  def view(m: Model): h[Msg] = {
    h.div(h.cls := "navbar-fixed")(
      h.nav(h.cls := "navbar")(
        h.div(h.cls := "nav-wrapper center-align center")(
          Views.icon(Views.DomType.Span, Views.Size.Medium, "menu", List("right", "hide-on-large-only", "padme"), Msg.MenuClick),
          DatePicker.view(m.from).map(Msg.FromDate.apply),
          DatePicker.view(m.to).map(Msg.FromDate.apply),
        )
      )
    )
  }
}
