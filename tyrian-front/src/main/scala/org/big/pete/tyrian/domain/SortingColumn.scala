package org.big.pete.tyrian.domain

import org.big.pete.sft.DateOrdering
import org.big.pete.sft.domain.Transaction

import java.time.LocalDate
import scala.annotation.tailrec


enum SortingOrder {
  case Asc
  case Desc
}

enum SortingName {
  case Date
  case Description
  case Amount
}

sealed trait SortingColumn[T] {
  def name: SortingName
  def order: SortingOrder
  def col: Transaction => T
  def ordering: Ordering[T]
}


case class Date(order: SortingOrder) extends SortingColumn[LocalDate] {
  override val name: SortingName = SortingName.Date
  override val col: Transaction => LocalDate = _.date
  override val ordering: Ordering[LocalDate] = order match {
    case SortingOrder.Asc => DateOrdering
    case SortingOrder.Desc => DateOrdering.reverse
  }
}
case class Description(order: SortingOrder) extends SortingColumn[String] {
  override val name: SortingName = SortingName.Description
  override val col: Transaction => String = _.description
  override val ordering: Ordering[String] = order match {
    case SortingOrder.Asc => Ordering.by[String, String](_.toLowerCase)
    case SortingOrder.Desc => Ordering.by[String, String](_.toLowerCase).reverse
  }
}
case class Amount(order: SortingOrder) extends SortingColumn[BigDecimal] {
  override val name: SortingName = SortingName.Amount
  override val col: Transaction => BigDecimal = _.amount
  override val ordering: Ordering[BigDecimal] = order match {
    case SortingOrder.Asc => Ordering.by[BigDecimal, BigDecimal](_.abs)
    case SortingOrder.Desc => Ordering.by[BigDecimal, BigDecimal](_.abs).reverse
  }
}

object SortingColumn {
  def apply(name: SortingName, order: SortingOrder): SortingColumn[?] = name match {
    case SortingName.Date => Date(order)
    case SortingName.Description => Description(order)
    case SortingName.Amount => Amount(order)
  }
}

final class TransactionOrdering(sorting: List[SortingColumn[?]]) extends Ordering[Transaction] {
  override def compare(x: Transaction, y: Transaction): Int =
    recurse(x, y, sorting)

  @tailrec
  private def recurse(x: Transaction, y: Transaction, tail: List[SortingColumn[?]]): Int = {
    tail match {
      case Nil => 0
      case head :: next =>
        val result = head.ordering.compare(head.col(x), head.col(y))
        if (result != 0) result else recurse(x, y, next)
    }
  }
}
