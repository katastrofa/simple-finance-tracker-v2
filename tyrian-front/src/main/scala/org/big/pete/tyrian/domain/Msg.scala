package org.big.pete.tyrian.domain

import cats.effect.IO
import monocle.Lens
import org.big.pete.sft.domain.ApiResponse
import org.big.pete.tyrian.component.{DatePicker, DropDown}
import org.big.pete.tyrian.parts.WalletsPage
import tyrian.Cmd


//enum Msg {
//  case NoOp
//  case Navigate(page: Page)
//
//  case MenuClick
//  case FromDate(msg: DatePicker.Msg)
//  case ToDate(msg: DatePicker.Msg)
//
//  case WalletsPageMsg(msg: WalletsPage.Msg)
//
//  case HttpSuccess(response: ApiResponse)
//  case HttpError(errorMessage: String)
//}


trait Msg

object Msg {
  case object NoOp extends Msg
  case class Navigate(page: Page) extends Msg
  case object MenuClick extends Msg
  case class FromDate(msg: DatePicker.Msg) extends Msg
  case class ToDate(msg: DatePicker.Msg) extends Msg
  case class WalletsPageMsg(msg: WalletsPage.Msg) extends Msg
  case class HttpSuccess(response: ApiResponse) extends Msg
  case class HttpError(errorMessage: String) extends Msg

  case class PassThrough[M](fn: M => M, fnCmd: M => Cmd[IO, Msg]) extends Msg
}