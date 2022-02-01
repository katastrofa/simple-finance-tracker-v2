package org.big.pete.sft.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

import java.time.LocalDate


sealed trait TransactionType extends EnumEntry
case object TransactionType extends Enum[TransactionType] with CirceEnum[TransactionType] {
  final case object Income extends TransactionType
  final case object Expense extends TransactionType
  final case object Transfer extends TransactionType

  val values: IndexedSeq[TransactionType] = findValues
}

sealed trait TransactionTracking extends EnumEntry
case object TransactionTracking extends Enum[TransactionTracking] with CirceEnum[TransactionTracking] {
  final case object None extends TransactionTracking
  final case object Auto extends TransactionTracking
  final case object Verified extends TransactionTracking

  val values: IndexedSeq[TransactionTracking] = findValues
}

sealed trait ApiAction extends EnumEntry
case object ApiAction extends Enum[ApiAction] with CirceEnum[ApiAction] {
  final case object Basic extends ApiAction

  final case object ModifyOwnAccount extends ApiAction
  final case object ModifyAccount extends ApiAction
  final case object DeleteOwnAccount extends ApiAction
  final case object DeleteAccount extends ApiAction

  final case object ModifyOwnCategory extends ApiAction
  final case object ModifyOwnMoneyAccount extends ApiAction
  final case object ModifyOwnTransactions extends ApiAction

  final case object ModifyCategory extends ApiAction
  final case object ModifyMoneyAccount extends ApiAction
  final case object ModifyTransactions extends ApiAction

  final case object DeleteCategory extends ApiAction
  final case object DeleteMoneyAccount extends ApiAction
  final case object DeleteTransactions extends ApiAction

  val values: IndexedSeq[ApiAction] = findValues
}

case class Account(id: Int, name: String, permalink: String, owner: Option[Int])
case class Currency(id: Int, name: String, symbol: String)
case class MoneyAccount(id: Int, name: String, startAmount: BigDecimal, currencyId: Int, created: LocalDate, accountId: Int, owner: Option[Int])
case class Category(id: Int, name: String, description: Option[String], parent: Option[Int], accountId: Int, owner: Option[Int])
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
    destinationMoneyAccountId: Option[Int],
    owner: Option[Int]
)

case class EnhancedMoneyAccount(
    id: Int,
    name: String,
    startAmount: BigDecimal,
    currency: Currency,
    created: LocalDate,
    periodStatus: PeriodAmountStatus,
    owner: Option[Int]
)
case class PeriodAmountStatus(start: BigDecimal, end: BigDecimal)

case class UserPermissions(global: Set[ApiAction], perAccount: Map[Int, Set[ApiAction]], default: Set[ApiAction])


case class ShiftStrategy(newId: Option[Int])
case class CategoryDeleteStrategies(shiftSubCats: ShiftStrategy, shiftTransactions: ShiftStrategy)
case class MoneyAccountDeleteStrategy(shiftTransactions: ShiftStrategy)
case class AccountEdit(oldPermalink: String, id: Int, name: String, permalink: String, owner: Option[Int])
case class NotAllowedResponse(message: String)


object Implicits {
  implicit val accountEncoder: Encoder[Account] = deriveEncoder[Account]
  implicit val currencyEncoder: Encoder[Currency] = deriveEncoder[Currency]
  implicit val moneyAccountEncoder: Encoder[MoneyAccount] = deriveEncoder[MoneyAccount]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]
  implicit val transactionEncoder: Encoder[Transaction] = deriveEncoder[Transaction]
  implicit val periodAmountStatusEncoder: Encoder[PeriodAmountStatus] = deriveEncoder[PeriodAmountStatus]
  implicit val enhancedMoneyAccountEncoder: Encoder[EnhancedMoneyAccount] = deriveEncoder[EnhancedMoneyAccount]
  implicit val accountEditEncoder: Encoder[AccountEdit] = deriveEncoder[AccountEdit]

  implicit val accountDecoder: Decoder[Account] = deriveDecoder[Account]
  implicit val currencyDecoder: Decoder[Currency] = deriveDecoder[Currency]
  implicit val moneyAccountDecoder: Decoder[MoneyAccount] = deriveDecoder[MoneyAccount]
  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val transactionDecoder: Decoder[Transaction] = deriveDecoder[Transaction]
  implicit val periodAmountStatusDecoder: Decoder[PeriodAmountStatus] = deriveDecoder[PeriodAmountStatus]
  implicit val enhancedMoneyAccountDecoder: Decoder[EnhancedMoneyAccount] = deriveDecoder[EnhancedMoneyAccount]
  implicit val accountEditDecoder: Decoder[AccountEdit] = deriveDecoder[AccountEdit]

  implicit val userPermissionsEncoder: Encoder[UserPermissions] = deriveEncoder[UserPermissions]
  implicit val userPermissionsDecoder: Decoder[UserPermissions] = deriveDecoder[UserPermissions]

  implicit val notAllowedResponseEncoder: Encoder[NotAllowedResponse] = deriveEncoder[NotAllowedResponse]
  implicit val notAllowedResponseDecoder: Decoder[NotAllowedResponse] = deriveDecoder[NotAllowedResponse]
  implicit val shiftStrategyEncoder: Encoder[ShiftStrategy] = deriveEncoder[ShiftStrategy]
  implicit val shiftStrategyDecoder: Decoder[ShiftStrategy] = deriveDecoder[ShiftStrategy]
  implicit val categoryDeleteStrategiesEncoder: Encoder[CategoryDeleteStrategies] = deriveEncoder[CategoryDeleteStrategies]
  implicit val categoryDeleteStrategiesDecoder: Decoder[CategoryDeleteStrategies] = deriveDecoder[CategoryDeleteStrategies]
  implicit val moneyAccountDeleteStrategyEncoder: Encoder[MoneyAccountDeleteStrategy] = deriveEncoder[MoneyAccountDeleteStrategy]
  implicit val moneyAccountDeleteStrategyDecoder: Decoder[MoneyAccountDeleteStrategy] = deriveDecoder[MoneyAccountDeleteStrategy]
}

object domain {

}
