package org.big.pete

//import scala.annotation.nowarn
import scalajs.js
import scalajs.js.|
import scalajs.js.annotation.JSImport


/// Facade for https://github.com/js-cookie/js-cookie/tree/latest
@js.native
@JSImport("js-cookie", JSImport.Default)
object Cookies extends js.Object {

  def get(): js.Dictionary[String] = js.native
//  @nowarn
  def get(name: String): js.UndefOr[String] = js.native

//  @nowarn
  def set(name: String, value: String): String = js.native
//  @nowarn
  def set(name: String, value: String, attributes: CookieAttributes): String = js.native

//  @nowarn
  def remove(name: String): Unit = js.native
//  @nowarn
  def remove(name: String, attributes: CookieAttributes): Unit = js.native
}

class CookieAttributes(
    var expires: js.UndefOr[Int | js.Date] = js.undefined,
    var path: js.UndefOr[String] = js.undefined,
    var domain: js.UndefOr[String] = js.undefined,
    var secure: js.UndefOr[Boolean] = js.undefined,
    var sameSite: js.UndefOr[String] = js.undefined
) extends js.Object
