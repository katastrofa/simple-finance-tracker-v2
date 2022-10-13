package org.big.pete.sft

import java.time.LocalDate


object DateOrdering extends Ordering[LocalDate] {
  override def compare(x: LocalDate, y: LocalDate): Int =
    x.compareTo(y)
}
