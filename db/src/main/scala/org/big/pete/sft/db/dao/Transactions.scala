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

  def changeCategory(oldCat: Int, newCat: Int, wallet: Int): ConnectionIO[Int] =
    sql"""UPDATE transactions AS t JOIN categories AS c ON t.category = c.id
         SET t.category = $newCat WHERE t.category = $oldCat AND c.wallet = $wallet""".update.run

  def deleteForCategory(cat: Int): ConnectionIO[Int] =
    sql"""DELETE FROM transactions WHERE category = $cat""".update.run

  def changeAccount(currency: String, oldAccount: Int, newAccount: Option[Int]): List[ConnectionIO[Int]] = {
    val oldMainFr = fr"(account = $oldAccount AND currency = $currency)"
    val oldDestFr = fr"(dest_account = $oldAccount AND dest_currency = $currency)"

    if (newAccount.isEmpty)
      List((fr"DELETE FROM transactions WHERE" ++ oldMainFr ++ fr"OR" ++ oldDestFr).update.run)
    else
      List(
        (fr"UPDATE transactions SET account = $newAccount WHERE" ++ oldMainFr).update.run,
        (fr"UPDATE transactions SET dest_account = $newAccount WHERE" ++ oldDestFr).update.run
      )
  }

  def getBalances(accounts: NonEmptyList[Int], until: LocalDate): ConnectionIO[List[Balance]] = (
    fr"SELECT date, type, amount, account, currency, dest_amount, dest_account, dest_currency FROM transactions WHERE " ++
      fr"(" ++ in(fr"account", accounts) ++ fr" OR " ++
      in(fr"dest_account", accounts) ++
      fr") AND date <= $until ORDER BY date"
    ).query[Balance].to[List]


  def getTransaction(id: Int): ConnectionIO[Option[Transaction]] =
    sql"SELECT * FROM transactions WHERE id = $id".query[Transaction].option

  def listTransactions(wallet: Int, start: LocalDate, end: LocalDate): ConnectionIO[List[Transaction]] = {
    sql"""SELECT t.* FROM transactions AS t
         JOIN accounts AS m
            ON t.account = m.id
         WHERE t.date >= $start AND t.date <= $end AND m.wallet = $wallet
        """.query[Transaction].to[List]
  }

  def addTransaction(trans: Transaction): ConnectionIO[Int] =
    sql"""INSERT INTO transactions (
                date, type, amount, description, category, account, currency, tracking, dest_amount, dest_account, dest_currency, owner
            ) VALUE (
                ${trans.date}, ${trans.transactionType}, ${trans.amount}, ${trans.description}, ${trans.category},
                ${trans.account}, ${trans.currency}, ${trans.tracking}, ${trans.destinationAmount},
                ${trans.destinationAccount}, ${trans.destinationCurrency}, ${trans.owner}
            )""".update.withUniqueGeneratedKeys[Int]("id")

  def editTransaction(trans: Transaction): ConnectionIO[Int] =
    sql"""UPDATE transactions
            SET date = ${trans.date}, type = ${trans.transactionType}, amount = ${trans.amount},
                description = ${trans.description}, category = ${trans.category}, account = ${trans.account},
                currency = ${trans.currency}, tracking = ${trans.tracking}, dest_amount = ${trans.destinationAmount},
                dest_account = ${trans.destinationAccount}, dest_currency = ${trans.destinationCurrency}
            WHERE id = ${trans.id}
            LIMIT 1
    """.update.run

  def editTracking(id: Int, tracking: TransactionTracking): ConnectionIO[Int] =
    sql"""UPDATE transactions SET tracking = $tracking WHERE id = $id""".update.run

  def massEditTransactions(ids: NonEmptyList[Int], newCat: Option[Int], newAccount: Option[Int]): ConnectionIO[Int] = {
    val cat = newCat.map(catId => fr"category = $catId")
    val account = newAccount.map(maId => fr"account = $maId")
    (fr"UPDATE transactions SET" ++ List(cat, account).flatten.intercalate(fr",") ++ fr"WHERE" ++ in(fr"id", ids)).update.run
  }

  def deleteTransaction(id: Int): ConnectionIO[Int] =
    sql"""DELETE FROM transactions WHERE id = $id""".update.run

  def deleteTransactions(ids: NonEmptyList[Int]): ConnectionIO[Int] =
    (fr"DELETE FROM transactions WHERE" ++ in(fr"id", ids)).update.run
}
