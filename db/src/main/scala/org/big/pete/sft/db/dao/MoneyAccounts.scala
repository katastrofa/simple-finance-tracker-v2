package org.big.pete.sft.db.dao

import doobie.ConnectionIO
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import org.big.pete.sft.domain.MoneyAccount


object MoneyAccounts {
  def getMoneyAccount(id: Int, accountId: Int): ConnectionIO[Option[MoneyAccount]] =
    sql"SELECT * FROM money_accounts WHERE id = $id AND account = $accountId".query[MoneyAccount].option

  def listMoneyAccounts(accountId: Int): ConnectionIO[List[MoneyAccount]] =
    sql"SELECT * FROM money_accounts WHERE account = $accountId".query[MoneyAccount].to[List]

  def addMoneyAccount(ma: MoneyAccount): ConnectionIO[Int] =
    sql"""INSERT INTO money_accounts (name, start_amount, currency, created, account, owner) VALUE (
         ${ma.name}, ${ma.startAmount}, ${ma.currencyId}, ${ma.created}, ${ma.accountId}, ${ma.owner}
       )""".update.withUniqueGeneratedKeys[Int]("id")

  def editMoneyAccount(ma: MoneyAccount, accountId: Int): ConnectionIO[Int] =
    sql"""UPDATE money_accounts SET name = ${ma.name}, start_amount = ${ma.startAmount}, currency = ${ma.currencyId},
         created = ${ma.created} WHERE id = ${ma.id} AND account = $accountId""".update.run

  def deleteMoneyAccount(id: Int, accountId: Int): ConnectionIO[Int] =
    sql"DELETE FROM money_accounts WHERE id = $id AND account = $accountId".update.run
}
