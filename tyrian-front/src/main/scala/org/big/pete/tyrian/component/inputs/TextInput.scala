package org.big.pete.tyrian.component.inputs

import org.big.pete.tyrian.component.Base


object TextInput extends InputsBase {

  case class Model(text: String, hasFocus: Boolean) extends BaseModel
  type Msg = BaseMsg

  def init(text: String): Model =
    Model(text, false)
  
  def update(msg: Msg, m: Model): Model = {
    msg match {
      case BaseMsg.NoOp | BaseMsg.EnterPress =>
        m
      case BaseMsg.TextChange(text) =>
        m.copy(text = text)
      case BaseMsg.ReceivedFocus =>
        m.copy(hasFocus = true)
      case BaseMsg.LostFocus =>
        m.copy(hasFocus = false)
    }
  }
}
