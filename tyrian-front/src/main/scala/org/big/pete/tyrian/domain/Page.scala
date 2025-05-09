package org.big.pete.tyrian.domain

sealed abstract class Page extends Product with Serializable {
  def permalink: String
}

object Page {
  def walletPermalink(active: Page): Option[String] = {
    active match {
      case Wallets => None
      case Transactions(wallet) => Some(wallet)
      case Categories(wallet) => Some(wallet)
      case Accounts(wallet) => Some(wallet)
    }
  }

  case object Wallets extends Page {
    override def permalink: String = "/"
  }
  final case class Transactions(wallet: String) extends Page {
    override def permalink: String = s"/$wallet/transactions"
  }
  final case class Categories(wallet: String) extends Page {
    override def permalink: String = s"/$wallet/categories"
  }
  final case class Accounts(wallet: String) extends Page {
    override def permalink: String = s"/$wallet/accounts"
  }
}
