package org.big.pete.sft.db

import cats.syntax.{EitherSyntax, ShowSyntax}
import doobie.util.meta.Meta
import doobie.util.{Get, Put}
import doobie.implicits._
import io.circe.Json
import io.circe.jawn.parse
import org.big.pete.sft.domain.{ApiAction, Op, Status, UserPermissions}
import org.big.pete.sft.domain.Givens.*
import org.big.pete.sft.json.BpJson
import wvlet.log.LogSupport

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import scala.util.Try


object domain {
  case class Balance(
      date: LocalDate,
      op: Op,
      amount: BigDecimal,
      account: Int,
      currency: String,
      destinationAmount: Option[BigDecimal],
      destinationAccount: Option[Int],
      destinationCurrency: Option[String]
  )

  final val MySqlDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  def localDateTimeFromString(time: String): LocalDateTime =
    LocalDateTime.parse(time, MySqlDateTimeFormatter)
  def localDateTimeToString(time: LocalDateTime): String =
    time.format(MySqlDateTimeFormatter)


  final case class User(id: Int, email: String, displayName: String, permissions: UserPermissions)
  final case class Login(id: Int, userId: Int, lastAccess: LocalDateTime, accessToken: String, refreshToken: String)

  object Givens extends LogSupport with EitherSyntax with ShowSyntax {
    implicit val opGet: Get[Op] = Get[String].temap(str => Try(Op.valueOf(str)).toEither.leftMap(_.getMessage))
    implicit val opPut: Put[Op] = Put[String].tcontramap(_.toString)
    implicit val statusGet: Get[Status] = Get[String].temap(str => Try(Status.valueOf(str)).toEither.leftMap(_.getMessage))
    implicit val statusPut: Put[Status] = Put[String].tcontramap(_.toString)
    implicit val apiActionGet: Get[ApiAction] = Get[String].temap(str => Try(ApiAction.valueOf(str)).toEither.leftMap(_.getMessage))
    implicit val apiActionPut: Put[ApiAction] = Put[String].tcontramap(_.toString)

    private def jsonEncoder(strJson: String): Either[String, Json] =
      parse(strJson).leftMap { parsingFailure =>
        error("Unable to parse json from DB: " + parsingFailure.getMessage, parsingFailure.underlying)
        parsingFailure.show
      }

    private def userPermissionsEncoder(strJson: String): Either[String, UserPermissions] =
      BpJson.extract[UserPermissions](strJson).leftMap { failure =>
        error("Unable to parse user permissions from DB: " + failure.getMessage)
        failure.getMessage
      }

    implicit val mysqlJsonGet: Get[Json] = Get[String].temap[Json](jsonEncoder)
    implicit val mysqlJsonPut: Put[Json] = Put[String].tcontramap(_.noSpaces)
    implicit val userPermissionsGet: Get[UserPermissions] = Get[String].temap[UserPermissions](userPermissionsEncoder)
    implicit val userPermissionsPut: Put[UserPermissions] = Put[String].tcontramap(obj => BpJson.write(obj))

  }
}
