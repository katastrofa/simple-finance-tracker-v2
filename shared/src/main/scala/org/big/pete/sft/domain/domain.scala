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

sealed trait ApiResponse
sealed trait ApiRequest

case class Wallet(id: Int, name: String, permalink: String, owner: Option[Int]) extends ApiResponse with ApiRequest

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

final case class User(id: Int, email: String, displayName: String, permissions: UserPermissions)
final case class UserPermissions(global: Set[ApiAction], perWallet: Map[Int, Set[ApiAction]], default: Set[ApiAction])


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
  given walletEncoder: Encoder[Wallet] = deriveEncoder[Wallet]
  given currencyEncoder: Encoder[Currency] = deriveEncoder[Currency]
  given accountCurrencyEncoder: Encoder[AccountCurrency] = deriveEncoder[AccountCurrency]
  given expandedAccountCurrencyEncoder: Encoder[ExpandedAccountCurrency] = deriveEncoder[ExpandedAccountCurrency]
  given pureAccountEncoder: Encoder[PureAccount] = deriveEncoder[PureAccount]
  given accountEncoder: Encoder[Account] = deriveEncoder[Account]
  given categoryEncoder: Encoder[Category] = deriveEncoder[Category]
  given transactionEncoder: Encoder[Transaction] = deriveEncoder[Transaction]
  given currencyBalanceEncoder: Encoder[CurrencyBalance] = deriveEncoder[CurrencyBalance]
  given enhancedAccountEncoder: Encoder[EnhancedAccount] = deriveEncoder[EnhancedAccount]
  given walletEditEncoder: Encoder[WalletEdit] = deriveEncoder[WalletEdit]
  given statusEditEncoder: Encoder[StatusEdit] = deriveEncoder[StatusEdit]

  given walletDecoder: Decoder[Wallet] = deriveDecoder[Wallet]
  given currencyDecoder: Decoder[Currency] = deriveDecoder[Currency]
  given accountCurrencyDecoder: Decoder[AccountCurrency] = deriveDecoder[AccountCurrency]
  given expandedAccountCurrencyDecoder: Decoder[ExpandedAccountCurrency] = deriveDecoder[ExpandedAccountCurrency]
  given pureAccountDecoder: Decoder[PureAccount] = deriveDecoder[PureAccount]
  given accountDecoder: Decoder[Account] = deriveDecoder[Account]
  given categoryDecoder: Decoder[Category] = deriveDecoder[Category]
  given transactionDecoder: Decoder[Transaction] = deriveDecoder[Transaction]
  given currencyBalanceDecoder: Decoder[CurrencyBalance] = deriveDecoder[CurrencyBalance]
  given enhancedAccountDecoder: Decoder[EnhancedAccount] = deriveDecoder[EnhancedAccount]
  given walletEditDecoder: Decoder[WalletEdit] = deriveDecoder[WalletEdit]
  given statusEditDecoder: Decoder[StatusEdit] = deriveDecoder[StatusEdit]

  given userEncoder: Encoder[User] = deriveEncoder[User]
  given userDecoder: Decoder[User] = deriveDecoder[User]
  given userPermissionsEncoder: Encoder[UserPermissions] = deriveEncoder[UserPermissions]
  given userPermissionsDecoder: Decoder[UserPermissions] = deriveDecoder[UserPermissions]

  given notAllowedResponseEncoder: Encoder[NotAllowedResponse] = deriveEncoder[NotAllowedResponse]
  given notAllowedResponseDecoder: Decoder[NotAllowedResponse] = deriveDecoder[NotAllowedResponse]
  given shiftStrategyEncoder: Encoder[ShiftStrategy] = deriveEncoder[ShiftStrategy]
  given shiftStrategyDecoder: Decoder[ShiftStrategy] = deriveDecoder[ShiftStrategy]
  given shiftStrategyPerCurrencyEncoder: Encoder[ShiftStrategyPerCurrency] = deriveEncoder[ShiftStrategyPerCurrency]
  given shiftStrategyPerCurrencyDecoder: Decoder[ShiftStrategyPerCurrency] = deriveDecoder[ShiftStrategyPerCurrency]
  given categoryDeleteStrategiesEncoder: Encoder[CategoryDeleteStrategies] = deriveEncoder[CategoryDeleteStrategies]
  given categoryDeleteStrategiesDecoder: Decoder[CategoryDeleteStrategies] = deriveDecoder[CategoryDeleteStrategies]
  given accountDeleteStrategyEncoder: Encoder[AccountDeleteStrategy] = deriveEncoder[AccountDeleteStrategy]
  given accountDeleteStrategyDecoder: Decoder[AccountDeleteStrategy] = deriveDecoder[AccountDeleteStrategy]
  given deleteTransactionsEncoder: Encoder[DeleteTransactions] = deriveEncoder[DeleteTransactions]
  given deleteTransactionsDecoder: Decoder[DeleteTransactions] = deriveDecoder[DeleteTransactions]
  given massEditTransactionsEncoder: Encoder[MassEditTransactions] = deriveEncoder[MassEditTransactions]
  given massEditTransactionsDecoder: Decoder[MassEditTransactions] = deriveDecoder[MassEditTransactions]
  
  given apiRequestEncoder: Encoder[ApiRequest] = Encoder.instance {
    case wallet: Wallet => walletEncoder(wallet)
  }
}

object domain {

}
