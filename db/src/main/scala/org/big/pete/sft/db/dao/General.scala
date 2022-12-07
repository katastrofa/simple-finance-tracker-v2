package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.implicits._
import doobie.Fragments.in
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{Account, AccountEdit, Currency}


object General {
  def getAccount(permalink: String): ConnectionIO[Option[Account]] =
    sql"SELECT * FROM accounts WHERE permalink = $permalink LIMIT 1".query[Account].option

  def getAccount(id: Int): ConnectionIO[Option[Account]] =
    sql"SELECT * FROM accounts WHERE id = $id LIMIT 1".query[Account].option

  def listAccounts(user: User): ConnectionIO[List[Account]] = {
    val accounts = user.permissions.perAccount.keySet
    val condition = if (accounts.isEmpty) fr"1 = 2" else in(fr"id", NonEmptyList(accounts.head, accounts.tail.toList))
    (fr"SELECT * FROM accounts WHERE " ++ condition).query[Account].to[List]
  }

  def addAccount(account: Account): ConnectionIO[Int] = {
    val permalink = cleanPermalink(account.permalink)
    sql"INSERT INTO accounts (name, permalink, owner) VALUE (${account.name}, $permalink, ${account.owner})".update
      .withUniqueGeneratedKeys[Int]("id")
  }

  def editAccount(account: AccountEdit): ConnectionIO[Int] = {
    val permalink = cleanPermalink(account.permalink)
    sql"UPDATE accounts SET permalink = $permalink, name = ${account.name} WHERE id = ${account.id}".update.run
  }

  def deleteAccount(id: Int): List[ConnectionIO[Int]] = {
    val deleteAccountQuery = sql"DELETE FROM accounts WHERE id = $id".update.run
    val accountSelector = "$.perAccount"
    val idSelector = "$.\"" + id + "\""
    val updateUsersQuery =
      sql"""UPDATE users SET
           permissions = JSON_REPLACE(permissions, $accountSelector, JSON_REMOVE(JSON_EXTRACT(permissions, $accountSelector), $idSelector))
           WHERE id = $id""".update.run
    List(deleteAccountQuery, updateUsersQuery)
  }

  private def cleanPermalink(permalink: String): String =
    permalink.replaceAll("[^a-zA-Z0-9-_.]", "")

  def listCurrencies: ConnectionIO[List[Currency]] =
    sql"SELECT * FROM currencies".query[Currency].to[List]
}
