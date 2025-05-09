package org.big.pete.tyrian.domain

import org.big.pete.tyrian.component.{DatePicker, DropDown}


enum Msg {
  case NoOp
  case Navigate(page: Page)

  case MenuClick
  case FromDate(msg: DatePicker.Msg)
  case ToDate(msg: DatePicker.Msg)
//  case Picker1(msg: DatePicker.Msg)
//  case Drop1(msg: DropDown.Msg[String])
}
