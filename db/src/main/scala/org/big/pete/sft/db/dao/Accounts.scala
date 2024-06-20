package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import cats.implicits.toTraverseOps
import doobie.ConnectionIO
import doobie.Fragments
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import doobie.util.update.Update
import org.big.pete.sft.domain.{Account, AccountCurrency, AccountCurrencyInfo, AccountWithCurrency, PureAccount}


object Accounts {
  def toAccount(list: List[AccountWithCurrency]): Account = {
    val ref = list.head
    Account(ref.id, ref.name, ref.created, ref.wallet, ref.owner, list.map(_.getCurrency))
  }

  def getPureAccount(id: Int, wallet: Int): ConnectionIO[Option[PureAccount]] =
    sql"SELECT * FROM accounts WHERE id = $id AND wallet = $wallet".query[PureAccount].option

  def getAccount(id: Int): ConnectionIO[List[AccountWithCurrency]] = {
    sql"""SELECT m.*, c.id, c.currency, c.start_amount FROM accounts AS m
        JOIN account_currencies AS c
            ON m.id = c.account
        WHERE m.id = $id"""
      .query[AccountWithCurrency].to[List]
  }

  def listAccounts(wallet: Int): ConnectionIO[List[AccountWithCurrency]] = {
    sql"""SELECT m.*, c.id, c.currency, c.start_amount FROM accounts AS m
          JOIN account_currencies AS c
              ON m.id = c.account
         WHERE wallet = $wallet"""
      .query[AccountWithCurrency].to[List]
  }

  def listCurrenciesForAccount(account: Int): ConnectionIO[List[AccountCurrency]] =
    sql"SELECT * FROM account_currencies WHERE account = $account".query[AccountCurrency].to[List]

  def addAccount(account: Account): ConnectionIO[Int] =
    sql"""INSERT INTO accounts (name, created, wallet, owner) VALUE (
         ${account.name}, ${account.created}, ${account.wallet}, ${account.owner}
       )""".update.withUniqueGeneratedKeys[Int]("id")

  def addCurrencies(currencies: List[AccountCurrency]): ConnectionIO[Int] = {
    if (currencies.nonEmpty) {
      val insert = "INSERT INTO account_currencies (account, currency, start_amount) VALUES (?, ?, ?)"
      val data = currencies.map(_.toInfo)
      Update[AccountCurrencyInfo](insert).updateMany(data)
    } else
      doobie.free.connection.pure(0)
  }

  def editAccount(account: Account): ConnectionIO[Int] =
    sql"UPDATE accounts SET name = ${account.name}, created = ${account.created} WHERE id = ${account.id}".update.run

  def updateStartAmount(currencyId: Int, startAmount: BigDecimal): ConnectionIO[Int] =
    sql"UPDATE account_currencies SET start_amount = $startAmount WHERE id = $currencyId".update.run

  def deleteCurrencies(ids: Option[NonEmptyList[Int]]): ConnectionIO[Int] = {
    if (ids.isDefined)
      (fr"DELETE FROM account_currencies WHERE" ++ Fragments.in(fr"id", ids.get)).update.run
    else
      doobie.free.connection.pure(0)
  }

  def deleteAccount(id: Int): ConnectionIO[Int] = {
    List(
      sql"DELETE FROM account_currencies WHERE account = $id".update.run,
      sql"DELETE FROM accounts WHERE id = $id".update.run
    ).sequence.map(_.sum)
  }
}
