package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import doobie.Fragments.in
import org.big.pete.sft.db.domain.Balance

import java.time.LocalDate


object Transactions {
  import org.big.pete.sft.db.domain.Implicits._

  def changeCategory(oldCat: Int, newCat: Int, accountId: Int): ConnectionIO[Int] =
    sql"""UPDATE transactions AS t JOIN categories AS c ON t.category = c.id
         SET t.category = $newCat WHERE t.category = $oldCat AND c.account = $accountId""".update.run

  def changeMoneyAccount(oldMA: Int, newMA: Int, accountId: Int): List[ConnectionIO[Int]] = {
    List(
      sql"""UPDATE transactions AS t JOIN money_accounts AS m ON t.money_account = m.id
           SET t.money_account = $newMA WHERE t.money_account = $oldMA AND m.account = $accountId""".update.run,
      sql"""UPDATE transactions AS t JOIN money_accounts AS m ON t.dest_money_account = m.id
           SET t.dest_money_account = $newMA WHERE t.dest_money_account = $oldMA AND m.account = $accountId""".update.run
    )
  }

  def getBalances(moneyAccounts: NonEmptyList[Int], until: LocalDate): ConnectionIO[List[Balance]] = (
    fr"SELECT date, type, amount, money_account, dest_amount, dest_money_account FROM transactions WHERE " ++
      fr"(" ++ in(fr"money_account", moneyAccounts) ++ fr" OR " ++
      in(fr"dest_money_account", moneyAccounts) ++
      fr") AND date <= $until ORDER BY date"
    ).query[Balance].to[List]
}
