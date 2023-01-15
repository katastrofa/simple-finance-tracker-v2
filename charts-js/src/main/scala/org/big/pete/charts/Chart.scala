package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.JSConverters._
import org.scalajs.dom.Element

import scala.scalajs.js.|


@nowarn
@js.native
@JSImport("chart.js/auto", JSImport.Default)
class Chart[D <: ChartData[_], O <: ChartOptions](element: Element, settings: js.Object) extends js.Object {
  @nowarn var data: D = js.native
  @nowarn var options: O = js.native

  def destroy(): Unit = js.native
  def clear(): Chart[D, O] = js.native
  def stop(): Chart[D, O] = js.native
  @nowarn def resize(width: js.UndefOr[Int], height: js.UndefOr[Int]): Unit = js.native
  def reset(): Unit = js.native
  @nowarn def update(mode: js.UndefOr[String]): Unit = js.native
  def render(): Unit = js.native

  @nowarn def hide(datasetIndex: Int, dataIndex: js.UndefOr[Int]): Unit = js.native
  @nowarn def show(datasetIndex: Int, dataIndex: js.UndefOr[Int]): Unit = js.native

  def draw(): Unit = js.native
}

class PieChart(element: Element, settings: js.Object)
  extends Chart[ChartData[PieDataset], PieChartOptions](element, settings)

class FullChart(element: Element, settings: js.Object)
  extends Chart[ChartData[Dataset[ChJsNumber | ComplexData] with PieDatasetExtra], ChartOptions with PieChartExtraOptions](element, settings)

@js.native
@JSImport("chart.js/auto", JSImport.Default)
object Chart extends js.Object {
  @nowarn var defaults: ChartOptions with PieChartExtraOptions with GlobalExtraOptions = js.native

  @nowarn def register(items: js.Object*): Unit = js.native
  @nowarn def unregister(items: js.Object*): Unit = js.native
  def aspectRatio(): js.UndefOr[Float] = js.native
}

@js.native
trait ChConfig[D <: ChartData[_], O <: ChartOptions] extends js.Object {
  @nowarn var `type`: String = js.native
  @nowarn var data: D = js.native
  @nowarn var options: js.UndefOr[O] = js.native
  @nowarn var plugins: js.UndefOr[js.Object] = js.native
}

object ChConfig {
  def apply[D <: ChartData[_], O <: ChartOptions](
      `type`: String,
      data: D,
      options: Option[O] = None,
      plugins: Option[js.Object] = None
  ): ChConfig[D, O] =
    js.Dynamic.literal(
      `type` = `type`,
      data = data,
      options = options.orUndefined,
      plugins = plugins.orUndefined
    ).asInstanceOf[ChConfig[D, O]]
}
