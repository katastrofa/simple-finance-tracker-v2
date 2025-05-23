package org.big.pete.tyrian

import cats.effect.IO
import org.big.pete.sft.domain.{Currency, User, UserPermissions, Wallet}
import org.big.pete.tyrian.component.{DatePicker, DropDownItem}
import org.big.pete.tyrian.domain.{Msg, Page}
import org.big.pete.tyrian.parts.{ApiMsgHelper, Header, Sidebar, WalletsMsg, WalletsPage}
import org.big.pete.tyrian.toolz.{CookieStorage, Routes}
import org.scalajs.dom
import tyrian.{Cmd, Html, Location, Sub, TyrianIOApp}

import scala.scalajs.js.annotation.JSExportTopLevel


case class AppModel(
    activePage: Page,

    sidebar: Sidebar.Model,
    walletsPage: WalletsPage.Model,

    from: DatePicker.Model,
    to: DatePicker.Model,

    user: User,
    wallets: List[Wallet],
    currencies: List[Currency],

    apiBase: String,
    httpError: String
)

type Model = AppModel

@JSExportTopLevel("TyrianApp")
object Main extends TyrianIOApp[Msg, Model] {
//  import Givens.given

  override def router: Location => Msg = Routes.router

  override def init(flags: Map[String, String]): (Model, Cmd[IO, Msg]) = {
    val apiBase = flags("apiBase")

    val m = AppModel(
      activePage = Page.Wallets,
      sidebar = Sidebar.init(),
      walletsPage = WalletsPage.init(),
      from = DatePicker.init(Some(CookieStorage.getBrowserSettings.from)),
      to = DatePicker.init(Some(CookieStorage.getBrowserSettings.to)),
      user = User(-1, "", "", UserPermissions(Set.empty, Map.empty, Set.empty)),
      wallets = List.empty,
      currencies = List.empty,
      apiBase = apiBase,
      httpError = ""
    )

    m -> ApiMsgHelper.initialLoad(m)
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

    case Msg.WalletsPageMsg(WalletsMsg.ConfirmClick) =>
      ApiMsgHelper.saveWallet(m)

    case Msg.WalletsPageMsg(msg) =>
      m.copy(walletsPage = WalletsPage.update(msg, m.walletsPage)) -> Cmd.None

    case Msg.HttpError(errMsg) =>
      m.copy(httpError = errMsg) -> Cmd.None
    case Msg.HttpSuccess(response) =>
      ApiMsgHelper.parseApiResponseAndUpdate(response, m)
  }

  override def view(model: Model): Html[Msg] = {
    Html.div(Html.id := "sft-full")(
      Html.header(
        Header.view(model),
        Sidebar.view(model.sidebar, model.activePage, model.user)
      ),
      Html.main(
        model.activePage match {
          case Page.Wallets =>
            WalletsPage.view(model.walletsPage, model.wallets).map(msg => Msg.WalletsPageMsg(msg))
          case Page.Transactions(wallet) =>
            Html.div(Html.cls := "padding")("Bla")
          case Page.Categories(wallet) =>
            Html.div(Html.cls := "padding")("Bla")
          case Page.Accounts(wallet) =>
            Html.div(Html.cls := "padding")("Bla")
        }
      )
    )
  }

  override def subscriptions(model: Model): Sub[IO, Msg] =
    Sub.None
//    DropDown.subscriptions(model.drop1).map(Msg.Drop1.apply)

  def main(args: Array[String]): Unit = {
    val loc = dom.window.location
    val url = loc.protocol + "//" + loc.host + (if (loc.port.matches("^(?:80)?$")) ":" + loc.port else "")
    val baseUrl = url + "/api"
    launch("sft-full", Map("apiBase" -> baseUrl))
  }
}





object Givens {
  given DropDownItem[String] with {
    extension (x: String) def key: String = x.toLowerCase.replaceAll("\\s+", " ").replaceAll("\\s", "-")
    extension (x: String) def display: String = x
  }
}
