package org.big.pete.react

import japgolly.scalajs.react.Reusability


object Implicits {
  implicit val bigDecimalReuse: Reusability[BigDecimal] = Reusability.by_==[BigDecimal]
  implicit val doubleReuse: Reusability[Double] = Reusability.by_==[Double]
  implicit val floatReuse: Reusability[Float] = Reusability.by_==[Float]
}
