package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import doobie.{ConnectionIO, Update}
import doobie.implicits._
import doobie.Fragments.in
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{Wallet, WalletEdit, AddWallet, Currency, FullWallet, SimpleUser}


object General {
  def getFullWallet(id: Int): ConnectionIO[Option[FullWallet]] = {
    sql"""SELECT a.*, u.id, u.display_name FROM wallets AS a
            LEFT JOIN patrons AS p ON a.id = p.account
            LEFT JOIN users AS u ON p.user = u.id
            WHERE a.id = $id
    """.query[(Int, String, String, Option[Int], Option[Int], Option[String])].to[List]
      .map(parseFullWallet)
      .map(_.headOption)
  }

  def getFullWallet(permalink: String): ConnectionIO[Option[FullWallet]] = {
    sql"""SELECT a.*, u.id, u.display_name FROM wallets AS a
            LEFT JOIN patrons AS p ON a.id = p.account
            LEFT JOIN users AS u ON p.user = u.id
            WHERE a.permalink = $permalink
    """.query[(Int, String, String, Option[Int], Option[Int], Option[String])].to[List]
      .map(parseFullWallet)
      .map(_.headOption)
  }

  def listFullWallets(user: User): ConnectionIO[List[FullWallet]] = {
    val wallets = user.permissions.perWallet.keySet
    val condition = if (wallets.isEmpty) fr"1 = 2" else in(fr"a.id", NonEmptyList(wallets.head, wallets.tail.toList))
    (
      sql"""SELECT a.*, u.id, u.display_name FROM wallets AS a
              LEFT JOIN patrons AS p ON a.id = p.wallet
              LEFT JOIN users AS u ON p.user = u.id
              WHERE """ ++ condition
    ).query[(Int, String, String, Option[Int], Option[Int], Option[String])].to[List]
      .map(parseFullWallet)
  }

  private def parseFullWallet(rows: List[(Int, String, String, Option[Int], Option[Int], Option[String])]): List[FullWallet] = {
    rows.groupBy(_._1)
      .map { case (id, data) =>
        val patrons = data.flatMap { case (_, _, _, _, idOpt, nameOpt) =>
          idOpt.flatMap { id => nameOpt.map { name =>
            SimpleUser(id, name)
          }}
        }
        val headItem = data.head
        FullWallet(id, headItem._2, headItem._3, headItem._4, patrons)
      }.toList
  }

  def listWallets(user: User): ConnectionIO[List[Wallet]] = {
    val wallets = user.permissions.perWallet.keySet
    val condition = if (wallets.isEmpty) fr"1 = 2" else in(fr"id", NonEmptyList(wallets.head, wallets.tail.toList))
    (fr"SELECT * FROM wallets WHERE " ++ condition).query[Wallet].to[List]
  }

  def addWallet(wallet: AddWallet): ConnectionIO[Int] = {
    val permalink = cleanPermalink(wallet.permalink)
    sql"INSERT INTO wallets (name, permalink, owner) VALUE (${wallet.name}, $permalink, ${wallet.owner})".update
      .withUniqueGeneratedKeys[Int]("id")
  }

  def listPatrons: ConnectionIO[List[SimpleUser]] =
    sql"SELECT id, display_name FROM users".query[SimpleUser].to[List]

  def addPatrons(users: Set[Int], wallet: Int): ConnectionIO[Int] = {
    if (users.nonEmpty) {
      val insert = "INSERT INTO patrons (user, account) VALUES (?, ?)"
      val data = users.map(_ -> wallet).toList
      Update[(Int, Int)](insert).updateMany(data)
    } else
      doobie.free.connection.pure(0)
  }

  def removePatrons(users: Set[Int], wallet: Int): ConnectionIO[Int] = {
    if (users.nonEmpty) {
      (fr"DELETE FROM patrons WHERE wallet = $wallet AND" ++ in(fr"user", NonEmptyList.fromListUnsafe(users.toList))).update.run
    } else
      doobie.free.connection.pure(0)
  }

  def editWallet(wallet: WalletEdit): ConnectionIO[Int] = {
    val permalink = cleanPermalink(wallet.permalink)
    sql"UPDATE wallets SET permalink = $permalink, name = ${wallet.name} WHERE id = ${wallet.id}".update.run
  }

  def deleteWallet(id: Int): List[ConnectionIO[Int]] = {
    val removePatronsQuery = sql"DELETE FROM patrons WHERE wallet = $id".update.run
    val deleteWalletQuery = sql"DELETE FROM wallets WHERE id = $id".update.run
    val walletSelector = "$.perWallet"
    val idSelector = "$.\"" + id + "\""
    val updateUsersQuery =
      sql"""UPDATE users SET
           permissions = JSON_REPLACE(permissions, $walletSelector, JSON_REMOVE(JSON_EXTRACT(permissions, $walletSelector), $idSelector))
           WHERE id = $id""".update.run
    List(removePatronsQuery, deleteWalletQuery, updateUsersQuery)
  }

  private def cleanPermalink(permalink: String): String =
    permalink.replaceAll("[^a-zA-Z0-9-_.]", "")

  def listCurrencies: ConnectionIO[List[Currency]] =
    sql"SELECT * FROM currencies".query[Currency].to[List]
}
