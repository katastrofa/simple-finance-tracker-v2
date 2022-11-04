package org.big.pete.sft.front.state

import org.big.pete.{BPCookie, CookieAttributes}

import java.time.LocalDate


object CookieStorage {
  import org.big.pete.sft.front.state.Implicits._

  final val SettingsCookieName = "sft-v2-settings"
  private var browserSettings: BrowserSettings = _

  def defaultSettings: BrowserSettings =
    BrowserSettings(
      LocalDate.now().withDayOfMonth(1),
      LocalDate.now().plusMonths(1L).withDayOfMonth(1).minusDays(1L)
    )

  def getBrowserSettings: BrowserSettings = {
    if (browserSettings == null)
      browserSettings = BPCookie.getObj[BrowserSettings](SettingsCookieName).getOrElse(defaultSettings)

    browserSettings
  }

  def updateBrowserSettings(settings: BrowserSettings): String = {
    browserSettings = settings
    BPCookie.setObj(SettingsCookieName, settings, new CookieAttributes(7, "/"))
  }
}
