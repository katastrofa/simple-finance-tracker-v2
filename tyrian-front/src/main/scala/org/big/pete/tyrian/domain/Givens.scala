package org.big.pete.tyrian.domain

import org.big.pete.sft.domain.{Account, Category, Currency, Op}
import org.big.pete.tyrian.component.DropDownItem


object Givens {
  given DropDownItem[Op] with {
    extension (op: Op) def key: String = op.toString
    extension (op: Op) def display: String = op.toString
  }
  
  given DropDownItem[Account] with {
    extension (account: Account) def key: String = account.id.toString
    extension (account: Account) def display: String = account.name
  }
  
  given DropDownItem[Currency] with {
    extension (currency: Currency) def key: String = currency.id
    extension (currency: Currency) def display: String = currency.name
  }
  
  class CatDropDownItem(using categories: Map[Int, Category]) extends DropDownItem[Category] {
    extension (cat: Category) def key: String = cat.id.toString
    extension (cat: Category) def display: String = parentCats(Some(cat.id), List.empty).map(_.name).mkString(" - ")
  }
}
