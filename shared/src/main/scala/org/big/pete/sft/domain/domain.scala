package org.big.pete.sft.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.latestbit.circe.adt.codec.*

import java.time.LocalDate


enum Op derives JsonTaggedAdt.Codec {
  case Income
  case Expense
  case Transfer
}

enum Status derives JsonTaggedAdt.Codec {
  case None
  case Auto
  case Verified
}

enum ApiAction derives JsonTaggedAdt.Codec {
  case Basic

  case ModifyOwnWallet
  case ModifyWallet
  case DeleteOwnWallet
  case DeleteWallet

  case ModifyOwnCategory
  case ModifyOwnAccount
  case ModifyOwnTransactions

  case ModifyCategory
  case ModifyAccount
  case ModifyTransactions

  case DeleteCategory
  case DeleteAccount
  case DeleteTransactions
}

case class Wallet(id: Int, name: String, permalink: String, owner: Option[Int])

case class Currency(id: String, name: String, symbol: String)

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
  def expand(moneyCurrencies: List[AccountCurrency]): Account =
    Account(id, name, created, wallet, owner, moneyCurrencies)
  def duplicateExpand(moneyCurrencies: List[AccountCurrency]): List[AccountWithCurrency] = {
    moneyCurrencies.map( mCurrency =>
      AccountWithCurrency(id, name, created, wallet, owner, mCurrency.id, mCurrency.currency, mCurrency.startAmount)
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
    op: Op,
    amount: BigDecimal,
    description: String,
    category: Int,
    account: Int,
    currency: String,
    status: Status,
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
    balance: List[CurrencyBalance],
    owner: Option[Int]
)
case class CurrencyBalance(currency: Currency, startAmount: BigDecimal, start: BigDecimal, end: BigDecimal)

case class UserPermissions(global: Set[ApiAction], perWallet: Map[Int, Set[ApiAction]], default: Set[ApiAction])


case class ShiftStrategy(newId: Option[Int])
case class ShiftStrategyPerCurrency(newId: Option[Int], currency: String)
case class CategoryDeleteStrategies(shiftSubCats: ShiftStrategy, shiftTransactions: ShiftStrategy)
case class AccountDeleteStrategy(shiftTransactions: List[ShiftStrategyPerCurrency])
case class WalletEdit(oldPermalink: String, id: Int, name: String, permalink: String, owner: Option[Int])
case class NotAllowedResponse(message: String)
case class StatusEdit(id: Int, status: Status)
case class DeleteTransactions(ids: List[Int])
case class MassEditTransactions(ids: List[Int], changeCat: ShiftStrategy, changeAccount: ShiftStrategy)


object Givens {
  implicit val walletEncoder: Encoder[Wallet] = deriveEncoder[Wallet]
  implicit val currencyEncoder: Encoder[Currency] = deriveEncoder[Currency]
  implicit val accountCurrencyEncoder: Encoder[AccountCurrency] = deriveEncoder[AccountCurrency]
  implicit val expandedAccountCurrencyEncoder: Encoder[ExpandedAccountCurrency] = deriveEncoder[ExpandedAccountCurrency]
  implicit val pureAccountEncoder: Encoder[PureAccount] = deriveEncoder[PureAccount]
  implicit val accountEncoder: Encoder[Account] = deriveEncoder[Account]
  implicit val categoryEncoder: Encoder[Category] = deriveEncoder[Category]
  implicit val transactionEncoder: Encoder[Transaction] = deriveEncoder[Transaction]
  implicit val currencyBalanceEncoder: Encoder[CurrencyBalance] = deriveEncoder[CurrencyBalance]
  implicit val enhancedAccountEncoder: Encoder[EnhancedAccount] = deriveEncoder[EnhancedAccount]
  implicit val walletEditEncoder: Encoder[WalletEdit] = deriveEncoder[WalletEdit]
  implicit val statusEditEncoder: Encoder[StatusEdit] = deriveEncoder[StatusEdit]

  implicit val walletDecoder: Decoder[Wallet] = deriveDecoder[Wallet]
  implicit val currencyDecoder: Decoder[Currency] = deriveDecoder[Currency]
  implicit val accountCurrencyDecoder: Decoder[AccountCurrency] = deriveDecoder[AccountCurrency]
  implicit val expandedAccountCurrencyDecoder: Decoder[ExpandedAccountCurrency] = deriveDecoder[ExpandedAccountCurrency]
  implicit val pureAccountDecoder: Decoder[PureAccount] = deriveDecoder[PureAccount]
  implicit val accountDecoder: Decoder[Account] = deriveDecoder[Account]
  implicit val categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  implicit val transactionDecoder: Decoder[Transaction] = deriveDecoder[Transaction]
  implicit val currencyBalanceDecoder: Decoder[CurrencyBalance] = deriveDecoder[CurrencyBalance]
  implicit val enhancedAccountDecoder: Decoder[EnhancedAccount] = deriveDecoder[EnhancedAccount]
  implicit val walletEditDecoder: Decoder[WalletEdit] = deriveDecoder[WalletEdit]
  implicit val statusEditDecoder: Decoder[StatusEdit] = deriveDecoder[StatusEdit]

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
