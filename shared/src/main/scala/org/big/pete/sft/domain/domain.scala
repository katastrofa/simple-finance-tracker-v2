package org.big.pete.sft.domain

import enumeratum.{Enum, EnumEntry}

import java.time.{LocalDate, LocalDateTime}


sealed trait TransactionType extends EnumEntry
object TransactionType extends Enum[TransactionType] {
  final case object Income extends TransactionType
  final case object Expense extends TransactionType
  final case object Transfer extends TransactionType

  val values: IndexedSeq[TransactionType] = findValues
}

sealed trait TransactionTracking extends EnumEntry
object TransactionTracking extends Enum[TransactionTracking] {
  final case object None extends TransactionTracking
  final case object Auto extends TransactionTracking
  final case object Verified extends TransactionTracking

  val values: IndexedSeq[TransactionTracking] = findValues
}

case class Account(id: Int, name: String, permalink: String)
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

object domain {

}
