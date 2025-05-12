package org.big.pete.tyrian.parts

import org.big.pete.sft.domain.Wallet
import org.big.pete.tyrian.component.{TextInput, TextInputMsg}
import org.big.pete.tyrian.domain.Page
import org.big.pete.tyrian.toolz.Views.MBIcon
import org.big.pete.tyrian.toolz.{Views, createPermalink}
import tyrian.Html as h


final case class WalletsModel(
    isModalOpen: Boolean,
    editing: Option[Wallet],
    nameInput: TextInput.Model,
    permalinkInput: TextInput.Model
)
enum WalletsMsg {
  case OpenModalAdd
  case OpenModalEdit(wallet: Wallet)

  /// Modal window
  case NameInputMsg(msg: TextInput.Msg)
  case PermalinkInputMsg(msg: TextInput.Msg)
  case ConfirmClick
  case CancelClick
}

object Wallets {

  type Model = WalletsModel
  type Msg = WalletsMsg

  def init(): Model =
    WalletsModel(false, None, TextInput.init(""), TextInput.init(""))

  def update(msg: Msg, m: Model): Model = {
    msg match {
      case WalletsMsg.OpenModalAdd =>
        m.copy(isModalOpen = true, editing = None)
      case WalletsMsg.OpenModalEdit(wallet) =>
        m.copy(
          isModalOpen = true,
          editing = Some(wallet),
          nameInput = TextInput.init(wallet.name),
          permalinkInput = TextInput.init(wallet.permalink)
        )
        
      case WalletsMsg.PermalinkInputMsg(msg) =>
        m.copy(permalinkInput = TextInput.update(msg, m.permalinkInput))
      case WalletsMsg.NameInputMsg(TextInputMsg.TextChange(text)) =>
        val permalink = createPermalink(text)
        m.copy(
          nameInput = TextInput.update(TextInputMsg.TextChange(text), m.nameInput),
          permalinkInput = m.permalinkInput.copy(text = permalink)
        )
      case WalletsMsg.NameInputMsg(msg) =>
        m.copy(nameInput = TextInput.update(msg, m.nameInput))

      case WalletsMsg.ConfirmClick =>
        init()
      case WalletsMsg.CancelClick =>
        init()
    }
  }

  def view(m: Model, wallets: List[Wallet]): h[Msg] = {
    Views.tableWrap(
      "wallets-table",
      h.div(h.id := "wallet-modal", Views.setClass(Set("modal" -> true, "open" -> m.isModalOpen)))(""),
      headerPart,
      walletsPart(wallets),
      headerPart,
      h.a(h.cls := "waves-effect waves-light btn nice", h.onClick(WalletsMsg.OpenModalAdd))(
        Views.icon("add"),
        h.text("Add")
      )
    )
  }

  private def headerPart: h[Msg] = {
    h.tr(
      h.th(h.cls := "hide-on-small-only id")("ID"),
      h.th(h.cls := "name")("Name"),
      h.th(h.cls := "permalink")("Permalink")
    )
  }

  private def walletsPart(wallets: List[Wallet]): h[Msg] = {
    h.tbody(
      wallets.map(walletPart)
    )
  }

  private def walletPart(wallet: Wallet): h[Msg] = {
    h.tr(
      h.td(h.cls := "hide-on-small-only id right-align")(wallet.id.toString),
      h.td(h.cls := "name")(
        Views.icon(Views.DomType.I, Views.Size.Small, "edit", List("right"), WalletsMsg.OpenModalEdit(wallet)),
        h.a(h.href := Page.Transactions(wallet.permalink).permalink)(
          wallet.name
        )
      ),
      h.td(h.cls := "permalink")(wallet.permalink)
    )
  }

  private def modalPart(m: Model): h[Msg] = {
    val (buttonLabel, buttonIcon) = m.editing.map(_ => "Add" -> MBIcon.Add).getOrElse("Edit" -> MBIcon.Edit)

    Views.modal("wallet-modal", m.isModalOpen)(
      h.div(h.cls := "container")(
        h.form(
          h.div(h.cls := "row")(
            TextInput
              .view(m.nameInput, "wallet-name", "Name", 20, List("col", "s12"))
              .map(msg => WalletsMsg.NameInputMsg(msg))
          ),
          h.div(h.cls := "row")(
            TextInput
              .view(m.permalinkInput, "wallet-permalink", "Permalink", 21, List("col", "s12"))
              .map(msg => WalletsMsg.PermalinkInputMsg(msg))
          ),
          h.div(h.cls := "row")(
            Views.modalButtons(buttonLabel, buttonIcon, 25, WalletsMsg.ConfirmClick, WalletsMsg.CancelClick)
          )
        )
      )
    )
  }
}
