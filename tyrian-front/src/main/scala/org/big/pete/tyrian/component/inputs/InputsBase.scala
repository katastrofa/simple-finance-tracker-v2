package org.big.pete.tyrian.component.inputs

import org.big.pete.tyrian.component.Base
import tyrian.{Tyrian, Html as <, Html as ^}


trait BaseModel {
  val text: String
  val hasFocus: Boolean
}

enum BaseMsg {
  case NoOp
  case EnterPress
  case ReceivedFocus
  case LostFocus
  case TextChange(text: String)
}

trait InputsBase extends Base {

  def view(m: BaseModel, id: String, label: String, tabIndex: Int, mainCls: List[String]): <[BaseMsg] = {
    <.div(^.cls := (List("input-field") ++ mainCls).mkString(" "))(
      <.input(
        ^.id := id, ^.`type` := "text", ^.tabIndex := tabIndex, ^.value := m.text,
        ^.onChange(text => BaseMsg.TextChange(text)),
        ^.onKeyPress(handleKeyPress),
        ^.onFocus(BaseMsg.ReceivedFocus),
        ^.onBlur(BaseMsg.LostFocus)
      ),
      <.label(^.`for` := id, setClass(Set("active" -> (m.text.nonEmpty || m.hasFocus))))(
        label
      )
    )
  }

  protected def handleKeyPress(evt: Tyrian.KeyboardEvent): BaseMsg = {
    evt.key match {
      case "Enter" =>
        evt.preventDefault()
        evt.stopPropagation()
        evt.stopImmediatePropagation()
        BaseMsg.EnterPress
      case _ =>
        BaseMsg.NoOp
    }
  }
}
