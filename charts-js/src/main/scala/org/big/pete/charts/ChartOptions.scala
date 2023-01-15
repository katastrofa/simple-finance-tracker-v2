package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


@js.native
trait ChartOptions extends js.Object {
  @nowarn var plugins: js.UndefOr[OptionsPlugins] = js.native
  @nowarn var scales: js.UndefOr[ScalesSpec] = js.native
  @nowarn var maintainAspectRatio: js.UndefOr[Boolean] = js.native
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
trait GlobalExtraOptions extends js.Object {
  @nowarn var color: Color = js.native
}

@js.native
trait PieChartOptions extends ChartOptions with PieChartExtraOptions

object ChartOptions {
  def apply(
      plugins: Option[OptionsPlugins] = None,
      scales: Option[ScalesSpec] = None,
      maintainAspectRatio: Option[Boolean] = None,
      aspectRatio: Option[ChJsNumber] = None,
      elements: Option[ElementsSpec] = None
  ): ChartOptions =
    cleanObject(Map(
      "plugins" -> plugins.orUndefined,
      "scales" -> scales.orUndefined,
      "maintainAspectRatio" -> maintainAspectRatio.orUndefined,
      "aspectRatio" -> aspectRatio.orUndefined,
      "elements" -> elements.orUndefined
    )).asInstanceOf[ChartOptions]

  def pie(
      plugins: Option[OptionsPlugins] = None,
      scales: Option[ScalesSpec] = None,
      maintainAspectRatio: Option[Boolean] = None,
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
      "maintainAspectRatio" -> maintainAspectRatio.orUndefined,
      "aspectRatio" -> aspectRatio.orUndefined,
      "elements" -> elements.orUndefined,
      "cutout" -> cutout.orUndefined,
      "radius" -> radius.orUndefined,
      "rotation" -> rotation.orUndefined,
      "circumference" -> circumference.orUndefined
    )).asInstanceOf[PieChartOptions]
}

@js.native
trait OptionsPlugins extends js.Object {
  @nowarn var legend: js.UndefOr[LegendSpec] = js.native
  @nowarn var title: js.UndefOr[TitleSpec] = js.native
}

object OptionsPlugins {
  def apply(
      legend: Option[LegendSpec] = None,
      title: Option[TitleSpec] = None
  ): OptionsPlugins =
    cleanObject(Map(
      "legend" -> legend.orUndefined,
      "title" -> title.orUndefined
    )).asInstanceOf[OptionsPlugins]
}
