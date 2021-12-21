package org.big.pete.sft.front.domain

import org.big.pete.sft.domain.{Category, EnhancedMoneyAccount, Transaction, TransactionTracking, TransactionType}

import java.time.LocalDate
import scala.annotation.tailrec


case class CategoryTree(id: Int, name: String, description: Option[String], treeLevel: Int, children: List[CategoryTree])

object CategoryTree {
  def generateTree(categories: List[Category]): List[CategoryTree] = {
    val groupedData = categories.groupBy(_.parent)

    def catToTree(cat: Category, level: Int): CategoryTree = {
      CategoryTree(
        cat.id,
        cat.name,
        cat.description,
        level,
        groupedData.getOrElse(Some(cat.id), List.empty[Category])
          .map(childCat => catToTree(childCat, level + 1))
      )
    }

    groupedData.getOrElse(None, List.empty[Category])
      .map(cat => catToTree(cat, 0))
  }
}

case class EnhancedTransaction(
    id: Int,
    date: LocalDate,
    transactionType: TransactionType,
    amount: BigDecimal,
    currencySymbol: String,
    description: String,
    categoryId: Int,
    categoryName: String,
    categoryFullName: String,
    moneyAccountId: Int,
    moneyAccountName: String,
    tracking: TransactionTracking,
    destinationAmount: Option[BigDecimal],
    destinationCurrencySymbol: Option[String],
    destinationMoneyAccountId: Option[Int],
    destinationMoneyAccountName: Option[String]
)

object EnhancedTransaction {
  def enhance(
      categories: Map[Int, Category],
      moneyAccounts: Map[Int, EnhancedMoneyAccount]
  )(
      transaction: Transaction
  ): EnhancedTransaction = {
    @tailrec
    def parentTree(catId: Option[Int], list: List[Category]): List[Category] = catId.map(categories.apply) match {
      case None => list
      case Some(cat) => parentTree(cat.parent, cat :: list)
    }

    val moneyAccount = moneyAccounts(transaction.moneyAccount)
    val destinationMoneyAccount = transaction.destinationMoneyAccountId.map(moneyAccounts)
    val parentCats = parentTree(Some(transaction.categoryId), List.empty)

    EnhancedTransaction(
      transaction.id,
      transaction.date,
      transaction.transactionType,
      transaction.amount,
      moneyAccount.currency.symbol,
      transaction.description,
      transaction.categoryId,
      Range(0, parentCats.length - 1).map(_ => "--").mkString("") + " " + categories(transaction.categoryId).name,
      parentCats.map(_.name).mkString(" - "),
      transaction.moneyAccount,
      moneyAccount.name,
      transaction.tracking,
      transaction.destinationAmount,
      destinationMoneyAccount.map(_.currency.symbol),
      transaction.destinationMoneyAccountId,
      destinationMoneyAccount.map(_.name)
    )
  }
}

object domain {

}
