package org.big.pete.tyrian.parts

import cats.effect.IO
import org.big.pete.sft.domain.{ApiResponse, Currencies, User, Wallet, Wallets, Transactions, Accounts, Categories}
import org.big.pete.tyrian.Model
import org.big.pete.tyrian.domain.Msg
import org.big.pete.tyrian.toolz.HttpHelper
import org.big.pete.tyrian.toolz.HttpHelper.{ApiCalls, apiCall}
import tyrian.Cmd


object ApiMsgHelper {
  def parseApiResponseAndUpdate(response: ApiResponse, m: Model): (Model, Cmd[IO, Msg]) = {
    response match {
      case wallet: Wallet =>
        val wallets = (wallet :: m.wallets.filter(_.id != wallet.id)).sortBy(_.id)
        m.copy(wallets = wallets) -> Cmd.None
      case Wallets(wallets) =>
        m.copy(wallets = wallets) -> Cmd.None

      case Currencies(currencies) =>
        m.copy(currencies = currencies) -> Cmd.None

      case usr: User =>
        m.copy(user = usr) -> Cmd.None

      case Transactions(_) => ???
      case Accounts(_) => ???
      case Categories(_) => ???
    }
  }

  def saveWallet(m: Model): (Model, Cmd[IO, Msg]) = {
    val call = if (m.walletsPage.editing.isDefined) ApiCalls.EditWallet else ApiCalls.AddWallet
    val wallet = Wallet(
      m.walletsPage.editing.map(_.id).getOrElse(-1),
      m.walletsPage.nameInput.text,
      m.walletsPage.permalinkInput.text,
      None
    )

    m.copy(walletsPage = WalletsPage.update(WalletsMsg.ConfirmClick, m.walletsPage)) ->
      HttpHelper.apiCall(m.apiBase, call, Some(wallet))
  }

  def initialLoad(m: Model): Cmd.Batch[IO, Msg] = {
    Cmd.Batch(
      apiCall(m.apiBase, ApiCalls.ListWallets, None),
      apiCall(m.apiBase, ApiCalls.ListCurrencies, None),
      apiCall(m.apiBase, ApiCalls.Me, None)
    )
  }
}
