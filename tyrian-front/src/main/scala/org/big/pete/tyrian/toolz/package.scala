package org.big.pete.tyrian

import java.time.format.DateTimeFormatter


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
}


