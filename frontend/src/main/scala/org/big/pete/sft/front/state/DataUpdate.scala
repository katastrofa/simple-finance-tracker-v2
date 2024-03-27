package org.big.pete.sft.front.state

import japgolly.scalajs.react.callback.Callback
import org.big.pete.BPJson
import org.big.pete.datepicker.ReactDatePicker
import org.big.pete.sft.domain.{WalletEdit, AddWallet, Category, CategoryDeleteStrategies, EnhancedAccount, FullWallet, Account, AccountCurrency, AccountDeleteStrategy, ShiftStrategy, ShiftStrategyPerCurrency}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.utilz.getWalletPermalink

import java.time.LocalDate


trait DataUpdate extends DataLoad {
  import org.big.pete.sft.domain.Implicits._

  def saveWallet(oldPermalink: Option[String], id: Option[Int], name: String, permalink: String, patrons: List[Int]): Callback = {
    val method = if (id.isDefined) "POST" else "PUT"
    val payload = if (id.isDefined)
      BPJson.write(WalletEdit(oldPermalink.get, id.get, name, permalink, None, patrons))
    else
      BPJson.write(AddWallet(-1, name, permalink, None, patrons))

    ajaxUpdate[FullWallet](
      method,
      "/wallets",
      payload,
      account => $.modState { state =>
        val newWallets = state.wallets.filter(_.id != account.id) ++ List(account)
        state.copy(wallets = newWallets.sortBy(_.name))
      }
    )
  }

  def saveCategory(id: Option[Int], name: String, description: String, parent: Option[Int]): Callback = {
    $.props.flatMap { props =>
      val wallet = getWalletPermalink(props.activePage).getOrElse("")
      val realParent = parent.flatMap(p => if (p == -42) None else Some(p))
      val realDescription = if (description.nonEmpty) Some(description) else None
      val method = if (id.isDefined) "POST" else "PUT"

      ajaxUpdate[Category](
        method,
        "/" + wallet + "/categories",
        BPJson.write(Category(id.getOrElse(-1), name, realDescription, realParent, -1, None)),
        cat => $.modState { state =>
          val newCats = state.categories + (cat.id -> cat)
          state.copy(categories = newCats, categoryTree = CategoryTree.generateTree(newCats.values.toList))
        }
      )
    }
  }

  def deleteCategory(id: Int, moveSubCats: Option[Int], moveTransactions: Option[Int]): Callback = {
    $.props.flatMap { props =>
      val wallet = getWalletPermalink(props.activePage).getOrElse("")
      ajaxUpdate[String](
        "DELETE",
        "/" + wallet + "/categories/" + id.toString,
        BPJson.write(CategoryDeleteStrategies(ShiftStrategy(moveSubCats), ShiftStrategy(moveTransactions))),
        _ => refreshWallet(wallet).toCallback
      )
    }
  }

  def saveAccount(id: Option[Int], name: String, created: LocalDate, currencies: List[AccountCurrency]): Callback = {
    $.props.flatMap { props =>
      $.state.flatMap { state =>
        val wallet = getWalletPermalink(props.activePage).getOrElse("")
        val method = if (id.isDefined) "POST" else "PUT"
        val accountId = id.getOrElse(-1)

        ajaxUpdate[EnhancedAccount](
          method,
          "/" + wallet + "/accounts?" +
            "start=" + state.from.format(ReactDatePicker.DateFormat) +
            "&end=" + state.to.format(ReactDatePicker.DateFormat),
          BPJson.write(Account(accountId, name, created, -1, None, currencies)),
          account => $.modState { oldState =>
            oldState.copy(accounts = oldState.accounts + (account.id -> account))
          }
        )
      }
    }
  }

  def deleteAccount(id: Int, strategies: List[ShiftStrategyPerCurrency]): Callback = {
    $.props.flatMap { props =>
      val wallet = getWalletPermalink(props.activePage).getOrElse("")
      ajaxUpdate[String](
        "DELETE",
        "/" + wallet + "/accounts/" + id.toString,
        BPJson.write(AccountDeleteStrategy(strategies)),
        _ => refreshWallet(wallet).toCallback
      )
    }
  }
}
