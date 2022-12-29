package org.big.pete

import japgolly.scalajs.react.callback.Callback
import org.scalajs.dom.window

import scala.collection.mutable


trait DelayedInvocation {
  private val invocations: mutable.Map[String, Int] = mutable.Map.empty[String, Int]

  def executeWithDelay(key: String, timeout: Double, fn: Callback): Callback = Callback {
    invocations.get(key).foreach(id => window.clearTimeout(id))
    invocations += key -> window.setTimeout(() => fn.runNow(), timeout)
  }

  def cancelExecutions(): Callback = Callback {
    invocations.foreach { case (_, id) => window.clearTimeout(id) }
    invocations.clear()
  }

  def cancelExecution(key: String): Callback = Callback {
    invocations.get(key).foreach(id => window.clearTimeout(id))
    invocations -= key
  }
}
