package org.big.pete

import io.circe.{Decoder, Encoder}
import io.circe.parser.parse
import io.circe.syntax._


object BPJson {
  def extract[T: Decoder](str: String): Either[String, T] =
    parse(str).left.map(_.message)
      .map(_.as[T])
      .map(_.left.map(_.message))
      .flatten

  def pureExtract[T: Decoder](str: String): T =
    extract[T](str) match {
      case Left(value) => throw new Exception(value)
      case Right(value) => value
    }

  def write[T: Encoder](obj: T): String =
    obj.asJson.noSpaces

  def pretty[T: Encoder](obj: T): String =
    obj.asJson.spaces2
}
