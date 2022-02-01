package org.big.pete.react

import japgolly.scalajs.react.Reusability


object Implicits {
  implicit val bigDecimalReuse: Reusability[BigDecimal] = Reusability.by_==[BigDecimal]
}
