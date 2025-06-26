package org.big.pete.tyrian.component.inputs

import org.big.pete.tyrian.toolz.{fDecimal, parseAmount}
import tyrian.Tyrian


object MoneyTextBox extends InputsBase {

  case class Model(text: String, value: BigDecimal, hasFocus: Boolean, strict: Boolean) extends BaseModel
  type Msg = BaseMsg

  def init(value: BigDecimal, strict: Boolean = true): Model =
    Model(fDecimal(value), value, false, strict)

  def update(msg: Msg, m: Model): Model = {
    msg match {
      case BaseMsg.NoOp =>
        m
      case BaseMsg.TextChange(text) =>
        m.copy(text = text)
      case BaseMsg.ReceivedFocus =>
        m.copy(hasFocus = true)
      case BaseMsg.LostFocus =>
        if (m.strict)
          parseValue(m).copy(hasFocus = false)
        else
          m.copy(hasFocus = false)

      case BaseMsg.EnterPress =>
        parseValue(m)
    }
  }

  private def parseValue(m: Model): Model = {
    parseAmount(m.text).map { newValue =>
      m.copy(text = fDecimal(newValue), value = newValue)
    }.getOrElse(m.copy(text = fDecimal(m.value)))
  }
}
