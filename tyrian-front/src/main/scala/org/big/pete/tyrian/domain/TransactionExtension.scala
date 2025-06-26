package org.big.pete.tyrian.domain

import org.big.pete.sft.domain.{Category, Currency, Transaction}
import org.big.pete.tyrian.toolz

import scala.annotation.tailrec


object TransactionExtension {
  extension (t: Transaction) {
    def fAmount(using currencies: Map[String, Currency]): String =
      toolz.fAmount(currencies(t.currency).symbol, t.amount)

    def fDAmount(using currencies: Map[String, Currency]): String =
      t.destinationAmount.map(amount => toolz.fAmount(currencies(t.currency).symbol, amount)).getOrElse("")

    def fullCategoryName(using categories: Map[Int, Category]): String =
      parentCats(Some(t.category), List.empty).map(_.name).mkString(" - ")
      
    def categoryName(using categories: Map[Int, Category]): String =
      categories(t.category).name
  }
}
