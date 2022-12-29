package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import cats.implicits.toTraverseOps
import doobie.ConnectionIO
import doobie.Fragments
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import doobie.util.update.Update
import org.big.pete.sft.domain.{MoneyAccount, MoneyAccountCurrency, MoneyAccountCurrencyInfo, MoneyAccountWithCurrency, PureMoneyAccount}


object MoneyAccounts {
  def toMoneyAccount(list: List[MoneyAccountWithCurrency]): MoneyAccount = {
    val ref = list.head
    MoneyAccount(ref.id, ref.name, ref.created, ref.accountId, ref.owner, list.map(_.getCurrency))
  }

  def getPureMoneyAccount(id: Int, accountId: Int): ConnectionIO[Option[PureMoneyAccount]] =
    sql"SELECT * FROM money_accounts WHERE id = $id AND account = $accountId".query[PureMoneyAccount].option

  def getMoneyAccount(id: Int): ConnectionIO[List[MoneyAccountWithCurrency]] = {
    sql"""SELECT m.*, c.id, c.currency, c.start_amount FROM money_accounts AS m
        JOIN money_account_currencies AS c
            ON m.id = c.money_account
        WHERE m.id = $id"""
      .query[MoneyAccountWithCurrency].to[List]
  }

  def listMoneyAccounts(accountId:Int): ConnectionIO[List[MoneyAccountWithCurrency]] = {
    sql"""SELECT m.*, c.id, c.currency, c.start_amount FROM money_accounts AS m
          JOIN money_account_currencies AS c
              ON m.id = c.money_account
         WHERE account = $accountId"""
      .query[MoneyAccountWithCurrency].to[List]
  }

  def listCurrenciesForMoneyAccount(moneyAccountId: Int): ConnectionIO[List[MoneyAccountCurrency]] =
    sql"SELECT * FROM money_account_currencies WHERE money_account = $moneyAccountId".query[MoneyAccountCurrency].to[List]

  def addMoneyAccount(ma: MoneyAccount): ConnectionIO[Int] =
    sql"""INSERT INTO money_accounts (name, created, account, owner) VALUE (
         ${ma.name}, ${ma.created}, ${ma.accountId}, ${ma.owner}
       )""".update.withUniqueGeneratedKeys[Int]("id")

  def addCurrencies(currencies: List[MoneyAccountCurrency]): ConnectionIO[Int] = {
    if (currencies.nonEmpty) {
      val insert = "INSERT INTO money_account_currencies (money_account, currency, start_amount) VALUES (?, ?, ?)"
      val data = currencies.map(_.toInfo)
      Update[MoneyAccountCurrencyInfo](insert).updateMany(data)
    } else
      doobie.free.connection.pure(0)
  }

  def editMoneyAccount(ma: MoneyAccount): ConnectionIO[Int] =
    sql"UPDATE money_accounts SET name = ${ma.name}, created = ${ma.created} WHERE id = ${ma.id}".update.run

  def updateStartAmount(currencyId: Int, startAmount: BigDecimal): ConnectionIO[Int] =
    sql"UPDATE money_account_currencies SET start_amount = $startAmount WHERE id = $currencyId".update.run

  def deleteCurrencies(ids: Option[NonEmptyList[Int]]): ConnectionIO[Int] = {
    if (ids.isDefined)
      (fr"DELETE FROM money_account_currencies WHERE" ++ Fragments.in(fr"id", ids.get)).update.run
    else
      doobie.free.connection.pure(0)
  }

  def deleteMoneyAccount(id: Int): ConnectionIO[Int] = {
    List(
      sql"DELETE FROM money_account_currencies WHERE money_account = $id".update.run,
      sql"DELETE FROM money_accounts WHERE id = $id".update.run
    ).sequence.map(_.sum)
  }
}
