package org.big.pete.react

import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html.Div


object Modal {
  case class Props(
      id: String,
      topClasses: Array[String] = Array.empty[String],
      contentClasses: Array[String] = Array.empty[String],
      content: TagMod
  )
  case class State(isOpen: Boolean)

  class Backend($: BackendScope[Props, State]) {
    def render(prop: Props, state: State): VdomTagOf[Div] = {
      <.div(
        ^.id := s"modal-${prop.id}",
        ^.tabIndex := 0,
        ^.classSet(Array("modal" -> true, "open" -> state.isOpen) ++ prop.topClasses.map(_ -> true): _*),
        <.div(^.cls := (Array("modal-content") ++ prop.contentClasses).mkString(" "),
          prop.content
        )
      )
    }
  }

  val Modal: Component[Props, State, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .initialState(State(false))
    .renderBackend[Backend]
    .build

  def apply(
      id: String,
      topClasses: Array[String] = Array.empty[String],
      contentClasses: Array[String] = Array.empty[String],
      content: TagMod
  ): Unmounted[Props, State, Backend] =
    Modal(Props(id, topClasses, contentClasses, content))
}
