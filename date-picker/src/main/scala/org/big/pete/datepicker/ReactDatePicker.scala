package org.big.pete.datepicker

import japgolly.scalajs.react.BackendScope
import japgolly.scalajs.react.vdom.html_<^._


object ReactDatePicker {
  case class State()
  case class Props(id: String, label: String)

  class Backend($: BackendScope[Props, State]) {
    def render(prop: Props, state: State) = {
      <.div(^.cls := "input-field",
        Modal(),
        <.input(^.id := prop.id, ^.`type` := "text", ^.cls := "dpick"),
        <.label(^.`for` := prop.id, prop.label)
      )
    }
  }
}
