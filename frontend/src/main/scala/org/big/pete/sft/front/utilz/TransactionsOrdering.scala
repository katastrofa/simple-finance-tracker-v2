package org.big.pete.sft.front.utilz

import org.big.pete.sft.DateOrdering
import org.big.pete.sft.front.domain.{EnhancedTransaction, Order, SortingColumn}

import scala.annotation.tailrec


class TransactionsOrdering(sorting: List[(SortingColumn, Order)]) extends Ordering[EnhancedTransaction] {

  private def getOrdering[T](ord: Ordering[T], order: Order): Ordering[T] = order match {
    case Order.Asc => ord
    case Order.Desc => ord.reverse
  }

  @tailrec
  private def recurse(x: EnhancedTransaction, y: EnhancedTransaction, tail: List[(SortingColumn, Order)]): Int = {
    tail match {
      case Nil => 0
      case (sort, order) :: next =>
        sort match {
          case SortingColumn.Date =>
            val result = getOrdering(DateOrdering, order).compare(x.date, y.date)
            if (result != 0) result else recurse(x, y, next)

          case SortingColumn.Description =>
            val result = getOrdering(Ordering.String, order).compare(x.description, y.description)
            if (result != 0) result else recurse(x, y, next)

          case SortingColumn.Amount =>
            val result = getOrdering(Ordering.BigDecimal, order).compare(x.amount, y.amount)
            if (result != 0) result else recurse(x, y, next)
        }
    }
  }

  override def compare(x: EnhancedTransaction, y: EnhancedTransaction): Int =
    recurse(x, y, sorting)
}
