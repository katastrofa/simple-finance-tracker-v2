package org.big.pete.sft.front.state

import org.big.pete.sft.domain.TransactionType
import org.big.pete.{BPCookie, CookieAttributes}

import java.time.LocalDate


object CookieStorage {
  import org.big.pete.sft.front.state.Implicits._

  final val SettingsCookieName = "sft-v2-settings"
  final val AddTransactionCookieName = "sft-v2-add-transaction"

  private var browserSettings: BrowserSettings = _
  private var addTransactionSetup: AddTransactionSetup = _

  def defaultSettings: BrowserSettings =
    BrowserSettings(
      LocalDate.now().withDayOfMonth(1),
      LocalDate.now().plusMonths(1L).withDayOfMonth(1).minusDays(1L)
    )

  def defaultAddTransactionSetup: AddTransactionSetup =
    AddTransactionSetup(LocalDate.now(), TransactionType.Expense, None, None, None)

  def getBrowserSettings: BrowserSettings = {
    if (browserSettings == null)
      browserSettings = BPCookie.getObj[BrowserSettings](SettingsCookieName).getOrElse(defaultSettings)

    browserSettings
  }

  def updateBrowserSettings(settings: BrowserSettings): String = {
    browserSettings = settings
    BPCookie.setObj(SettingsCookieName, settings, new CookieAttributes(7, "/"))
  }

  def getAddTransactionSetup: AddTransactionSetup = {
    if (addTransactionSetup == null)
      addTransactionSetup = BPCookie.getObj[AddTransactionSetup](AddTransactionCookieName).getOrElse(defaultAddTransactionSetup)
    addTransactionSetup
  }

  def updateAddTransactionSetup(setup: AddTransactionSetup): String = {
    addTransactionSetup = setup
    BPCookie.setObj(AddTransactionCookieName, setup, new CookieAttributes(3, "/"))
  }
}
