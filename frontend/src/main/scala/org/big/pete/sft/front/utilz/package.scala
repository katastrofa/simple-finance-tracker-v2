package org.big.pete.sft.front

import org.big.pete.sft.domain.Account
import org.big.pete.sft.front.SftMain.SftPages


package object utilz {
  def getAccount(page: SftPages, accounts: List[Account]): Option[Account] = page match {
    case SftMain.AccountsSelectionPage => None
    case SftMain.TransactionsPage(account) => accounts.find(_.permalink == account)
    case SftMain.CategoriesPage(account) => accounts.find(_.permalink == account)
    case SftMain.MoneyAccountsPage(account) => accounts.find(_.permalink == account)
  }

  def getAccountPermalink(page: SftPages): Option[String] = page match {
    case SftMain.AccountsSelectionPage => None
    case SftMain.TransactionsPage(account) => Some(account)
    case SftMain.CategoriesPage(account) => Some(account)
    case SftMain.MoneyAccountsPage(account) => Some(account)
  }

  def getAccountId(page: SftPages, accounts: List[Account]): Option[Int] =
    getAccount(page, accounts).map(_.id)
}
