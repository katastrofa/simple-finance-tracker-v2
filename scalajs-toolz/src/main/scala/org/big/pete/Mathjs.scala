package org.big.pete

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport


/// Facade for https://github.com/josdejong/mathjs
@js.native
@JSImport("mathjs", JSImport.Default)
object Mathjs extends js.Object {

  @nowarn
  def evaluate(expr: String): Double = js.native
}
