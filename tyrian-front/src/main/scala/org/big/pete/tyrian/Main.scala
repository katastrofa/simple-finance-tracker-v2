package org.big.pete.tyrian

import cats.effect.IO
import org.big.pete.sft.domain.User
import org.big.pete.tyrian.component.{DatePicker, DropDown, DropDownItem}
import org.big.pete.tyrian.domain.{Msg, Page}
import org.big.pete.tyrian.parts.Sidebar
import org.big.pete.tyrian.sample.Data
import org.big.pete.tyrian.toolz.Routes
import tyrian.{Cmd, Html, Location, Sub, TyrianIOApp}

import java.time.LocalDate
import scala.scalajs.js.annotation.JSExportTopLevel




case class AppModel(
    activePage: Page,
    user: User,
    sidebar: Sidebar.Model,

    from: DatePicker.Model,
    to: DatePicker.Model
    /// test
//    items: List[String],
//    picker1: DatePicker.Model,
//    drop1: DropDown.Model[String]
)

type Model = AppModel

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, Model] {
  import Givens.given

  override def router: Location => Msg = Routes.router

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    initSample() -> Cmd.None
  }

  private def initSample(): Model = {
    AppModel(
      Page.Wallets,
      Data.user,
      Sidebar.init(),
      DatePicker.init("from-date", List("date-select", "date-select-from"), 15, Some(LocalDate.of(2025, 5, 1))),
      DatePicker.init("to-date", List("date-select", "date-select-to"), 15, Some(LocalDate.of(2025, 5, 1))),
    )
  }

  override def update(m: Model): Msg => (Model, Cmd[IO, Msg]) = {
    case Msg.NoOp =>
      m -> Cmd.None
    case Msg.Navigate(page) =>
      m.copy(activePage = page) -> Cmd.None
    case Msg.MenuClick =>
      m.copy(sidebar = m.sidebar.copy(isOpen = !m.sidebar.isOpen)) -> Cmd.None
    case Msg.FromDate(msg) =>
      m.copy(from = DatePicker.update(msg, m.from)) -> Cmd.None
    case Msg.ToDate(msg) =>
      m.copy(to = DatePicker.update(msg, m.to)) -> Cmd.None

//    case Msg.Picker1(msg) =>
//      model.copy(picker1 = DatePicker.update(msg, model.picker1)) -> Cmd.None
//    case Msg.Drop1(msg) =>
//      val result = DropDown.update(items, msg, model.drop1)
//      model.copy(drop1 = result._1) -> result._2.map(Msg.Drop1.apply)
  }

  override def view(model: Model): Html[Msg] = {
    Html.div(
      Html.header(
        Html.div(Html.cls := "navbar-fixed")("bla"),
        Sidebar.view(model.sidebar, model.activePage, model.user)
      ),
      Html.main(
        Html.div(Html.cls := "padding")("Bla")
      )
    )
  }


  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
//    DropDown.subscriptions(model.drop1).map(Msg.Drop1.apply)

  def main(args: Array[String]): Unit =
    launch("sft-full")
}





object Givens {
  given DropDownItem[String] with {
    extension (x: String) def key: String = x.toLowerCase.replaceAll("\\s+", " ").replaceAll("\\s", "-")
    extension (x: String) def display: String = x
  }
}
