package org.big.pete.tyrian

import cats.effect.IO
import org.big.pete.tyrian.toolz.{DropDown, DropDownItem, DropDownModel}
import tyrian.{Cmd, Html, Location, Routing, Sub, TyrianIOApp}

import scala.scalajs.js.annotation.JSExportTopLevel


trait MyMsg
case object NoOp extends MyMsg
case object Tick extends MyMsg

case class AppModel(
    items: List[String],
    dd: DropDownModel[String]
)

type Model = AppModel

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[MyMsg, Model] {
  import Givens.given

  private val testDD = new DropDown[Model, String](1, _.dd, (m, dd) => m.copy(dd = dd))

  override def router: Location => MyMsg =
    Routing.none(NoOp)

  override def init(flags: Map[String, String]): (Model, Cmd[IO, MyMsg]) = {
    val items = List("Google", "Apple", "Amazon", "Samsung", "Larian", "Microsoft", "Big Pete", "kokot", "sulin", "piculienka", "kokotinkaz")
    val dd = DropDownModel[String]("test", "This is my test", None, 4, List.empty[String], false, None, "", items, None)
    AppModel(items, dd) -> Cmd.None
  }

  override def update(model: Model): MyMsg => (Model, Cmd[IO, MyMsg]) = {
    testDD.processMsg(model.items, model).orElse {
      case NoOp => model -> Cmd.None
    }
  }

  override def view(model: Model): Html[MyMsg] =
    Html.div(
      Html.button("-"),
      Html.button("+"),
      Html.div(s"Counter: pici"),
      testDD.view(model.items, model)
    )

  override def subscriptions(model: Model): Sub[IO, MyMsg] =
    testDD.subscriptions(model)

  def main(args: Array[String]): Unit =
    launch("myapp")
}





object Givens {
  given DropDownItem[String] with {
    extension (x: String) def key: String = x.toLowerCase.replaceAll("\\s+", " ").replaceAll("\\s", "-")
    extension (x: String) def display: String = x
  }
}
