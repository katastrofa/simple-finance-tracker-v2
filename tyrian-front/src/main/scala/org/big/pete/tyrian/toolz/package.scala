package org.big.pete.tyrian

import org.big.pete.Mathjs
import org.big.pete.sft.domain.Category
import org.big.pete.tyrian.domain.CategoryExtension.*

import java.time.format.DateTimeFormatter
import scala.util.Try


package object toolz {
  def createPermalink(full: String): String =
    full.trim.toLowerCase
      .replaceAll("\\s+", "-")
      .replaceAll("[^a-z0-9_-]+", "_")

  final val DateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  final val SmallDateFormat = DateTimeFormatter.ofPattern("MM-dd")

  def fAmount(currencySymbol: String, amount: BigDecimal): String =
    "%s%.2f".format(currencySymbol, amount)

  def fDecimal(amount: BigDecimal): String =
    "%.2f".format(amount)

  def parseAmount(newAmount: String): Option[BigDecimal] = {
    Try(Mathjs.evaluate(newAmount))
      .toOption
      .map { value =>
        val bd = BigDecimal.decimal(value)
        bd.setScale(2, BigDecimal.RoundingMode.HALF_EVEN)
      }
  }
  
  def sortCategories(categories: Map[Int, Category]): List[Category] = {
    given cats: Map[Int, Category] = categories
    
    categories.values
      .map(cat => cat.fullName -> cat)
      .toList
      .sortBy(_._1)
      .map(_._2)
  }
}


