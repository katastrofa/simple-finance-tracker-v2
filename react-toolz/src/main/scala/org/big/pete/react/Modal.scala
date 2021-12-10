package org.big.pete.react

import cats.effect.SyncIO
import japgolly.scalajs.react.BackendScope
import org.scalajs.dom.html.Div


object Modal {

  trait ModalState {
    val isOpen: Boolean
  }
  val isOpenPath = shapeless.^.isOpen

  trait ModalSupport[P, S <: ModalState] {
    import japgolly.scalajs.react.vdom.html_<^._

    implicit val isOpenL: isOpenPath.Lens[S, Boolean]
    val $: BackendScope[P, S]

    def closeModal: SyncIO[Unit] =
      $.modState(isOpenL().set(_)(false))

    def openModal: SyncIO[Unit] =
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
}
