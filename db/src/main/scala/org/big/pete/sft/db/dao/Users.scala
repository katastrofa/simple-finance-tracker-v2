package org.big.pete.sft.db.dao

import doobie.ConnectionIO
import org.big.pete.sft.db.domain.{Login, User}
import doobie.implicits._
import doobie.implicits.javatimedrivernative._
import org.big.pete.sft.domain.UserPermissions


object Users {
  import org.big.pete.sft.db.domain.Implicits._

//  protected def userHasPermissionQuery(userId: Int, accountCondition: Fragment): ConnectionIO[Option[Account]] = {
//    sql"""SELECT a.* FROM accounts AS a
//            JOIN user_account AS ua
//                ON a.id = ua.account
//            WHERE ua.user = $userId AND $accountCondition
//            LIMIT 1"""
//      .query[Account]
//      .option
//  }
//
//  def userHasPermission(userId: Int, accountPermalink: String): ConnectionIO[Option[Account]] =
//    userHasPermissionQuery(userId, fr"a.permalink = $accountPermalink")
//
//  def userHasPermission(userId: Int, accountId: Int): ConnectionIO[Option[Account]] =
//    userHasPermissionQuery(userId, fr"a.id = $accountId")

  def getLogins(userId: Int): ConnectionIO[List[Login]] =
    sql"SELECT * FROM logins WHERE user = $userId".query[Login].to[List]

  def getLogin(token: String): ConnectionIO[Option[Login]] =
    sql"SELECT * FROM logins WHERE access_token = $token LIMIT 1".query[Login].option

  def storeLogin(userId: Int, accessToken: String, refreshToken: String): ConnectionIO[Int] =
    sql"INSERT INTO logins (user, access_token, refresh_token) VALUES ($userId, $accessToken, $refreshToken)".update.run

  def getUser(email: String): ConnectionIO[Option[User]] =
    sql"SELECT * FROM users WHERE email = $email LIMIT 1".query[User].option

  def getUser(id: Int): ConnectionIO[Option[User]] =
    sql"SELECT * FROM users WHERE id = $id LIMIT 1".query[User].option

  def updatePermissions(id: Int, permissions: UserPermissions): ConnectionIO[Int] =
    sql"UPDATE users SET permissions = $permissions WHERE id = $id".update.run
}
