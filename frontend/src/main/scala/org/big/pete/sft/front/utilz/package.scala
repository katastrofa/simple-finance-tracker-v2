package org.big.pete.sft.front

import org.big.pete.sft.front.SftMain.SftPages


package object utilz {
  def getWalletPermalink(page: SftPages): Option[String] = page match {
    case SftMain.WalletSelectionPage => None
    case SftMain.TransactionsPage(account) => Some(account)
    case SftMain.CategoriesPage(account) => Some(account)
    case SftMain.AccountsPage(account) => Some(account)
  }

  def parsePermalink(full: String): String =
    full.trim.toLowerCase
      .replaceAll("\\s+", "-")
      .replaceAll("[^a-z0-9_-]+", "_")
}
