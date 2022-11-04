package org.big.pete.react

import japgolly.scalajs.react.Ref.ToScalaComponent
import japgolly.scalajs.react.{Callback, Ref}


trait HasFocus {
  def focus: Callback
}

trait WithFocus[P, S, B <: HasFocus] extends HasFocus {
  protected val focusRef: ToScalaComponent[P, S, B] = Ref.toScalaComponent[P, S, B]

  def focus: Callback =
    focusRef.foreachCB(_.backend.focus).async.delayMs(100).toCallback
}
