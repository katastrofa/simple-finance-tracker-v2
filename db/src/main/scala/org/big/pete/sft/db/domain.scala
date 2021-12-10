package org.big.pete.sft.db

import cats.syntax.{EitherSyntax, ShowSyntax}
import doobie.util.meta.Meta
import doobie.util.{Get, Put}
import io.circe.Json
import io.circe.jawn.parse
import org.big.pete.sft.domain.{TransactionTracking, TransactionType}
import wvlet.log.LogSupport

import java.time.format.DateTimeFormatter
import java.time.LocalDateTime


object domain {
  final val MySqlDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  def localDateTimeFromString(time: String): LocalDateTime =
    LocalDateTime.parse(time, MySqlDateTimeFormatter)
  def localDateTimeToString(time: LocalDateTime): String =
    time.format(MySqlDateTimeFormatter)


  case class User(id: Int, email: String, displayName: String, permissions: Json)

  case class UserAccount(userId: Int, accountId: Int)
  case class Login(id: Int, userId: Int, lastAccess: LocalDateTime, accessToken: String, refreshToken: String)

  object Implicits extends LogSupport with EitherSyntax with ShowSyntax {
    implicit val transactionTypeMeta: Meta[TransactionType] =
      Meta[String].timap(TransactionType.withName)(_.toString)
    implicit val transactionTrackingMeta: Meta[TransactionTracking] =
      Meta[String].timap(TransactionTracking.withName)(_.toString)

    implicit val mysqlJsonGet: Get[Json] = Get[String].temap[Json] { strJson: String =>
      parse(strJson).leftMap { parsingFailure =>
        error("Unable to parse json from DB: " + parsingFailure.getMessage, parsingFailure.underlying)
        parsingFailure.show
      }
    }

    implicit val mysqlJsonPut: Put[Json] = Put[String].tcontramap(_.noSpaces)
  }
}
