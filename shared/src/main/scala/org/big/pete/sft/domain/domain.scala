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

case class Currency(id: String, name: String, symbol: String)

case class MoneyAccountCurrencyInfo(moneyAccount: Int, currency: String, startAmount: BigDecimal)
case class MoneyAccountCurrency(id: Int, moneyAccount: Int, currency: String, startAmount: BigDecimal) {
  def expand(fullCurrency: Currency): ExpandedMoneyAccountCurrency =
    ExpandedMoneyAccountCurrency(id, moneyAccount, fullCurrency, startAmount)
  def expand(currencies: List[Currency]): ExpandedMoneyAccountCurrency =
    expand(currencies.find(_.id == currency).get)

  def toInfo: MoneyAccountCurrencyInfo =
    MoneyAccountCurrencyInfo(moneyAccount, currency, startAmount)
}
case class ExpandedMoneyAccountCurrency(id: Int, moneyAccount: Int, currency: Currency, startAmount: BigDecimal) {
  def simple: MoneyAccountCurrency =  MoneyAccountCurrency(id, moneyAccount, currency.id, startAmount)
}

case class MoneyAccountWithCurrency(
    id: Int,
    name: String,
    created: LocalDate,
    accountId: Int,
    owner: Option[Int],
    currencyId: Int,
    currency: String,
    startAmount: BigDecimal
) {
  def getCurrency: MoneyAccountCurrency = MoneyAccountCurrency(currencyId, id, currency, startAmount)
}

case class PureMoneyAccount(id: Int, name: String, created: LocalDate, accountId: Int, owner: Option[Int]) {
  def expand(moneyCurrencies: List[MoneyAccountCurrency]): MoneyAccount =
    MoneyAccount(id, name, created, accountId, owner, moneyCurrencies)
  def duplicateExpand(moneyCurrencies: List[MoneyAccountCurrency]): List[MoneyAccountWithCurrency] = {
    moneyCurrencies.map( mCurrency =>
      MoneyAccountWithCurrency(id, name, created, accountId, owner, mCurrency.id, mCurrency.currency, mCurrency.startAmount)
    )
  }
}
case class MoneyAccount(
    id: Int,
    name: String,
    created: LocalDate,
    accountId: Int,
    owner: Option[Int],
    currencies: List[MoneyAccountCurrency]
)

case class Category(id: Int, name: String, description: Option[String], parent: Option[Int], accountId: Int, owner: Option[Int])

case class Transaction(
    id: Int,
    date: LocalDate,
    transactionType: TransactionType,
    amount: BigDecimal,
    description: String,
    categoryId: Int,
    moneyAccount: Int,
    currency: String,
    tracking: TransactionTracking,
    destinationAmount: Option[BigDecimal],
    destinationMoneyAccountId: Option[Int],
    destinationCurrency: Option[String],
    owner: Option[Int]
)

case class EnhancedMoneyAccount(
    id: Int,
    name: String,
    created: LocalDate,
    currencies: List[ExpandedMoneyAccountCurrency],
    status: List[CurrencyAndStatus],
    owner: Option[Int]
)
case class CurrencyAndStatus(currency: Currency, startAmount: BigDecimal, start: BigDecimal, end: BigDecimal)

case class UserPermissions(global: Set[ApiAction], perAccount: Map[Int, Set[ApiAction]], default: Set[ApiAction])


case class ShiftStrategy(newId: Option[Int])
case class ShiftStrategyPerCurrency(newId: Option[Int], currency: String)
case class CategoryDeleteStrategies(shiftSubCats: ShiftStrategy, shiftTransactions: ShiftStrategy)
case class MoneyAccountDeleteStrategy(shiftTransactions: List[ShiftStrategyPerCurrency])
case class AccountEdit(oldPermalink: String, id: Int, name: String, permalink: String, owner: Option[Int])
case class NotAllowedResponse(message: String)
case class TrackingEdit(id: Int, tracking: TransactionTracking)
case class DeleteTransactions(ids: List[Int])
case class MassEditTransactions(ids: List[Int], changeCat: ShiftStrategy, changeMoneyAccount: ShiftStrategy)


object Implicits {
  implicit val accountEncoder: Encoder[Account] = deriveEncoder[Account]
  implicit val currencyEncoder: Encoder[Currency] = deriveEncoder[Currency]
  implicit val moneyAccountCurrencyEncoder: Encoder[MoneyAccountCurrency] = deriveEncoder[MoneyAccountCurrency]
  implicit val expandedMoneyAccountCurrencyEncoder: Encoder[ExpandedMoneyAccountCurrency] = deriveEncoder[ExpandedMoneyAccountCurrency]
  implicit val pureMoneyAccountEncoder: Encoder[PureMoneyAccount] = deriveEncoder[PureMoneyAccount]
  implicit val moneyAccountEncoder: Encoder[MoneyAccount] = deriveEncoder[MoneyAccount]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]
  implicit val transactionEncoder: Encoder[Transaction] = deriveEncoder[Transaction]
  implicit val currencyAndStatusEncoder: Encoder[CurrencyAndStatus] = deriveEncoder[CurrencyAndStatus]
  implicit val enhancedMoneyAccountEncoder: Encoder[EnhancedMoneyAccount] = deriveEncoder[EnhancedMoneyAccount]
  implicit val accountEditEncoder: Encoder[AccountEdit] = deriveEncoder[AccountEdit]
  implicit val trackingEditEncoder: Encoder[TrackingEdit] = deriveEncoder[TrackingEdit]

  implicit val accountDecoder: Decoder[Account] = deriveDecoder[Account]
  implicit val currencyDecoder: Decoder[Currency] = deriveDecoder[Currency]
  implicit val moneyAccountCurrencyDecoder: Decoder[MoneyAccountCurrency] = deriveDecoder[MoneyAccountCurrency]
  implicit val expandedMoneyAccountCurrencyDecoder: Decoder[ExpandedMoneyAccountCurrency] = deriveDecoder[ExpandedMoneyAccountCurrency]
  implicit val pureMoneyAccountDecoder: Decoder[PureMoneyAccount] = deriveDecoder[PureMoneyAccount]
  implicit val moneyAccountDecoder: Decoder[MoneyAccount] = deriveDecoder[MoneyAccount]
  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val transactionDecoder: Decoder[Transaction] = deriveDecoder[Transaction]
  implicit val currencyAndStatusDecoder: Decoder[CurrencyAndStatus] = deriveDecoder[CurrencyAndStatus]
  implicit val enhancedMoneyAccountDecoder: Decoder[EnhancedMoneyAccount] = deriveDecoder[EnhancedMoneyAccount]
  implicit val accountEditDecoder: Decoder[AccountEdit] = deriveDecoder[AccountEdit]
  implicit val trackingEditDecoder: Decoder[TrackingEdit] = deriveDecoder[TrackingEdit]

  implicit val userPermissionsEncoder: Encoder[UserPermissions] = deriveEncoder[UserPermissions]
  implicit val userPermissionsDecoder: Decoder[UserPermissions] = deriveDecoder[UserPermissions]

  implicit val notAllowedResponseEncoder: Encoder[NotAllowedResponse] = deriveEncoder[NotAllowedResponse]
  implicit val notAllowedResponseDecoder: Decoder[NotAllowedResponse] = deriveDecoder[NotAllowedResponse]
  implicit val shiftStrategyEncoder: Encoder[ShiftStrategy] = deriveEncoder[ShiftStrategy]
  implicit val shiftStrategyDecoder: Decoder[ShiftStrategy] = deriveDecoder[ShiftStrategy]
  implicit val shiftStrategyPerCurrencyEncoder: Encoder[ShiftStrategyPerCurrency] = deriveEncoder[ShiftStrategyPerCurrency]
  implicit val shiftStrategyPerCurrencyDecoder: Decoder[ShiftStrategyPerCurrency] = deriveDecoder[ShiftStrategyPerCurrency]
  implicit val categoryDeleteStrategiesEncoder: Encoder[CategoryDeleteStrategies] = deriveEncoder[CategoryDeleteStrategies]
  implicit val categoryDeleteStrategiesDecoder: Decoder[CategoryDeleteStrategies] = deriveDecoder[CategoryDeleteStrategies]
  implicit val moneyAccountDeleteStrategyEncoder: Encoder[MoneyAccountDeleteStrategy] = deriveEncoder[MoneyAccountDeleteStrategy]
  implicit val moneyAccountDeleteStrategyDecoder: Decoder[MoneyAccountDeleteStrategy] = deriveDecoder[MoneyAccountDeleteStrategy]
  implicit val deleteTransactionsEncoder: Encoder[DeleteTransactions] = deriveEncoder[DeleteTransactions]
  implicit val deleteTransactionsDecoder: Decoder[DeleteTransactions] = deriveDecoder[DeleteTransactions]
  implicit val massEditTransactionsEncoder: Encoder[MassEditTransactions] = deriveEncoder[MassEditTransactions]
  implicit val massEditTransactionsDecoder: Decoder[MassEditTransactions] = deriveDecoder[MassEditTransactions]
}

object domain {

}
