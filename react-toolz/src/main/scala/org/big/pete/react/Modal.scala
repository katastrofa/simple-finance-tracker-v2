package org.big.pete.react

import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback}
import org.scalajs.dom.html.Div


object Modal {

  trait ModalState {
    val isOpen: Boolean
  }
  val isOpenPath = shapeless.^.isOpen

  trait ModalSupport[P, S <: ModalState] {
//    import japgolly.scalajs.react.vdom.html_<^._

    implicit val isOpenL: isOpenPath.Lens[S, Boolean]
    val $: BackendScope[P, S]

    def closeModal: Callback =
      $.modState(isOpenL().set(_)(false))

    def openModal: Callback =
      $.modState(isOpenL().set(_)(true))

    def wrapContent(state: S, id: String, modalClasses: Array[String], contentWrapperClasses: Array[String])(content: TagMod): VdomTagOf[Div] = {
      <.div(
        ^.id := s"modal-$id",
        ^.tabIndex := 0,
        ^.classSet((Array("modal" -> true, "open" -> state.isOpen) ++ modalClasses.map(_ -> true)).toIndexedSeq: _*),
        <.div(^.cls := (Array("modal-content") ++ contentWrapperClasses).mkString(" "),
          content
        )
      )
    }
  }

  def modalDiv(isOpen: Boolean, id: String, modalClasses: Array[String], contentWrapperClasses: Array[String])(content: TagMod): VdomTagOf[Div] = {
    <.div(
      ^.id := s"modal-$id",
      ^.tabIndex := 0,
      ^.classSet((Array("modal" -> true, "open" -> isOpen) ++ modalClasses.map(_ -> true)).toIndexedSeq: _*),
      <.div(
        ^.cls := (Array("modal-content") ++ contentWrapperClasses).mkString(" "),
        content
      )
    )
  }
}
