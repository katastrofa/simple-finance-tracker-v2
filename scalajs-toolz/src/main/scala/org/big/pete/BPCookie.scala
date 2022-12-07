package org.big.pete

import io.circe.{Decoder, Encoder}


object BPCookie {
  def getObj[T: Decoder](name: String): Option[T] = {
    val cookie = Cookies.get(name)
    if (cookie.isEmpty)
      None
    else
      BPJson.extract[T](cookie.get).toOption
  }

  def setObj[T: Encoder](name: String, value: T, attributes: CookieAttributes): String = {
    val valueStr = BPJson.write(value)
    Cookies.set(name, valueStr, attributes)
  }
}
