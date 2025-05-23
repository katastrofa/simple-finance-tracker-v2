package org.big.pete.tyrian.domain

import org.big.pete.sft.domain.ApiResponse
import org.big.pete.tyrian.component.{DatePicker, DropDown}
import org.big.pete.tyrian.parts.WalletsPage


enum Msg {
  case NoOp
  case Navigate(page: Page)

  case MenuClick
  case FromDate(msg: DatePicker.Msg)
  case ToDate(msg: DatePicker.Msg)
  
  case WalletsPageMsg(msg: WalletsPage.Msg)

  case HttpSuccess(response: ApiResponse)
  case HttpError(errorMessage: String)
}
