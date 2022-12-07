package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import cats.implicits.toFoldableOps
import doobie.ConnectionIO
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import doobie.Fragments.in
import org.big.pete.sft.db.domain.Balance
import org.big.pete.sft.domain.{Transaction, TransactionTracking}

import java.time.LocalDate


object Transactions {
  import org.big.pete.sft.db.domain.Implicits._

  def changeCategory(oldCat: Int, newCat: Int, accountId: Int): ConnectionIO[Int] =
    sql"""UPDATE transactions AS t JOIN categories AS c ON t.category = c.id
         SET t.category = $newCat WHERE t.category = $oldCat AND c.account = $accountId""".update.run

  def deleteForCategory(cat: Int): ConnectionIO[Int] =
    sql"""DELETE FROM transactions WHERE category = $cat""".update.run

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


  def getTransaction(id: Int): ConnectionIO[Option[Transaction]] =
    sql"SELECT * FROM transactions WHERE id = $id".query[Transaction].option

  def listTransactions(accountId: Int, start: LocalDate, end: LocalDate): ConnectionIO[List[Transaction]] = {
    sql"""SELECT t.* FROM transactions AS t
         JOIN money_accounts AS m
            ON t.money_account = m.id
         WHERE t.date >= $start AND t.date <= $end AND m.account = $accountId
        """.query[Transaction].to[List]
  }

  def addTransaction(trans: Transaction): ConnectionIO[Int] =
    sql"""INSERT INTO transactions (
                date, type, amount, description, category, money_account, tracking, dest_amount, dest_money_account, owner
            ) VALUE (
                ${trans.date}, ${trans.transactionType}, ${trans.amount}, ${trans.description}, ${trans.categoryId},
                ${trans.moneyAccount}, ${trans.tracking}, ${trans.destinationAmount}, ${trans.destinationMoneyAccountId},
                ${trans.owner}
            )""".update.withUniqueGeneratedKeys[Int]("id")

  def editTransaction(trans: Transaction): ConnectionIO[Int] =
    sql"""UPDATE transactions
            SET date = ${trans.date}, type = ${trans.transactionType}, amount = ${trans.amount},
                description = ${trans.description}, category = ${trans.categoryId}, money_account = ${trans.moneyAccount},
                tracking = ${trans.tracking}, dest_amount = ${trans.destinationAmount},
                dest_money_account = ${trans.destinationMoneyAccountId}
            WHERE id = ${trans.id}
            LIMIT 1
    """.update.run

  def editTracking(id: Int, tracking: TransactionTracking): ConnectionIO[Int] =
    sql"""UPDATE transactions SET tracking = $tracking WHERE id = $id""".update.run

  def massEditTransactions(ids: NonEmptyList[Int], newCat: Option[Int], newMoneyAccount: Option[Int]): ConnectionIO[Int] = {
    val cat = newCat.map(catId => fr"category = $catId")
    val ma = newMoneyAccount.map(maId => fr"money_account = $maId")
    (fr"UPDATE transactions SET" ++ List(cat, ma).flatten.intercalate(fr",") ++ fr"WHERE" ++ in(fr"id", ids)).update.run
  }

  def deleteTransaction(id: Int): ConnectionIO[Int] =
    sql"""DELETE FROM transactions WHERE id = $id""".update.run

  def deleteTransactions(ids: NonEmptyList[Int]): ConnectionIO[Int] =
    (fr"DELETE FROM transactions WHERE" ++ in(fr"id", ids)).update.run
}
