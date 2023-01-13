package org.big.pete.charts

import org.scalajs.dom.{HTMLCanvasElement, HTMLImageElement}

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


@js.native
trait LegendItem extends js.Object {
  @nowarn var text: String = js.native
  @nowarn var borderRadius: js.UndefOr[Int | BorderRadius] = js.native
  @nowarn var datasetIndex: Int = js.native
  @nowarn var index: Int = js.native
  @nowarn var fillStyle: Color = js.native
  @nowarn var fontColor: Color = js.native
  @nowarn var hidden: Boolean = js.native
  @nowarn var lineCap: String = js.native
  @nowarn var lineDash: js.Array[Int] = js.native
  @nowarn var lineDashOffset: Int = js.native
  @nowarn var lineJoin: String = js.native
  @nowarn var lineWidth: Int = js.native
  @nowarn var strokeStyle: Color = js.native
  @nowarn var pointStyle: String | Boolean | HTMLImageElement | HTMLCanvasElement = js.native
  @nowarn var rotation: Int = js.native
  @nowarn var textAlign: String = js.native
}

object LegendItem {
  def apply(
      text: String,
      borderRadius: Option[Int | BorderRadius] = None,
      datasetIndex: Option[Int] = None,
      index: Option[Int] = None,
      fillStyle: Option[Color] = None,
      fontColor: Option[Color] = None,
      hidden: Option[Boolean] = None,
      lineCap: Option[String] = None,
      lineDash: Option[Array[Int]] = None,
      lineDashOffset: Option[Int] = None,
      lineJoin: Option[String] = None,
      lineWidth: Option[Int] = None,
      strokeStyle: Option[Color] = None,
      pointStyle: Option[String | Boolean | HTMLImageElement | HTMLCanvasElement] = None,
      rotation: Option[Int] = None,
      textAlign: Option[String] = None
  ): LegendItem =
    cleanObject(Map(
      "text" -> text,
      "borderRadius" -> borderRadius.orUndefined,
      "datasetIndex" -> datasetIndex.orUndefined,
      "index" -> index.orUndefined,
      "fillStyle" -> fillStyle.orUndefined,
      "fontColor" -> fontColor.orUndefined,
      "hidden" -> hidden.orUndefined,
      "lineCap" -> lineCap.orUndefined,
      "lineDash" -> lineDash.orUndefined,
      "lineDashOffset" -> lineDashOffset.orUndefined,
      "lineJoin" -> lineJoin.orUndefined,
      "lineWidth" -> lineWidth.orUndefined,
      "strokeStyle" -> strokeStyle.orUndefined,
      "pointStyle" -> pointStyle.orUndefined,
      "rotation" -> rotation.orUndefined,
      "textAlign" -> textAlign.orUndefined,
    )).asInstanceOf[LegendItem]
}


@js.native
trait LegendSpec[DS <: Dataset[_]] extends js.Object {
  @nowarn var display: Boolean = js.native
  @nowarn var position: String = js.native
  @nowarn var align: String = js.native
  @nowarn var maxHeight: Int = js.native
  @nowarn var maxWidth: Int = js.native
  @nowarn var fullSize: Boolean = js.native
  @nowarn var labels: LegendLabelSpec[DS] = js.native
  @nowarn var title: LegendTitleSpec = js.native
}

object LegendSpec {
  def apply[DS <: Dataset[_]](
      display: Option[Boolean] = None,
      position: Option[String] = None,
      align: Option[String] = None,
      maxHeight: Option[Int] = None,
      maxWidth: Option[Int] = None,
      fullSize: Option[Boolean] = None,
      labels: Option[LegendLabelSpec[DS]] = None,
      title: Option[LegendTitleSpec] = None
  ): LegendSpec[DS] =
    cleanObject(Map(
      "display" -> display.orUndefined,
      "position" -> position.orUndefined,
      "align" -> align.orUndefined,
      "maxHeight" -> maxHeight.orUndefined,
      "maxWidth" -> maxWidth.orUndefined,
      "fullSize" -> fullSize.orUndefined,
      "labels" -> labels.orUndefined,
      "title" -> title.orUndefined
    )).asInstanceOf[LegendSpec[DS]]
}


@js.native
trait LegendLabelSpec[DS <: Dataset[_]] extends js.Object {
  @nowarn var boxWidth: Int = js.native
  @nowarn var boxHeight: Int = js.native
  @nowarn var color: Color = js.native
  @nowarn var font: ChFont = js.native
  @nowarn var textAlign: String = js.native
  @nowarn var generateLabels: js.Function1[Chart[DS], js.Array[LegendItem]] = js.native
}

object LegendLabelSpec {
  def apply[DS <: Dataset[_]](
      boxWidth: Option[Int] = None,
      boxHeight: Option[Int] = None,
      color: Option[Color] = None,
      font: Option[ChFont] = None,
      textAlign: Option[String] = None,
      generateLabels: Option[Chart[DS] => js.Array[LegendItem]] = None
  ): LegendLabelSpec[DS] =
    cleanObject(Map(
      "boxWidth" -> boxWidth.orUndefined,
      "boxHeight" -> boxHeight.orUndefined,
      "color" -> color.orUndefined,
      "font" -> font.orUndefined,
      "textAlign" -> textAlign.orUndefined,
      "generateLabels" -> generateLabels.map(js.Any.fromFunction1(_)).orUndefined
    )).asInstanceOf[LegendLabelSpec[DS]]
}


@js.native
trait LegendTitleSpec extends js.Object {
  @nowarn var text: String = js.native
  @nowarn var color: Color = js.native
  @nowarn var display: Boolean = js.native
  @nowarn var font: ChFont = js.native
  @nowarn var padding: Padding = js.native
}

object Legend {

}
