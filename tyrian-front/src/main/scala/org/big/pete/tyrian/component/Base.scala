package org.big.pete.tyrian.component

import org.scalajs.dom.HTMLElement
import tyrian.{Attribute, Html as ^}


trait Base {
  def setClass(classes: Set[(String, Boolean)]): Attribute = {
    val clsString = classes.filter(_._2).map(_._1).mkString(" ")
    ^.cls := clsString
  }

  def isChild(el: HTMLElement, id: String): Boolean =
    el.id == id || Option(el.parentElement).exists(isChild(_, id))
}
