package org.big.pete

//import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, Reusability}
import japgolly.scalajs.react.extra.StateSnapshot
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.react.DropDown
import org.big.pete.domain.DDItem
//import org.big.pete.react.DropDown

import java.time.LocalDate
//import org.big.pete.react.MICheckbox
import org.scalajs.dom.{HTMLInputElement, document}


object TestMain {
  case class TestItem(id: Int, name: String, unused: String) extends DDItem {
    override def ddId: String = id.toString
    override def ddDisplayName: String = name
  }

  implicit val testItemReuse: Reusability[TestItem] = Reusability.derive[TestItem]

  def main(args: Array[String]): Unit = {
    var ld = LocalDate.now
    val ssFn: (Option[LocalDate], Callback) => Callback = (dOpt, fn) =>
      Callback.log(dOpt.getOrElse(LocalDate.now).toString()) >> Callback { ld = dOpt.getOrElse(ld) } >> fn

    document.getElementsByClassName("sft-calendar-picker").foreach { el =>
      ReactDatePicker(el.id, StateSnapshot(ld)(ssFn))
        .renderIntoDOM(el)
    }

    document.getElementsByTagName("INPUT")
      .filter(_.classList.contains("indeterminate"))
      .foreach {
        case element: HTMLInputElement =>
          element.indeterminate = true
        case _ =>
      }

    DropDown.apply(
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
      StateSnapshot.apply[Option[TestItem]](None) { (item: Option[Option[TestItem]], fn: Callback) =>
        Callback.log(s"${item.get.get.id} - ${item.get.get.name} - ${item.get.get.unused}") >> fn
      },
      15
    ).renderIntoDOM(document.getElementById("me-dropdown"))

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
