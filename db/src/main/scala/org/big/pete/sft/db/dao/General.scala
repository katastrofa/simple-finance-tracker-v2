package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import doobie.{ConnectionIO, Update}
import doobie.implicits._
import doobie.Fragments.in
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{Account, AccountEdit, AddAccount, Currency, FullAccount, SimpleUser}


object General {
  def getFullAccount(id: Int): ConnectionIO[Option[FullAccount]] = {
    sql"""SELECT a.*, u.id, u.display_name FROM accounts AS a
            LEFT JOIN patrons AS p ON a.id = p.account
            LEFT JOIN users AS u ON p.user = u.id
            WHERE a.id = $id
    """.query[(Int, String, String, Option[Int], Option[Int], Option[String])].to[List]
      .map(parseFullAccount)
      .map(_.headOption)
  }

  def getFullAccount(permalink: String): ConnectionIO[Option[FullAccount]] = {
    sql"""SELECT a.*, u.id, u.display_name FROM accounts AS a
            LEFT JOIN patrons AS p ON a.id = p.account
            LEFT JOIN users AS u ON p.user = u.id
            WHERE a.permalink = $permalink
    """.query[(Int, String, String, Option[Int], Option[Int], Option[String])].to[List]
      .map(parseFullAccount)
      .map(_.headOption)
  }

  def listFullAccounts(user: User): ConnectionIO[List[FullAccount]] = {
    val accounts = user.permissions.perAccount.keySet
    val condition = if (accounts.isEmpty) fr"1 = 2" else in(fr"a.id", NonEmptyList(accounts.head, accounts.tail.toList))
    (
      sql"""SELECT a.*, u.id, u.display_name FROM accounts AS a
              LEFT JOIN patrons AS p ON a.id = p.account
              LEFT JOIN users AS u ON p.user = u.id
              WHERE """ ++ condition
    ).query[(Int, String, String, Option[Int], Option[Int], Option[String])].to[List]
      .map(parseFullAccount)
  }

  private def parseFullAccount(rows: List[(Int, String, String, Option[Int], Option[Int], Option[String])]): List[FullAccount] = {
    rows.groupBy(_._1)
      .map { case (id, data) =>
        val patrons = data.flatMap { case (_, _, _, _, idOpt, nameOpt) =>
          idOpt.flatMap { id => nameOpt.map { name =>
            SimpleUser(id, name)
          }}
        }
        val headItem = data.head
        FullAccount(id, headItem._2, headItem._3, headItem._4, patrons)
      }.toList
  }

  def listAccounts(user: User): ConnectionIO[List[Account]] = {
    val accounts = user.permissions.perAccount.keySet
    val condition = if (accounts.isEmpty) fr"1 = 2" else in(fr"id", NonEmptyList(accounts.head, accounts.tail.toList))
    (fr"SELECT * FROM accounts WHERE " ++ condition).query[Account].to[List]
  }

  def addAccount(account: AddAccount): ConnectionIO[Int] = {
    val permalink = cleanPermalink(account.permalink)
    sql"INSERT INTO accounts (name, permalink, owner) VALUE (${account.name}, $permalink, ${account.owner})".update
      .withUniqueGeneratedKeys[Int]("id")
  }

  def listPatrons: ConnectionIO[List[SimpleUser]] =
    sql"SELECT id, display_name FROM users".query[SimpleUser].to[List]

  def addPatrons(users: Set[Int], account: Int): ConnectionIO[Int] = {
    if (users.nonEmpty) {
      val insert = "INSERT INTO patrons (user, account) VALUES (?, ?)"
      val data = users.map(_ -> account).toList
      Update[(Int, Int)](insert).updateMany(data)
    } else
      doobie.free.connection.pure(0)
  }

  def removePatrons(users: Set[Int], account: Int): ConnectionIO[Int] = {
    if (users.nonEmpty) {
      (fr"DELETE FROM patrons WHERE account = $account AND" ++ in(fr"user", NonEmptyList.fromListUnsafe(users.toList))).update.run
    } else
      doobie.free.connection.pure(0)
  }

  def editAccount(account: AccountEdit): ConnectionIO[Int] = {
    val permalink = cleanPermalink(account.permalink)
    sql"UPDATE accounts SET permalink = $permalink, name = ${account.name} WHERE id = ${account.id}".update.run
  }

  def deleteAccount(id: Int): List[ConnectionIO[Int]] = {
    val removePatronsQuery = sql"DELETE FROM patrons WHERE account = $id".update.run
    val deleteAccountQuery = sql"DELETE FROM accounts WHERE id = $id".update.run
    val accountSelector = "$.perAccount"
    val idSelector = "$.\"" + id + "\""
    val updateUsersQuery =
      sql"""UPDATE users SET
           permissions = JSON_REPLACE(permissions, $accountSelector, JSON_REMOVE(JSON_EXTRACT(permissions, $accountSelector), $idSelector))
           WHERE id = $id""".update.run
    List(removePatronsQuery, deleteAccountQuery, updateUsersQuery)
  }

  private def cleanPermalink(permalink: String): String =
    permalink.replaceAll("[^a-zA-Z0-9-_.]", "")

  def listCurrencies: ConnectionIO[List[Currency]] =
    sql"SELECT * FROM currencies".query[Currency].to[List]
}
