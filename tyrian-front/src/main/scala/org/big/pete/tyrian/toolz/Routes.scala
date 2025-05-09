package org.big.pete.tyrian.toolz

import org.big.pete.tyrian.domain.{Msg, Page}
import tyrian.Location


object Routes {
  private final val WalletSublinksRegex = "([a-zA-Z0-9-_]+)/(transactions|categories|accounts)".r
  
  def router: Location => Msg = {
    case loc: Location.Internal =>
      loc.pathName match {
        case "/" => Msg.Navigate(Page.Wallets)
        case WalletSublinksRegex(permalink, subPage) =>
          subPage match {
            case "transactions" => Msg.Navigate(Page.Transactions(permalink))
            case "categories" => Msg.Navigate(Page.Categories(permalink))
            case "accounts" => Msg.Navigate(Page.Accounts(permalink))
          }
        case _ =>
          Msg.NoOp
      }
    case loc: Location.External =>
      Msg.NoOp
  }
}
