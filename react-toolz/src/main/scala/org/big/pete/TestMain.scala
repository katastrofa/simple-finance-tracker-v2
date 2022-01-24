package org.big.pete

//import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, Reusability}
import japgolly.scalajs.react.callback.CallbackTo
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.DropDown
//import org.big.pete.react.MICheckbox
import org.scalajs.dom.{HTMLInputElement, console, document}


object TestMain {
  case class TestItem(id: Int, name: String, unused: String)
  implicit val testItemReuse: Reusability[TestItem] = Reusability.derive[TestItem]

  def main(args: Array[String]): Unit = {
    document.getElementsByClassName("sft-calendar-picker").foreach { el =>
      ReactDatePicker(el.id, "", ld => CallbackTo { console.log(ld.toString); ld }, None, isOpened = false, ReactDatePicker.ExtendedKeyBindings)
        .renderIntoDOM(el)
    }
    document.getElementsByTagName("INPUT")
      .filter(_.classList.contains("indeterminate"))
      .foreach {
        case element: HTMLInputElement =>
          element.indeterminate = true
        case _ =>
      }

    val ddTestClass = new DropDown[TestItem]
    val props = ddTestClass.Props(
      "test-dropdown",
      "Test Items",
      List(
        TestItem(1, "Google", "elgoog"),
        TestItem(2, "Apple", "shitty stuff"),
        TestItem(3, "Amazon", "everything"),
        TestItem(4, "Samsung", "mobile phones"),
        TestItem(5, "Larian", "Best gaming studio"),
        TestItem(6, "Microsoft", "Billy"),
        TestItem(42, "Big Pete", "jackpot")
      ),
      (x: TestItem) => x.name,
      (x: TestItem) => s"id-${x.id}",
      (x: TestItem) => Callback.log(s"${x.id} - ${x.name} - ${x.unused}"),
      None
    )
    console.log(ddTestClass)
    ddTestClass.component.apply(props).renderIntoDOM(document.getElementById("me-dropdown"))

//    document.getElementsByClassName("sft-checkbox").map { el =>
//      val text = el.innerHTML
//      val value = el.getAttribute("data-value")
//      val key = el.getAttribute("data-key")
//      val reactEl = {
//        if (el.tagName == "LI")
//          MICheckbox(tagMods => <.li(tagMods: _*), value, text, MICheckbox.Status.none, key)
//        else if (el.tagName == "TH")
//          MICheckbox(tagMods => <.th(tagMods: _*), value, text, MICheckbox.Status.none, key)
//        else if (el.tagName == "TD")
//          MICheckbox(tagMods => <.td(tagMods: _*), value, text, MICheckbox.Status.none, key)
//        else
//          MICheckbox(tagMods => <.div(tagMods: _*), value, text, MICheckbox.Status.none, key)
//      }
//      val parent = el.parentNode.asInstanceOf[Element]
//
//      parent -> reactEl
//    }.groupBy(_._1)
//      .view.mapValues(_.map(_._2).toVdomArray).toMap
//      .foreach { case (parent, arr) =>
//        arr.renderIntoDOM(parent)
//      }

    ()
  }
}
