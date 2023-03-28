package org.big.pete.sft.front.domain

import enumeratum.{Enum, EnumEntry}
import japgolly.scalajs.react.Reusability
import org.big.pete.domain.DDItem
import org.big.pete.react.MICheckbox
import org.big.pete.sft.domain.{Account, Category, Currency, CurrencyAndStatus, EnhancedMoneyAccount, ExpandedMoneyAccountCurrency, FullAccount, MoneyAccountCurrency, MoneyAccountOptionalCurrency, SimpleUser, Transaction, TransactionTracking, TransactionType}
import org.big.pete.sft.front.SftMain.SftPages

import java.time.LocalDate
import scala.annotation.tailrec


case class CategoryTree(
    id: Int,
    name: String,
    description: Option[String],
    expandedDisplayName: String,
    treeLevel: Int,
    parent: Option[Int],
    children: List[CategoryTree]
) extends DDItem {
  override def equals(obj: Any): Boolean = {
    if (!obj.isInstanceOf[CategoryTree])
      false
    else {
      val o = obj.asInstanceOf[CategoryTree]
      id == o.id && name == o.name && description == o.description && treeLevel == o.treeLevel && children == o.children
    }
  }

  def shortDisplayName: String =
    "-".repeat(treeLevel) + " " + name

  override def ddId: String = id.toString
  override def ddDisplayName: String = expandedDisplayName
}

object CategoryTree {

  def generateTree(categories: List[Category]): List[CategoryTree] = {
    val groupedData = categories.groupBy(_.parent)

    def catToTree(cat: Category, level: Int, parents: List[Category]): CategoryTree = {
      CategoryTree(
        cat.id,
        cat.name,
        cat.description,
        parents.map(_.name).mkString(" - ") + " - " + cat.name,
        level,
        cat.parent,
        groupedData.getOrElse(Some(cat.id), List.empty[Category])
          .map(childCat => catToTree(childCat, level + 1, parents ++ List(cat)))
      )
    }

    groupedData.getOrElse(None, List.empty[Category])
      .map(cat => catToTree(cat, 0, List.empty))
  }

  def linearize(cats: List[CategoryTree]): List[CategoryTree] = {
    cats.flatMap(cat => cat :: linearize(cat.children))
  }

  @tailrec
  def parentTree(categories: Map[Int, Category], catId: Option[Int], list: List[Category] = List.empty): List[Category] =
    catId.map(categories.apply) match {
      case None => list
      case Some(cat) => parentTree(categories, cat.parent, cat :: list)
    }
}

case class EnhancedTransaction(
    id: Int,
    date: LocalDate,
    transactionType: TransactionType,
    amount: BigDecimal,
    currency: Currency,
    description: String,
    categoryId: Int,
    categoryName: String,
    categoryFullName: String,
    moneyAccountId: Int,
    moneyAccountName: String,
    tracking: TransactionTracking,
    destinationAmount: Option[BigDecimal],
    destinationCurrency: Option[Currency],
    destinationMoneyAccountId: Option[Int],
    destinationMoneyAccountName: Option[String]
)

case class TransactionEntry(
    transaction: EnhancedTransaction,
    checked: MICheckbox.Status
)

object EnhancedTransaction {
  def enhance(
      categories: Map[Int, Category],
      moneyAccounts: Map[Int, EnhancedMoneyAccount],
      currencies: Map[String, Currency]
  )(
      transaction: Transaction
  ): EnhancedTransaction = {
    val moneyAccount = moneyAccounts(transaction.moneyAccount)
    val destinationMoneyAccount = transaction.destinationMoneyAccountId.map(moneyAccounts)
    val parentCats = CategoryTree.parentTree(categories, Some(transaction.categoryId), List.empty)

    EnhancedTransaction(
      transaction.id,
      transaction.date,
      transaction.transactionType,
      transaction.amount,
      currencies(transaction.currency),
      transaction.description,
      transaction.categoryId,
      Range(0, parentCats.length - 1).map(_ => "--").mkString("") + " " + categories(transaction.categoryId).name,
      parentCats.map(_.name).mkString(" - "),
      transaction.moneyAccount,
      moneyAccount.name,
      transaction.tracking,
      transaction.destinationAmount,
      transaction.destinationCurrency.map(currencies),
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

sealed trait SortingColumn extends EnumEntry
case object SortingColumn extends Enum[SortingColumn] {
  final case object Date extends SortingColumn
  final case object Description extends SortingColumn
  final case object Amount extends SortingColumn

  val values: IndexedSeq[SortingColumn] = findValues
}

sealed trait Order extends EnumEntry
case object Order extends Enum[Order] {
  final case object Asc extends Order
  final case object Desc extends Order
  val values: IndexedSeq[Order] = findValues
}



object Implicits {
  import org.big.pete.react.Implicits._

  implicit val stringIntMapReuse: Reusability[Map[String, Int]] = Reusability.map[String, Int]

  implicit val simpleUserReuse: Reusability[SimpleUser] = Reusability.derive[SimpleUser]
  implicit val simpleUserMapReuse: Reusability[Map[Int, SimpleUser]] = Reusability.map[Int, SimpleUser]
  implicit val accountReuse: Reusability[Account] = Reusability.derive[Account]
  implicit val fullAccountReuse: Reusability[FullAccount] = Reusability.derive[FullAccount]
  implicit val sftPagesReuse: Reusability[SftPages] = Reusability.byRefOr_==[SftPages]
  implicit val sortingColumnReuse: Reusability[SortingColumn] = Reusability.byRefOr_==[SortingColumn]
  implicit val orderReuse: Reusability[Order] = Reusability.byRefOr_==[Order]
  implicit val transactionTypeReuse: Reusability[TransactionType] = Reusability.by_==[TransactionType]
  implicit val transactionTrackingReuse: Reusability[TransactionTracking] = Reusability.by_==[TransactionTracking]
  implicit val currencyReuse: Reusability[Currency] = Reusability.derive[Currency]
  implicit val currencyMapReuse: Reusability[Map[String, Currency]] = Reusability.map[String, Currency]
  implicit val moneyAccountCurrencyReuse: Reusability[MoneyAccountCurrency] = Reusability.derive[MoneyAccountCurrency]
  implicit val moneyAccountCurrencyMapReuse: Reusability[Map[Int, MoneyAccountCurrency]] = Reusability.map[Int, MoneyAccountCurrency]
  implicit val moneyAccountOptionalCurrencyReuse: Reusability[MoneyAccountOptionalCurrency] = Reusability.derive[MoneyAccountOptionalCurrency]
  implicit val moneyAccountOptionalCurrencyMapReuse: Reusability[Map[Int, MoneyAccountOptionalCurrency]] = Reusability.map[Int, MoneyAccountOptionalCurrency]
  implicit val categoryReuse: Reusability[Category] = Reusability.derive[Category]
  implicit val categoryMapReuse: Reusability[Map[Int, Category]] = Reusability.map[Int, Category]
  implicit val categoryTreeReuse: Reusability[CategoryTree] = Reusability.by_==[CategoryTree]
  implicit val currencyAndStatusReuse: Reusability[CurrencyAndStatus] = Reusability.derive[CurrencyAndStatus]
  implicit val expandedMoneyAccountCurrencyReuse: Reusability[ExpandedMoneyAccountCurrency] = Reusability.derive[ExpandedMoneyAccountCurrency]
  implicit val enhancedMoneyAccountReuse: Reusability[EnhancedMoneyAccount] = Reusability.derive[EnhancedMoneyAccount]
  implicit val moneyAccountMapReuse: Reusability[Map[Int, EnhancedMoneyAccount]] = Reusability.map[Int, EnhancedMoneyAccount]
  implicit val enhancedTransactionReuse: Reusability[EnhancedTransaction] = Reusability.derive[EnhancedTransaction]
}

object domain {}
