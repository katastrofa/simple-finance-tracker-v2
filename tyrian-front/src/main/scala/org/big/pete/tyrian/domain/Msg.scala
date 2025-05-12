package org.big.pete.tyrian.domain

import org.big.pete.tyrian.component.{DatePicker, DropDown}
import org.big.pete.tyrian.parts.Wallets


enum Msg {
  case NoOp
  case Navigate(page: Page)

  case MenuClick
  case FromDate(msg: DatePicker.Msg)
  case ToDate(msg: DatePicker.Msg)
  
  case WalletsPageMsg(msg: Wallets.Msg)
//  case Picker1(msg: DatePicker.Msg)
//  case Drop1(msg: DropDown.Msg[String])
}
