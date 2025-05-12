package org.big.pete.tyrian.component

import tyrian.{Tyrian, Html as <, Html as ^}


case class TextInputModel(text: String, hasFocus: Boolean)
enum TextInputMsg {
  case NoOp
  case EnterPress
  case ReceivedFocus
  case LostFocus
  case TextChange(text: String)
}

object TextInput extends Base {

  type Model = TextInputModel
  type Msg = TextInputMsg

  def init(text: String): Model =
    TextInputModel(text, false)
  
  def update(msg: Msg, m: Model): Model = {
    msg match {
      case TextInputMsg.NoOp | TextInputMsg.EnterPress =>
        m
      case TextInputMsg.TextChange(text) =>
        m.copy(text = text)
      case TextInputMsg.ReceivedFocus =>
        m.copy(hasFocus = true)
      case TextInputMsg.LostFocus =>
        m.copy(hasFocus = false)
    }
  }
  
  def view(m: Model, id: String, label: String, tabIndex: Int, mainCls: List[String]): <[Msg] = {
    <.div(^.cls := (List("input-field") ++ mainCls).mkString(" "))(
      <.input(
        ^.id := id, ^.`type` := "text", ^.tabIndex := tabIndex, ^.value := m.text,
        ^.onChange(text => TextInputMsg.TextChange(text)),
        ^.onKeyPress(handleKeyPress),
        ^.onFocus(TextInputMsg.ReceivedFocus),
        ^.onBlur(TextInputMsg.LostFocus)
      ),
      <.label(^.`for` := id, setClass(Set("active" -> (m.text.nonEmpty || m.hasFocus))))(
        label
      )
    )
  }
  
  private def handleKeyPress(evt: Tyrian.KeyboardEvent): Msg = {
    evt.key match {
      case "Enter" =>
        evt.preventDefault()
        evt.stopPropagation()
        evt.stopImmediatePropagation()
        TextInputMsg.EnterPress
      case _ =>
        TextInputMsg.NoOp
    }
  }
}
