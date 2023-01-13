package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


@js.native
trait TitleSpec extends js.Object {
  @nowarn var align: String = js.native
  @nowarn var color: Color = js.native
  @nowarn var display: Boolean = js.native
  @nowarn var fullSize: Boolean = js.native
  @nowarn var position: String = js.native
  @nowarn var font: ChFont = js.native
  @nowarn var padding: Padding = js.native
  @nowarn var text: String | js.Array[String] = js.native
}

object TitleSpec {
  def apply(
      align: Option[String] = None,
      color: Option[Color] = None,
      display: Option[Boolean] = None,
      fullSize: Option[Boolean] = None,
      position: Option[String] = None,
      font: Option[ChFont] = None,
      padding: Option[Padding] = None,
      text: Option[String | js.Array[String]] = None
  ): TitleSpec =
    cleanObject(Map(
      "align" -> align.orUndefined,
      "color" -> color.orUndefined,
      "display" -> display.orUndefined,
      "fullSize" -> fullSize.orUndefined,
      "position" -> position.orUndefined,
      "font" -> font.orUndefined,
      "padding" -> padding.orUndefined,
      "text" -> text.orUndefined
    )).asInstanceOf[TitleSpec]
}

object Title {

}
