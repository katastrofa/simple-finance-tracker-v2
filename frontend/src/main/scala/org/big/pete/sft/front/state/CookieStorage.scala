package org.big.pete.sft.front.state

import org.big.pete.sft.domain.TransactionType
import org.big.pete.{BPCookie, CookieAttributes}

import java.time.LocalDate
import scala.collection.mutable


object CookieStorage {
  import org.big.pete.sft.front.state.Implicits._

  final private val SettingsCookieName = "sft-v2-settings"
  final private val AddTransactionCookieName = "sft-v2-add-transaction-{account}"

  private var browserSettings: BrowserSettings = _
  private val addTransactionSetup: mutable.Map[String, AddTransactionSetup] = mutable.Map.empty[String, AddTransactionSetup]

  private def defaultSettings: BrowserSettings =
    BrowserSettings(
      LocalDate.now().withDayOfMonth(1),
      LocalDate.now().plusMonths(1L).withDayOfMonth(1).minusDays(1L)
    )

  private def defaultAddTransactionSetup: AddTransactionSetup =
    AddTransactionSetup(LocalDate.now(), TransactionType.Expense, None, None, None, None, None)

  def getBrowserSettings: BrowserSettings = {
    if (browserSettings == null)
      browserSettings = BPCookie.getObj[BrowserSettings](SettingsCookieName).getOrElse(defaultSettings)

    browserSettings
  }

  def updateBrowserSettings(settings: BrowserSettings): String = {
    browserSettings = settings
    BPCookie.setObj(SettingsCookieName, settings, new CookieAttributes(7, "/"))
  }

  def getAddTransactionSetup(account: String): AddTransactionSetup = {
    if (!addTransactionSetup.contains(account)) {
      val setup = BPCookie.getObj[AddTransactionSetup](AddTransactionCookieName.replace("{account}", account))
        .getOrElse(defaultAddTransactionSetup)
      addTransactionSetup += account -> setup
    }
    addTransactionSetup(account)
  }

  def updateAddTransactionSetup(account: String, setup: AddTransactionSetup): String = {
    addTransactionSetup += account -> setup
    BPCookie.setObj(AddTransactionCookieName.replace("{account}", account), setup, new CookieAttributes(3, "/"))
  }
}
