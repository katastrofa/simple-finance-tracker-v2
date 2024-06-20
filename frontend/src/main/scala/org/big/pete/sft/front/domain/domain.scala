package org.big.pete.sft.front.domain

import enumeratum.{Enum, EnumEntry}
import japgolly.scalajs.react.Reusability
import org.big.pete.sft.domain.{Wallet, Category, Currency, CurrencyBalance, EnhancedAccount, ExpandedAccountCurrency, AccountCurrency, AccountOptionalCurrency, Transaction, Status, Op}
import org.big.pete.sft.front.SftMain.SftPages

import java.time.LocalDate
import scala.annotation.tailrec


case class CategoryTree(id: Int, name: String, description: Option[String], treeLevel: Int, parent: Option[Int], children: List[CategoryTree]) {
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
        cat.parent,
        groupedData.getOrElse(Some(cat.id), List.empty[Category])
          .map(childCat => catToTree(childCat, level + 1))
      )
    }

    groupedData.getOrElse(None, List.empty[Category])
      .map(cat => catToTree(cat, 0))
  }

  def linearize(cats: List[CategoryTree]): List[CategoryTree] = {
    cats.flatMap(cat => cat :: linearize(cat.children))
  }

  def name(cat: CategoryTree): String =
    "-".repeat(cat.treeLevel) + " " + cat.name

  def fullName(categories: Map[Int, Category])(cat: CategoryTree): String = {
    val parentCats = parentTree(categories, Some(cat.id))
    parentCats.map(_.name).mkString(" - ")
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
    transactionType: Op,
    amount: BigDecimal,
    currency: Currency,
    description: String,
    categoryId: Int,
    categoryName: String,
    categoryFullName: String,
    moneyAccountId: Int,
    moneyAccountName: String,
    tracking: Status,
    destinationAmount: Option[BigDecimal],
    destinationCurrency: Option[Currency],
    destinationMoneyAccountId: Option[Int],
    destinationMoneyAccountName: Option[String]
)

object EnhancedTransaction {
  def enhance(
      categories: Map[Int, Category],
      moneyAccounts: Map[Int, EnhancedAccount],
      currencies: Map[String, Currency]
  )(
      transaction: Transaction
  ): EnhancedTransaction = {
    val moneyAccount = moneyAccounts(transaction.account)
    val destinationMoneyAccount = transaction.destinationAccount.map(moneyAccounts)
    val parentCats = CategoryTree.parentTree(categories, Some(transaction.category), List.empty)

    EnhancedTransaction(
      transaction.id,
      transaction.date,
      transaction.op,
      transaction.amount,
      currencies(transaction.currency),
      transaction.description,
      transaction.category,
      Range(0, parentCats.length - 1).map(_ => "--").mkString("") + " " + categories(transaction.category).name,
      parentCats.map(_.name).mkString(" - "),
      transaction.account,
      moneyAccount.name,
      transaction.status,
      transaction.destinationAmount,
      transaction.destinationCurrency.map(currencies),
      transaction.destinationAccount,
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

  implicit val accountReuse: Reusability[Wallet] = Reusability.derive[Wallet]
  implicit val sftPagesReuse: Reusability[SftPages] = Reusability.byRefOr_==[SftPages]
  implicit val sortingColumnReuse: Reusability[SortingColumn] = Reusability.byRefOr_==[SortingColumn]
  implicit val orderReuse: Reusability[Order] = Reusability.byRefOr_==[Order]
  implicit val transactionTypeReuse: Reusability[Op] = Reusability.by_==[Op]
  implicit val transactionTrackingReuse: Reusability[Status] = Reusability.by_==[Status]
  implicit val currencyReuse: Reusability[Currency] = Reusability.derive[Currency]
  implicit val currencyMapReuse: Reusability[Map[String, Currency]] = Reusability.map[String, Currency]
  implicit val moneyAccountCurrencyReuse: Reusability[AccountCurrency] = Reusability.derive[AccountCurrency]
  implicit val moneyAccountCurrencyMapReuse: Reusability[Map[Int, AccountCurrency]] = Reusability.map[Int, AccountCurrency]
  implicit val moneyAccountOptionalCurrencyReuse: Reusability[AccountOptionalCurrency] = Reusability.derive[AccountOptionalCurrency]
  implicit val moneyAccountOptionalCurrencyMapReuse: Reusability[Map[Int, AccountOptionalCurrency]] = Reusability.map[Int, AccountOptionalCurrency]
  implicit val categoryReuse: Reusability[Category] = Reusability.derive[Category]
  implicit val categoryMapReuse: Reusability[Map[Int, Category]] = Reusability.map[Int, Category]
  implicit val categoryTreeReuse: Reusability[CategoryTree] = Reusability.by_==[CategoryTree]
  implicit val currencyAndStatusReuse: Reusability[CurrencyBalance] = Reusability.derive[CurrencyBalance]
  implicit val expandedMoneyAccountCurrencyReuse: Reusability[ExpandedAccountCurrency] = Reusability.derive[ExpandedAccountCurrency]
  implicit val enhancedMoneyAccountReuse: Reusability[EnhancedAccount] = Reusability.derive[EnhancedAccount]
  implicit val moneyAccountMapReuse: Reusability[Map[Int, EnhancedAccount]] = Reusability.map[Int, EnhancedAccount]
  implicit val enhancedTransactionReuse: Reusability[EnhancedTransaction] = Reusability.derive[EnhancedTransaction]
}

object domain {}
