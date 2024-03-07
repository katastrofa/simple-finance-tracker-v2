package org.big.pete.sft.domain

import enumeratum.{CirceEnum, Enum, EnumEntry}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.big.pete.domain.DDItem

import java.time.LocalDate


sealed trait TransactionType extends EnumEntry with DDItem {
  override def ddId: String = toString
  override def ddDisplayName: String = toString
}
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

sealed trait TransactionSplitType extends EnumEntry
case object TransactionSplitType extends Enum[TransactionSplitType] with CirceEnum[TransactionSplitType] {
  final case object Exact extends TransactionSplitType
  final case object Percentage extends TransactionSplitType
  final case object Even extends TransactionSplitType

  val values: IndexedSeq[TransactionSplitType] = findValues
}

sealed trait ApiAction extends EnumEntry
case object ApiAction extends Enum[ApiAction] with CirceEnum[ApiAction] {
  final case object Basic extends ApiAction

  final case object ModifyOwnWallet extends ApiAction
  final case object ModifyWallet extends ApiAction
  final case object DeleteOwnWallet extends ApiAction
  final case object DeleteWallet extends ApiAction

  final case object ModifyOwnCategory extends ApiAction
  final case object ModifyOwnAccount extends ApiAction
  final case object ModifyOwnTransactions extends ApiAction

  final case object ModifyCategory extends ApiAction
  final case object ModifyAccount extends ApiAction
  final case object ModifyTransactions extends ApiAction

  final case object DeleteCategory extends ApiAction
  final case object DeleteAccount extends ApiAction
  final case object DeleteTransactions extends ApiAction

  val values: IndexedSeq[ApiAction] = findValues
}



case class SimpleUser(id: Int, displayName: String) extends DDItem {
  override def ddId: String = id.toString
  override def ddDisplayName: String = displayName
}
case class Wallet(id: Int, name: String, permalink: String, owner: Option[Int])
case class AddWallet(id: Int, name: String, permalink: String, owner: Option[Int], patrons: List[Int])
case class FullWallet(id: Int, name: String, permalink: String, owner: Option[Int], patrons: List[SimpleUser])

case class Currency(id: String, name: String, symbol: String) extends DDItem {
  override def ddId: String = id
  override def ddDisplayName: String = s"$name ($symbol)"
}

case class GeneralData(me: SimpleUser, patrons: List[SimpleUser], currencies: List[Currency], wallets: List[FullWallet])

case class AccountCurrencyInfo(account: Int, currency: String, startAmount: BigDecimal)
case class AccountOptionalCurrency(id: Int, account: Int, currency: Option[String], startAmount: BigDecimal) {
  def toCurrency: Option[AccountCurrency] = currency.map { cur =>
    AccountCurrency(id, account, cur, startAmount)
  }
}
case class AccountCurrency(id: Int, account: Int, currency: String, startAmount: BigDecimal) {
  def expand(fullCurrency: Currency): ExpandedAccountCurrency =
    ExpandedAccountCurrency(id, account, fullCurrency, startAmount)
  def expand(currencies: List[Currency]): ExpandedAccountCurrency =
    expand(currencies.find(_.id == currency).get)

  def toInfo: AccountCurrencyInfo =
    AccountCurrencyInfo(account, currency, startAmount)
  def toOptional: AccountOptionalCurrency =
    AccountOptionalCurrency(id, account, Some(currency), startAmount)
}
case class ExpandedAccountCurrency(id: Int, account: Int, currency: Currency, startAmount: BigDecimal) {
  def simple: AccountCurrency =  AccountCurrency(id, account, currency.id, startAmount)
}

case class AccountWithCurrency(
    id: Int,
    name: String,
    created: LocalDate,
    wallet: Int,
    owner: Option[Int],
    currencyId: Int,
    currency: String,
    startAmount: BigDecimal
) {
  def getCurrency: AccountCurrency = AccountCurrency(currencyId, id, currency, startAmount)
}

case class PureAccount(id: Int, name: String, created: LocalDate, wallet: Int, owner: Option[Int]) {
  def expand(currencies: List[AccountCurrency]): Account =
    Account(id, name, created, wallet, owner, currencies)
  def duplicateExpand(currencies: List[AccountCurrency]): List[AccountWithCurrency] = {
    currencies.map(currency =>
      AccountWithCurrency(id, name, created, wallet, owner, currency.id, currency.currency, currency.startAmount)
    )
  }
}
case class Account(
    id: Int,
    name: String,
    created: LocalDate,
    wallet: Int,
    owner: Option[Int],
    currencies: List[AccountCurrency]
)

case class Category(id: Int, name: String, description: Option[String], parent: Option[Int], wallet: Int, owner: Option[Int])

case class Transaction(
    id: Int,
    date: LocalDate,
    transactionType: TransactionType,
    amount: BigDecimal,
    description: String,
    category: Int,
    account: Int,
    currency: String,
    tracking: TransactionTracking,
    destinationAmount: Option[BigDecimal],
    destinationAccount: Option[Int],
    destinationCurrency: Option[String],
    owner: Option[Int]
)

case class EnhancedAccount(
    id: Int,
    name: String,
    created: LocalDate,
    currencies: List[ExpandedAccountCurrency],
    status: List[CurrencyAndStatus],
    owner: Option[Int]
) extends DDItem {
  override def ddId: String = id.toString
  override def ddDisplayName: String = name
}
case class CurrencyAndStatus(currency: Currency, startAmount: BigDecimal, start: BigDecimal, end: BigDecimal)

case class UserPermissions(global: Set[ApiAction], perWallet: Map[Int, Set[ApiAction]], default: Set[ApiAction])


case class ShiftStrategy(newId: Option[Int])
case class ShiftStrategyPerCurrency(newId: Option[Int], currency: String)
case class CategoryDeleteStrategies(shiftSubCats: ShiftStrategy, shiftTransactions: ShiftStrategy)
case class AccountDeleteStrategy(shiftTransactions: List[ShiftStrategyPerCurrency])
case class WalletEdit(oldPermalink: String, id: Int, name: String, permalink: String, owner: Option[Int], patrons: List[Int])
case class NotAllowedResponse(message: String)
case class TrackingEdit(id: Int, tracking: TransactionTracking)
case class DeleteTransactions(ids: List[Int])
case class MassEditTransactions(ids: List[Int], changeCat: ShiftStrategy, changeAccount: ShiftStrategy)


object Implicits {
  implicit val simpleUserEncoder: Encoder[SimpleUser] = deriveEncoder[SimpleUser]
  implicit val walletEncoder: Encoder[Wallet] = deriveEncoder[Wallet]
  implicit val addWalletEncoder: Encoder[AddWallet] = deriveEncoder[AddWallet]
  implicit val fullWalletEncoder: Encoder[FullWallet] = deriveEncoder[FullWallet]
  implicit val currencyEncoder: Encoder[Currency] = deriveEncoder[Currency]
  implicit val generalDataEncoder: Encoder[GeneralData] = deriveEncoder[GeneralData]
  implicit val accountCurrencyEncoder: Encoder[AccountCurrency] = deriveEncoder[AccountCurrency]
  implicit val expandedAccountCurrencyEncoder: Encoder[ExpandedAccountCurrency] = deriveEncoder[ExpandedAccountCurrency]
  implicit val pureAccountEncoder: Encoder[PureAccount] = deriveEncoder[PureAccount]
  implicit val accountEncoder: Encoder[Account] = deriveEncoder[Account]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]
  implicit val transactionEncoder: Encoder[Transaction] = deriveEncoder[Transaction]
  implicit val currencyAndStatusEncoder: Encoder[CurrencyAndStatus] = deriveEncoder[CurrencyAndStatus]
  implicit val enhancedAccountEncoder: Encoder[EnhancedAccount] = deriveEncoder[EnhancedAccount]
  implicit val walletEditEncoder: Encoder[WalletEdit] = deriveEncoder[WalletEdit]
  implicit val trackingEditEncoder: Encoder[TrackingEdit] = deriveEncoder[TrackingEdit]

  implicit val simpleUserDecoder: Decoder[SimpleUser] = deriveDecoder[SimpleUser]
  implicit val walletDecoder: Decoder[Wallet] = deriveDecoder[Wallet]
  implicit val addWalletDecoder: Decoder[AddWallet] = deriveDecoder[AddWallet]
  implicit val fullWalletDecoder: Decoder[FullWallet] = deriveDecoder[FullWallet]
  implicit val currencyDecoder: Decoder[Currency] = deriveDecoder[Currency]
  implicit val generalDataDecoder: Decoder[GeneralData] = deriveDecoder[GeneralData]
  implicit val accountCurrencyDecoder: Decoder[AccountCurrency] = deriveDecoder[AccountCurrency]
  implicit val expandedAccountCurrencyDecoder: Decoder[ExpandedAccountCurrency] = deriveDecoder[ExpandedAccountCurrency]
  implicit val pureAccountDecoder: Decoder[PureAccount] = deriveDecoder[PureAccount]
  implicit val accountDecoder: Decoder[Account] = deriveDecoder[Account]
  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val transactionDecoder: Decoder[Transaction] = deriveDecoder[Transaction]
  implicit val currencyAndStatusDecoder: Decoder[CurrencyAndStatus] = deriveDecoder[CurrencyAndStatus]
  implicit val enhancedAccountDecoder: Decoder[EnhancedAccount] = deriveDecoder[EnhancedAccount]
  implicit val walletEditDecoder: Decoder[WalletEdit] = deriveDecoder[WalletEdit]
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
  implicit val accountDeleteStrategyEncoder: Encoder[AccountDeleteStrategy] = deriveEncoder[AccountDeleteStrategy]
  implicit val accountDeleteStrategyDecoder: Decoder[AccountDeleteStrategy] = deriveDecoder[AccountDeleteStrategy]
  implicit val deleteTransactionsEncoder: Encoder[DeleteTransactions] = deriveEncoder[DeleteTransactions]
  implicit val deleteTransactionsDecoder: Decoder[DeleteTransactions] = deriveDecoder[DeleteTransactions]
  implicit val massEditTransactionsEncoder: Encoder[MassEditTransactions] = deriveEncoder[MassEditTransactions]
  implicit val massEditTransactionsDecoder: Decoder[MassEditTransactions] = deriveDecoder[MassEditTransactions]
}

object domain {

}
