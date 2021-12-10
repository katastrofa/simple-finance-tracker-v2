package org.big.pete

import cats.effect.SyncIO
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.MICheckbox
import org.scalajs.dom.{Element, HTMLInputElement, console, document}


object TestMain {
  def main(args: Array[String]): Unit = {
    document.getElementsByClassName("sft-calendar-picker").foreach { el =>
      ReactDatePicker(el.id, ld => SyncIO(console.log(ld.toString)), None, isOpened = false, ReactDatePicker.ExtendedKeyBindings)
        .renderIntoDOM(el)
    }
    document.getElementsByTagName("INPUT")
      .filter(_.classList.contains("indeterminate"))
      .foreach {
        case element: HTMLInputElement =>
          element.indeterminate = true
        case _ =>
      }

    document.getElementsByClassName("sft-checkbox").map { el =>
      val text = el.innerHTML
      val value = el.getAttribute("data-value")
      val key = el.getAttribute("data-key")
      val reactEl = {
        if (el.tagName == "LI")
          MICheckbox(tagMods => <.li(tagMods: _*), value, text, MICheckbox.Status.none, key)
        else if (el.tagName == "TH")
          MICheckbox(tagMods => <.th(tagMods: _*), value, text, MICheckbox.Status.none, key)
        else if (el.tagName == "TD")
          MICheckbox(tagMods => <.td(tagMods: _*), value, text, MICheckbox.Status.none, key)
        else
          MICheckbox(tagMods => <.div(tagMods: _*), value, text, MICheckbox.Status.none, key)
      }
      val parent = el.parentNode.asInstanceOf[Element]

      parent -> reactEl
    }.groupBy(_._1)
      .view.mapValues(_.map(_._2).toVdomArray).toMap
      .foreach { case (parent, arr) =>
        arr.renderIntoDOM(parent)
      }

    ()
  }
}
