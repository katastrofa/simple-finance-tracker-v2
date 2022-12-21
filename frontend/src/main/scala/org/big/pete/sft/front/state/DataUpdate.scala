package org.big.pete.sft.front.state

import japgolly.scalajs.react.callback.Callback
import org.big.pete.BPJson
import org.big.pete.sft.domain.{Account, AccountEdit, Category, CategoryDeleteStrategies, EnhancedMoneyAccount, MoneyAccount, MoneyAccountCurrency, MoneyAccountDeleteStrategy, ShiftStrategy, ShiftStrategyPerCurrency}
import org.big.pete.sft.front.domain.CategoryTree
import org.big.pete.sft.front.utilz.getAccountPermalink

import java.time.LocalDate


trait DataUpdate extends DataLoad {
  import org.big.pete.sft.domain.Implicits._

  def saveAccount(oldPermalink: Option[String], id: Option[Int], name: String, permalink: String): Callback = {
    val method = if (id.isDefined) "POST" else "PUT"
    val payload = if (id.isDefined)
      BPJson.write(AccountEdit(oldPermalink.get, id.get, name, permalink, None))
    else
      BPJson.write(Account(-1, name, permalink, None))

    ajaxUpdate[Account](
      method,
      "/accounts",
      payload,
      account => $.modState { state =>
        val newAccounts = state.accounts.filter(_.id != account.id) ++ List(account)
        state.copy(accounts = newAccounts.sortBy(_.name))
      }
    )
  }

  def saveCategory(id: Option[Int], name: String, description: String, parent: Option[Int]): Callback = {
    $.props.flatMap { props =>
      val account = getAccountPermalink(props.activePage).getOrElse("")
      val realParent = parent.flatMap(p => if (p == -42) None else Some(p))
      val realDescription = if (description.nonEmpty) Some(description) else None
      val method = if (id.isDefined) "POST" else "PUT"

      ajaxUpdate[Category](
        method,
        "/" + account + "/categories",
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
      val account = getAccountPermalink(props.activePage).getOrElse("")
      ajaxUpdate[String](
        "DELETE",
        "/" + account + "/categories/" + id.toString,
        BPJson.write(CategoryDeleteStrategies(ShiftStrategy(moveSubCats), ShiftStrategy(moveTransactions))),
        _ => refreshAccount(account).toCallback
      )
    }
  }

  def saveMoneyAccount(id: Option[Int], name: String, created: LocalDate, currencies: List[MoneyAccountCurrency]): Callback = {
    $.props.flatMap { props =>
      val account = getAccountPermalink(props.activePage).getOrElse("")
      val method = if (id.isDefined) "POST" else "PUT"
      val maId = id.getOrElse(-1)

      ajaxUpdate[EnhancedMoneyAccount](
        method,
        "/" + account + "/money-accounts",
        BPJson.write(MoneyAccount(maId, name, created, -1, None, currencies)),
        ma => $.modState { oldState =>
          oldState.copy(moneyAccounts = oldState.moneyAccounts + (ma.id -> ma))
        }
      )
    }
  }

  def deleteMoneyAccount(id: Int, strategies: List[ShiftStrategyPerCurrency]): Callback = {
    $.props.flatMap { props =>
      val account = getAccountPermalink(props.activePage).getOrElse("")
      ajaxUpdate[String](
        "DELETE",
        "/" + account + "/money-accounts/" + id.toString,
        BPJson.write(MoneyAccountDeleteStrategy(strategies)),
        _ => refreshAccount(account).toCallback
      )
    }
  }
}
