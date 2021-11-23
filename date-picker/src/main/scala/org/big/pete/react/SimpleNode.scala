package org.big.pete.react

import japgolly.scalajs.react.{CtorType, ScalaComponent}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._


object SimpleNode {
  val Node: Component[String, Unit, Unit, CtorType.Props] = ScalaComponent.builder[String]
    .render_P(text => text)
    .build

  def apply(text: String): Unmounted[String, Unit, Unit] = Node(text)
}
