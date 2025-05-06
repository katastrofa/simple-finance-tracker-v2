package org.big.pete.tyrian.domain

import java.time.LocalDate


enum Msg {
  case NoOp
//  case Tick
  /// DatePicker Messages
  case DpMove(id: ComponentId, move: DatePickerMovement)
  case DpSelect(id: ComponentId, date: LocalDate)
  case DpTextChange(id: ComponentId, text: String)
  /// DropDown Messages
  case DdActivate(id: ComponentId)
  case DdDeactivate(id: ComponentId)
  case DdMove(id: ComponentId, direction: Int)
  case DdTextChange(id: ComponentId, text: String)
  case DdSelect[T: DropDownItem](id: ComponentId, item: T)
  case DdTimePassed(id: ComponentId)
  case DdRecalcPosition(id: ComponentId)
}
