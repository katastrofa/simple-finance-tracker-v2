package org.big.pete.sft.front.domain

import enumeratum.{Enum, EnumEntry}
import japgolly.scalajs.react.Reusability
import org.big.pete.sft.domain.{Account, Category, Currency, EnhancedMoneyAccount, PeriodAmountStatus, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.SftPages

import java.time.LocalDate
import scala.annotation.tailrec


case class CategoryTree(id: Int, name: String, description: Option[String], treeLevel: Int, children: List[CategoryTree]) {
  override def equals(obj: Any): Boolean = {
    if (!obj.isInstanceOf[CategoryTree])
      false
    else {
      val o = obj.asInstanceOf[CategoryTree]
      id == o.id && name == o.name && description == o.description && treeLevel == o.treeLevel && children == o.children
    }
  }
}

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

  def makeLinearCats(cats: List[CategoryTree]): List[CategoryTree] = {
    cats.flatMap(cat => cat :: makeLinearCats(cat.children))
  }

  def name(cat: CategoryTree): String =
    "-".repeat(cat.treeLevel) + " " + cat.name
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

sealed trait MAUpdateAction extends EnumEntry
case object MAUpdateAction extends Enum[MAUpdateAction] {
  final case object Attach extends MAUpdateAction
  final case object Reverse extends MAUpdateAction

  val values: IndexedSeq[MAUpdateAction] = findValues
}

sealed trait MAUpdateOperation extends EnumEntry
case object MAUpdateOperation extends Enum[MAUpdateOperation] {
  final case object Add extends MAUpdateOperation
  final case object Remove extends MAUpdateOperation

  val values: IndexedSeq[MAUpdateOperation] = findValues
}

object Implicits {
  import org.big.pete.react.Implicits._

  implicit val accountReuse: Reusability[Account] = Reusability.derive[Account]
  implicit val sftPagesReuse: Reusability[SftPages] = Reusability.byRefOr_==[SftPages]
  implicit val transactionTypeReuse: Reusability[TransactionType] = Reusability.by_==[TransactionType]
  implicit val transactionTrackingReuse: Reusability[TransactionTracking] = Reusability.by_==[TransactionTracking]
  implicit val currencyReuse: Reusability[Currency] = Reusability.derive[Currency]
  implicit val categoryTreeReuse: Reusability[CategoryTree] = Reusability.by_==[CategoryTree]
  implicit val periodAmountStatusReuse: Reusability[PeriodAmountStatus] = Reusability.derive[PeriodAmountStatus]
  implicit val enhancedMoneyAccountReuse: Reusability[EnhancedMoneyAccount] = Reusability.derive[EnhancedMoneyAccount]

}

object domain {

}
