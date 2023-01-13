package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.|


@js.native
trait ChartData[DS <: Dataset[_]] extends js.Object {
  @nowarn var datasets: js.Array[DS] = js.native
  @nowarn var labels: js.UndefOr[js.Array[String]] = js.native
}

object ChartData {
  def apply[DS <: Dataset[_]](datasets: Array[DS], labels: Option[Array[String]] = None): ChartData[DS] =
    js.Dynamic.literal(
      datasets = datasets.toJSArray,
      labels = labels.map(_.toJSArray).orUndefined
    ).asInstanceOf[ChartData[DS]]
}

@js.native
trait Dataset[D] extends js.Object {
  @nowarn var data: js.Array[D] = js.native
  @nowarn var label: js.UndefOr[String] = js.native
  @nowarn var clip: js.UndefOr[Boolean | LTRB] = js.native
  @nowarn var order: js.UndefOr[Int] = js.native
  @nowarn var stack: js.UndefOr[String] = js.native
  @nowarn var parsing: js.UndefOr[Boolean | ChParsing] = js.native
  @nowarn var hidden: js.UndefOr[Boolean] = js.native
}

@js.native
trait PieDatasetExtra extends js.Object {
  @nowarn var backgroundColor: js.UndefOr[BackgroundColor] = js.native
  @nowarn var borderColor: js.UndefOr[Color] = js.native
  @nowarn var offset: js.UndefOr[Int] = js.native
  @nowarn var spacing: js.UndefOr[Int] = js.native
}

@js.native
trait PieDataset extends Dataset[ChJsNumber] with PieDatasetExtra


object Dataset {
  def apply[D](
      data: Array[D],
      label: Option[String] = None,
      clip: Option[Boolean | LTRB] = None,
      order: Option[Int] = None,
      stack: Option[String] = None,
      parsing: Option[Boolean | ChParsing] = None,
      hidden: Option[Boolean] = None
  ): Dataset[D] =
    cleanObject(Map(
      "data" -> data.toJSArray, "label" -> label.orUndefined, "clip" -> clip.orUndefined, "order" -> order.orUndefined,
      "stack" -> stack.orUndefined, "parsing" -> parsing.orUndefined, "hidden" -> hidden.orUndefined
    )).asInstanceOf[Dataset[D]]

  def pie(
      data: Array[ChJsNumber],
      label: Option[String] = None,
      clip: Option[Boolean | LTRB] = None,
      order: Option[Int] = None,
      stack: Option[String] = None,
      parsing: Option[Boolean | ChParsing] = None,
      hidden: Option[Boolean] = None,
      backgroundColor: Option[BackgroundColor] = None,
      borderColor: Option[Color] = None,
      offset: Option[Int] = None,
      spacing: Option[Int] = None
  ): PieDataset =
    cleanObject(Map(
      "data" -> data.toJSArray, "label" -> label.orUndefined, "clip" -> clip.orUndefined, "order" -> order.orUndefined,
      "stack" -> stack.orUndefined, "parsing" -> parsing.orUndefined, "hidden" -> hidden.orUndefined,
      "backgroundColor" -> backgroundColor.orUndefined, "borderColor" -> borderColor.orUndefined,
      "offset" -> offset.orUndefined, "spacing" -> spacing.orUndefined
    )).asInstanceOf[PieDataset]
}

@JSExportTopLevel("ComplexData")
class ComplexData(
    val x: String | ChJsNumber,
    val y: String | ChJsNumber,
    val other: js.UndefOr[String | ChJsNumber] = js.undefined,
    val label: js.UndefOr[String] = js.undefined,
    val labelCreator: js.UndefOr[js.Function1[String | ChJsNumber, String]] = js.undefined
) {}

