package org.big.pete.react

import japgolly.scalajs.react.Ref.{ToScalaComponent, ToVdom}
import japgolly.scalajs.react.{Callback, Ref}
import org.scalajs.dom.html
import org.scalajs.dom.html.Input


trait HasFocus {
  def focus: Callback
}

trait WithInputFocus extends HasFocus {
  protected val inputRef: ToVdom[Input] = Ref.toVdom[html.Input]

  override def focus: Callback =
    inputRef.foreach(_.focus()).async.delayMs(100).toCallback
}

trait WithFocus[P, S, B <: HasFocus] extends HasFocus {
  protected val focusRef: ToScalaComponent[P, S, B] = Ref.toScalaComponent[P, S, B]

  override def focus: Callback =
    focusRef.foreachCB(_.backend.focus).async.delayMs(100).toCallback
}
