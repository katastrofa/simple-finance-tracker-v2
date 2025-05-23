package org.big.pete.tyrian.parts

import org.big.pete.sft.domain.Wallet
import org.big.pete.tyrian.component.{TextInput, TextInputMsg}
import org.big.pete.tyrian.domain.Page
import org.big.pete.tyrian.toolz.Views.MBIcon
import org.big.pete.tyrian.toolz.{Views, createPermalink}
import tyrian.{Html as <, Html as ^}


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

object WalletsPage {

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

  def view(m: Model, wallets: List[Wallet]): <[Msg] = {
    Views.tableWrap(
      "wallets-table",
      <.div(^.id := "wallet-modal", Views.setClass(Set("modal" -> true, "open" -> m.isModalOpen)))(modalPart(m)),
      headerPart,
      walletsPart(wallets),
      headerPart,
      <.a(^.cls := "waves-effect waves-light btn nice", ^.onClick(WalletsMsg.OpenModalAdd))(
        Views.icon("add"),
        <.text("Add")
      )
    )
  }

  private def headerPart: <[Msg] = {
    <.tr(
      <.th(^.cls := "hide-on-small-only id")("ID"),
      <.th(^.cls := "name")("Name"),
      <.th(^.cls := "permalink")("Permalink")
    )
  }

  private def walletsPart(wallets: List[Wallet]): <[Msg] = {
    <.tbody(
      wallets.map(walletPart)
    )
  }

  private def walletPart(wallet: Wallet): <[Msg] = {
    <.tr(
      <.td(^.cls := "hide-on-small-only id right-align")(wallet.id.toString),
      <.td(^.cls := "name")(
        Views.icon(Views.DomType.I, Views.Size.Small, "edit", List("right"), WalletsMsg.OpenModalEdit(wallet)),
        <.a(^.href := Page.Transactions(wallet.permalink).permalink)(
          wallet.name
        )
      ),
      <.td(^.cls := "permalink")(wallet.permalink)
    )
  }

  private def modalPart(m: Model): <[Msg] = {
    val (buttonLabel, buttonIcon) = m.editing.map(_ => "Add" -> MBIcon.Add).getOrElse("Edit" -> MBIcon.Edit)

    Views.modal("wallet-modal", m.isModalOpen)(
      <.div(^.cls := "container")(
        <.form(
          <.div(^.cls := "row")(
            TextInput
              .view(m.nameInput, "wallet-name", "Name", 20, List("col", "s12"))
              .map(msg => WalletsMsg.NameInputMsg(msg))
          ),
          <.div(^.cls := "row")(
            TextInput
              .view(m.permalinkInput, "wallet-permalink", "Permalink", 21, List("col", "s12"))
              .map(msg => WalletsMsg.PermalinkInputMsg(msg))
          ),
          <.div(^.cls := "row")(
            Views.modalButtons(buttonLabel, buttonIcon, 25, WalletsMsg.ConfirmClick, WalletsMsg.CancelClick)
          )
        )
      )
    )
  }
}
