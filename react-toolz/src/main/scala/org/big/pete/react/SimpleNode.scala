package org.big.pete.react

import japgolly.scalajs.react.ScalaFnComponent
import japgolly.scalajs.react.component.ScalaFn
import japgolly.scalajs.react.vdom.html_<^._


object SimpleNode {
  private final val NewNode = ScalaFnComponent.withReuse[String](text => text)
  def apply(text: String): ScalaFn.Unmounted[String] = NewNode(text)
}
