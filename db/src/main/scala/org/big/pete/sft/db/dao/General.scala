package org.big.pete.sft.db.dao

import cats.data.NonEmptyList
import doobie.ConnectionIO
import doobie.implicits._
import doobie.Fragments.in
import org.big.pete.sft.db.domain.User
import org.big.pete.sft.domain.{Wallet, WalletEdit, Currency}


object General {
  def getWallet(permalink: String): ConnectionIO[Option[Wallet]] =
    sql"SELECT * FROM wallets WHERE permalink = $permalink LIMIT 1".query[Wallet].option

  def getWallet(id: Int): ConnectionIO[Option[Wallet]] =
    sql"SELECT * FROM wallets WHERE id = $id LIMIT 1".query[Wallet].option

  def listWallets(user: User): ConnectionIO[List[Wallet]] = {
    val wallets = user.permissions.perWallet.keySet
    val condition = if (wallets.isEmpty) fr"1 = 2" else in(fr"id", NonEmptyList(wallets.head, wallets.tail.toList))
    (fr"SELECT * FROM wallets WHERE " ++ condition).query[Wallet].to[List]
  }

  def addWallet(wallet: Wallet): ConnectionIO[Int] = {
    val permalink = cleanPermalink(wallet.permalink)
    sql"INSERT INTO wallets (name, permalink, owner) VALUE (${wallet.name}, $permalink, ${wallet.owner})".update
      .withUniqueGeneratedKeys[Int]("id")
  }

  def editWallet(wallet: WalletEdit): ConnectionIO[Int] = {
    val permalink = cleanPermalink(wallet.permalink)
    sql"UPDATE wallets SET permalink = $permalink, name = ${wallet.name} WHERE id = ${wallet.id}".update.run
  }

  def deleteWallet(id: Int): List[ConnectionIO[Int]] = {
    val deleteWalletQuery = sql"DELETE FROM wallets WHERE id = $id".update.run
    val accountSelector = "$.perWallet"
    val idSelector = "$.\"" + id + "\""
    val updateUsersQuery =
      sql"""UPDATE users SET
           permissions = JSON_REPLACE(permissions, $accountSelector, JSON_REMOVE(JSON_EXTRACT(permissions, $accountSelector), $idSelector))
           WHERE id = $id""".update.run
    List(deleteWalletQuery, updateUsersQuery)
  }

  private def cleanPermalink(permalink: String): String =
    permalink.replaceAll("[^a-zA-Z0-9-_.]", "")

  def listCurrencies: ConnectionIO[List[Currency]] =
    sql"SELECT * FROM currencies".query[Currency].to[List]
}
