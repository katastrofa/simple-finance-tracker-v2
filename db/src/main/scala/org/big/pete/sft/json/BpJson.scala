package org.big.pete.sft.json

import cats.syntax.EitherSyntax
import io.circe.{Decoder, Encoder, Json}
import io.circe.jawn.parse
import io.circe.syntax._


object BpJson extends EitherSyntax {
  def extract[T: Decoder](str: String): Either[io.circe.Error, T] =
    parse(str).flatMap(_.as[T])

  def extract[T: Decoder](json: Json): Either[io.circe.Error, T] =
    json.as[T]

  def write[T: Encoder](obj: T): String =
    obj.asJson.noSpaces

  def pretty[T: Encoder](obj: T): String =
    obj.asJson.spaces2
}
