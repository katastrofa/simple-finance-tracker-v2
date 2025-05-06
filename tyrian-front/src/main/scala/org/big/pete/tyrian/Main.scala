package org.big.pete.tyrian

import cats.effect.IO
import org.big.pete.tyrian.domain.{DropDownItem, Msg, ComponentId}
import org.big.pete.tyrian.toolz.{DatePicker, DatePickerModel, DropDown, DropDownModel}
import tyrian.{Cmd, Html, Location, Routing, Sub, TyrianIOApp}

import java.time.LocalDate
import scala.scalajs.js.annotation.JSExportTopLevel




case class AppModel(
    items: List[String],
    dd: DropDownModel[String],
    dpm: DatePickerModel
)

type Model = AppModel

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, Model] {
  import Givens.given

  private val dropDowns: Map[ComponentId, DropDown[Model, ?]] = Map(
    ComponentId.Drop -> new DropDown[Model, String](ComponentId.Drop, _.dd, (m, dd) => m.copy(dd = dd)),
  )
  private val pickers: Map[ComponentId, DatePicker[Model]] = Map(
    ComponentId.Picker -> new DatePicker[Model](ComponentId.Picker, _.dpm, (m, dpm) => m.copy(dpm = dpm))
  )

  override def router: Location => Msg =
    Routing.none(Msg.NoOp)

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val items = List("Google", "Apple", "Amazon", "Samsung", "Larian", "Microsoft", "Big Pete", "kokot", "sulin", "piculienka", "kokotinkaz")
    val dd = DropDownModel[String]("test", "This is my test", items, None, 4, List.empty[String], false, None, "", items, None)
    val dpm = DatePickerModel("dpm-test", List.empty, 10, false, LocalDate.now(), None, None)
    AppModel(items, dd, dpm) -> Cmd.None
  }

  override def update(model: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.DpMove(id, move) =>
      pickers(id).handleMove(move, model)
    case Msg.DpSelect(id, date) =>
      pickers(id).handleSelect(date, model)
    case Msg.DpTextChange(id, text) =>
      pickers(id).handleTextChange(text, model)

    case Msg.DdMove(id, direction) =>
      dropDowns(id).handleMove(direction, model)
    case m: Msg.DdSelect[?] =>
      m.item match {
        case i: String =>
          dropDowns(m.id).asInstanceOf[DropDown[Model, String]].handleSelect(i, model)    
      }
    case Msg.DdActivate(id) =>
      dropDowns(id).handleActivate(model)
    case Msg.DdDeactivate(id) =>
      dropDowns(id).handleDeactivate(model)
    case Msg.DdTextChange(id, text) =>
      dropDowns(id).handleTextChange(text, model)
    case Msg.DdTimePassed(id) =>
      dropDowns(id).handleTick(model)
    case Msg.DdRecalcPosition(id) =>
      dropDowns(id).handleRecalcPosition(model)

    case Msg.NoOp =>
      model -> Cmd.None
  }

  override def view(model: Model): Html[Msg] =
    Html.div(
      Html.button("-"),
      Html.button("+"),
      Html.div(s"Counter: pici"),
      dropDowns.head._2.view(model),
      pickers.head._2.view(model)
    )

  override def subscriptions(model: Model): Sub[IO, Msg] =
    dropDowns.head._2.subscriptions(model)

  def main(args: Array[String]): Unit =
    launch("myapp")
}





object Givens {
  given DropDownItem[String] with {
    extension (x: String) def key: String = x.toLowerCase.replaceAll("\\s+", " ").replaceAll("\\s", "-")
    extension (x: String) def display: String = x
  }
}
