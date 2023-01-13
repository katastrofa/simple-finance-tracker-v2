package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


@js.native
trait ChartOptions[DS <: Dataset[_]] extends js.Object {
  @nowarn var plugins: js.UndefOr[OptionsPlugins[DS]] = js.native
  @nowarn var scales: js.UndefOr[ScalesSpec] = js.native
  @nowarn var aspectRatio: ChJsNumber = js.native
  @nowarn var elements: js.UndefOr[ElementsSpec] = js.native
}

@js.native
trait PieChartExtraOptions extends js.Object {
  @nowarn var cutout: Int | String = js.native
  @nowarn var radius: Int | String = js.native
  @nowarn var rotation: ChJsNumber = js.native
  @nowarn var circumference: Int = js.native
}

@js.native
trait PieChartOptions extends ChartOptions[PieDataset] with PieChartExtraOptions

object ChartOptions {
  def apply[DS <: Dataset[_]](
      plugins: Option[OptionsPlugins[DS]] = None,
      scales: Option[ScalesSpec] = None,
      aspectRatio: Option[ChJsNumber] = None,
      elements: Option[ElementsSpec] = None
  ): ChartOptions[DS] =
    cleanObject(Map(
      "plugins" -> plugins.orUndefined,
      "scales" -> scales.orUndefined,
      "aspectRatio" -> aspectRatio.orUndefined,
      "elements" -> elements.orUndefined
    )).asInstanceOf[ChartOptions[DS]]

  def pie(
      plugins: Option[OptionsPlugins[PieDataset]] = None,
      scales: Option[ScalesSpec] = None,
      aspectRatio: Option[ChJsNumber] = None,
      elements: Option[ElementsSpec] = None,
      cutout: Option[Int | String] = None,
      radius: Option[Int | String] = None,
      rotation: Option[ChJsNumber] = None,
      circumference: Option[Int] = None,
  ): PieChartOptions =
    cleanObject(Map(
      "plugins" -> plugins.orUndefined,
      "scales" -> scales.orUndefined,
      "aspectRatio" -> aspectRatio.orUndefined,
      "elements" -> elements.orUndefined,
      "cutout" -> cutout.orUndefined,
      "radius" -> radius.orUndefined,
      "rotation" -> rotation.orUndefined,
      "circumference" -> circumference.orUndefined
    )).asInstanceOf[PieChartOptions]
}

@js.native
trait OptionsPlugins[DS <: Dataset[_]] extends js.Object {
  @nowarn var legend: js.UndefOr[LegendSpec[DS]] = js.native
  @nowarn var title: js.UndefOr[TitleSpec] = js.native
}

object OptionsPlugins {
  def apply[DS <: Dataset[_]](
      legend: Option[LegendSpec[DS]] = None,
      title: Option[TitleSpec] = None
  ): OptionsPlugins[DS] =
    cleanObject(Map(
      "legend" -> legend.orUndefined,
      "title" -> title.orUndefined
    )).asInstanceOf[OptionsPlugins[DS]]
}
