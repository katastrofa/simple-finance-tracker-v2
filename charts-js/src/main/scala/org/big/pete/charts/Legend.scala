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
trait LegendSpec extends js.Object {
  @nowarn var display: js.UndefOr[Boolean] = js.native
  @nowarn var position: js.UndefOr[String] = js.native
  @nowarn var align: js.UndefOr[String] = js.native
  @nowarn var maxHeight: js.UndefOr[Int] = js.native
  @nowarn var maxWidth: js.UndefOr[Int] = js.native
  @nowarn var fullSize: js.UndefOr[Boolean] = js.native
  @nowarn var labels: js.UndefOr[LegendLabelSpec] = js.native
  @nowarn var title: js.UndefOr[LegendTitleSpec] = js.native
}

object LegendSpec {
  def apply(
      display: Option[Boolean] = None,
      position: Option[String] = None,
      align: Option[String] = None,
      maxHeight: Option[Int] = None,
      maxWidth: Option[Int] = None,
      fullSize: Option[Boolean] = None,
      labels: Option[LegendLabelSpec] = None,
      title: Option[LegendTitleSpec] = None
  ): LegendSpec =
    cleanObject(Map(
      "display" -> display.orUndefined,
      "position" -> position.orUndefined,
      "align" -> align.orUndefined,
      "maxHeight" -> maxHeight.orUndefined,
      "maxWidth" -> maxWidth.orUndefined,
      "fullSize" -> fullSize.orUndefined,
      "labels" -> labels.orUndefined,
      "title" -> title.orUndefined
    )).asInstanceOf[LegendSpec]
}


@js.native
trait LegendLabelSpec extends js.Object {
  @nowarn var boxWidth: js.UndefOr[Int] = js.native
  @nowarn var boxHeight: js.UndefOr[Int] = js.native
  @nowarn var color: js.UndefOr[Color] = js.native
  @nowarn var font: js.UndefOr[ChFont] = js.native
  @nowarn var textAlign: js.UndefOr[String] = js.native
  @nowarn var generateLabels: js.UndefOr[js.Function1[FullChart, js.Array[LegendItem]]] = js.native
}

object LegendLabelSpec {
  def apply(
      boxWidth: Option[Int] = None,
      boxHeight: Option[Int] = None,
      color: Option[Color] = None,
      font: Option[ChFont] = None,
      textAlign: Option[String] = None,
      generateLabels: Option[FullChart => js.Array[LegendItem]] = None
  ): LegendLabelSpec =
    cleanObject(Map(
      "boxWidth" -> boxWidth.orUndefined,
      "boxHeight" -> boxHeight.orUndefined,
      "color" -> color.orUndefined,
      "font" -> font.orUndefined,
      "textAlign" -> textAlign.orUndefined,
      "generateLabels" -> generateLabels.map(js.Any.fromFunction1(_)).orUndefined
    )).asInstanceOf[LegendLabelSpec]
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
