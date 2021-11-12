package org.big.pete.sft.db

import cats.syntax.{EitherSyntax, ShowSyntax}
import doobie.util.meta.Meta
import doobie.util.{Get, Put}
import io.circe.Json
import io.circe.jawn.parse
import org.big.pete.{SealedObjects, SealedTraitEnumSupport}
import wvlet.log.LogSupport

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}


object domain {
  final val MySqlDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  def localDateTimeFromString(time: String): LocalDateTime =
    LocalDateTime.parse(time, MySqlDateTimeFormatter)
  def localDateTimeToString(time: LocalDateTime): String =
    time.format(MySqlDateTimeFormatter)


  sealed trait TransactionType
  object TransactionType extends SealedTraitEnumSupport[TransactionType] {
    final case object Income extends TransactionType
    final case object Expense extends TransactionType
    final case object Transfer extends TransactionType

    override protected def allPossibleObjects: Set[TransactionType] = SealedObjects.allPossibleValues[TransactionType]
  }

  sealed trait TransactionTracking
  object TransactionTracking extends SealedTraitEnumSupport[TransactionTracking] {
    final case object None extends TransactionTracking
    final case object Auto extends TransactionTracking
    final case object Verified extends TransactionTracking

    override protected def allPossibleObjects: Set[TransactionTracking] = SealedObjects.allPossibleValues[TransactionTracking]
  }

  case class User(id: Int, email: String, displayName: String, permissions: Json)
  case class Account(id: Int, name: String, permalink: String)
  case class UserAccount(userId: Int, accountId: Int)
  case class Login(id: Int, userId: Int, lastAccess: LocalDateTime, accessToken: String, refreshToken: String)
  case class Currency(id: Int, name: String, symbol: String)
  case class MoneyAccount(id: Int, name: String, startAmount: BigDecimal, currencyId: Int, created: LocalDateTime, accountId: Int)
  case class Category(id: Int, name: String, description: Option[String], parent: Option[Int], accountId: Int)
  case class Transaction(
      id: Int,
      date: LocalDate,
      transactionType: TransactionType,
      amount: BigDecimal,
      description: String,
      categoryId: Int,
      moneyAccount: Int,
      tracking: TransactionTracking,
      destinationAmount: Option[BigDecimal],
      destinationMoneyAccountId: Option[Int]
  )

  object Implicits extends LogSupport with EitherSyntax with ShowSyntax {
    implicit val transactionTypeMeta: Meta[TransactionType] =
      Meta[String].timap(TransactionType.apply)(TransactionType.toString)
    implicit val transactionTrackingMeta: Meta[TransactionTracking] =
      Meta[String].timap(TransactionTracking.apply)(TransactionTracking.toString)

    implicit val mysqlJsonGet: Get[Json] = Get[String].temap[Json] { strJson: String =>
      parse(strJson).leftMap { parsingFailure =>
        error("Unable to parse json from DB: " + parsingFailure.getMessage, parsingFailure.underlying)
        parsingFailure.show
      }
    }

    implicit val mysqlJsonPut: Put[Json] = Put[String].tcontramap(_.noSpaces)
  }
}
