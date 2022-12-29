package org.big.pete.sft.db

import cats.syntax.{EitherSyntax, ShowSyntax}
import doobie.util.meta.Meta
import doobie.util.{Get, Put}
import io.circe.Json
import io.circe.jawn.parse
import org.big.pete.sft.domain.{ApiAction, TransactionTracking, TransactionType, UserPermissions}
import org.big.pete.sft.domain.Implicits._
import org.big.pete.sft.json.BpJson
import wvlet.log.LogSupport

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}


object domain {
  case class Balance(
      date: LocalDate,
      transactionType: TransactionType,
      amount: BigDecimal,
      moneyAccount: Int,
      currency: String,
      destinationAmount: Option[BigDecimal],
      destinationMoneyAccountId: Option[Int],
      destinationCurrency: Option[String]
  )

  final val MySqlDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  def localDateTimeFromString(time: String): LocalDateTime =
    LocalDateTime.parse(time, MySqlDateTimeFormatter)
  def localDateTimeToString(time: LocalDateTime): String =
    time.format(MySqlDateTimeFormatter)


  case class User(id: Int, email: String, displayName: String, permissions: UserPermissions)
  case class Login(id: Int, userId: Int, lastAccess: LocalDateTime, accessToken: String, refreshToken: String)

  object Implicits extends LogSupport with EitherSyntax with ShowSyntax {
    implicit val transactionTypeMeta: Meta[TransactionType] =
      Meta[String].timap(TransactionType.withName)(_.toString)
    implicit val transactionTrackingMeta: Meta[TransactionTracking] =
      Meta[String].timap(TransactionTracking.withName)(_.toString)
    implicit val apiActionMeta: Meta[ApiAction] =
      Meta[String].timap(ApiAction.withName)(_.toString)

    implicit val mysqlJsonGet: Get[Json] = Get[String].temap[Json] { strJson: String =>
      parse(strJson).leftMap { parsingFailure =>
        error("Unable to parse json from DB: " + parsingFailure.getMessage, parsingFailure.underlying)
        parsingFailure.show
      }
    }
    implicit val mysqlJsonPut: Put[Json] = Put[String].tcontramap(_.noSpaces)

    implicit val userPermissionsGet: Get[UserPermissions] = Get[String].temap[UserPermissions] { strJson: String =>
      BpJson.extract[UserPermissions](strJson).leftMap { failure =>
        error("Unable to parse user permissions from DB: " + failure.getMessage)
        failure.getMessage
      }
    }
    implicit val userPermissionsPut: Put[UserPermissions] = Put[String].tcontramap(obj => BpJson.write(obj))
  }
}
