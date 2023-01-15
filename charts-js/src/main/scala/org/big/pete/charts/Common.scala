package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


@js.native
trait LTRB extends js.Object {
  @nowarn var left: js.UndefOr[Int | Boolean] = js.native
  @nowarn var top: js.UndefOr[Int | Boolean] = js.native
  @nowarn var right: js.UndefOr[Int | Boolean] = js.native
  @nowarn var bottom: js.UndefOr[Int | Boolean] = js.native
}

@js.native
trait ChParsing extends js.Object {
  @nowarn var key: js.UndefOr[String] = js.native
  @nowarn var xAxisKey: js.UndefOr[String] = js.native
  @nowarn var yAxisKey: js.UndefOr[String] = js.native
}

object ChParsing {
  def apply(
      key: Option[String] = None,
      xAxisKey: Option[String] = None,
      yAxisKey: Option[String]= None
  ): ChParsing =
    cleanObject(Map(
      "key" -> key,
      "xAxisKey" -> xAxisKey,
      "yAxisKey" -> yAxisKey
    )).asInstanceOf[ChParsing]
}

@js.native
trait BorderRadius extends js.Object {
  @nowarn var topLeft: js.UndefOr[Int] = js.native
  @nowarn var topRight: js.UndefOr[Int] = js.native
  @nowarn var bottomLeft: js.UndefOr[Int] = js.native
  @nowarn var bottomRight: js.UndefOr[Int] = js.native
}

@js.native
trait ChFont extends js.Object {
  @nowarn var family: js.UndefOr[String] = js.native
  @nowarn var size: js.UndefOr[Int] = js.native
  @nowarn var style: js.UndefOr[String] = js.native
  @nowarn var weight: js.UndefOr[String] = js.native
  @nowarn var lineHeight: js.UndefOr[String | ChJsNumber] = js.native
}

object ChFont {
  def apply(
      family: Option[String] = None,
      size: Option[Int] = None,
      style: Option[String] = None,
      weight: Option[String] = None,
      lineHeight: Option[String | ChJsNumber] = None,
  ): ChFont =
    cleanObject(Map(
      "family" -> family.orUndefined,
      "size" -> size.orUndefined,
      "style" -> style.orUndefined,
      "weight" -> weight.orUndefined,
      "lineHeight" -> lineHeight.orUndefined
    )).asInstanceOf[ChFont]
}

object Common {

}
