package org.big.pete.tyrian.component

import org.scalajs.dom.HTMLElement


trait Base {
  def isChild(el: HTMLElement, id: String): Boolean =
    el.id == id || Option(el.parentElement).exists(isChild(_, id))
}
