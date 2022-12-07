package org.big.pete

import shapeless._
//import shapeless.syntax._
//import shapeless.ops.record._


object Shit extends App {

  trait MyBase extends OpenFamily[MyBase] {
    val isOpen: Boolean

    case class BaseFields(isOpen: Boolean)
    def baseFields = BaseFields(isOpen)
    def baseCopy(base: BaseFields): MyBase
  }

//  case class Picker(isOpen: Boolean, classes: String) extends MyBase {
//    def baseCopy(base: BaseFields) = this merge base
//  }

  trait OpenFamily[T] {
    type BaseFields
    def baseFields: BaseFields
    def baseCopy(base: BaseFields): T
  }




  trait ModalBase {
    val isOpen: Boolean
  }

  case class PickerFun(isOpen: Boolean, cls: String) extends ModalBase

  val openPath = ^.isOpen

  trait Kokot[S <: ModalBase] {
    implicit val openL: openPath.Lens[S, Boolean]

    def close(myState: S): S = {
      openL().set(myState)(false)
    }
  }

  class PickerKokot(implicit val openL: openPath.Lens[PickerFun, Boolean]) extends Kokot[PickerFun]

  val picifuz = new PickerKokot
  val state = PickerFun(true, "kokot")
  val newS = picifuz.close(state)

  println(newS)
}
