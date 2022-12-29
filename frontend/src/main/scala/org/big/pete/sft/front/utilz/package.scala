package org.big.pete.sft.front

import org.big.pete.sft.front.SftMain.SftPages


package object utilz {
  def getAccountPermalink(page: SftPages): Option[String] = page match {
    case SftMain.AccountsSelectionPage => None
    case SftMain.TransactionsPage(account) => Some(account)
    case SftMain.CategoriesPage(account) => Some(account)
    case SftMain.MoneyAccountsPage(account) => Some(account)
  }

  def parsePermalink(full: String): String =
    full.trim.toLowerCase
      .replaceAll("\\s+", "-")
      .replaceAll("[^a-z0-9_-]+", "_")
}
