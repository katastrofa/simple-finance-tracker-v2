package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


@js.native
trait ScalesSpec extends js.Object {
  @nowarn var x: js.UndefOr[LinearAxisOptions] = js.native
  @nowarn var x2: js.UndefOr[LinearAxisOptions] = js.native
  @nowarn var y: js.UndefOr[LinearAxisOptions] = js.native
  @nowarn var y2: js.UndefOr[LinearAxisOptions] = js.native
}

object ScalesSpec {
  def apply(
      x: Option[LinearAxisOptions] = None,
      x2: Option[LinearAxisOptions] = None,
      y: Option[LinearAxisOptions] = None,
      y2: Option[LinearAxisOptions] = None
  ): ScalesSpec =
    cleanObject(Map(
      "x" -> x.orUndefined,
      "x2" -> x2.orUndefined,
      "y" -> y.orUndefined,
      "y2" -> y2.orUndefined
    )).asInstanceOf[ScalesSpec]
}

@js.native
trait CommonScaleOptions extends js.Object {
  @nowarn var `type`: String = js.native
  @nowarn var backgroundColor: Color = js.native
  @nowarn var stacked: Boolean | String = js.native
  @nowarn var min: ChJsNumber = js.native
  @nowarn var max: ChJsNumber = js.native
  @nowarn var suggestedMin: ChJsNumber = js.native
  @nowarn var suggestedMax: ChJsNumber = js.native
}

@js.native
trait LinearAxisOptions extends CommonScaleOptions {
  @nowarn var beginAtZero: Boolean = js.native
  @nowarn var grace: String | ChJsNumber = js.native
}

object LinearAxisOptions {
  def apply(
      `type`: Option[String] = None,
      backgroundColor: Option[Color] = None,
      stacked: Option[Boolean] = None,
      min: Option[ChJsNumber] = None,
      max: Option[ChJsNumber] = None,
      suggestedMin: Option[ChJsNumber] = None,
      suggestedMax: Option[ChJsNumber] = None,
      beginAtZero: Option[Boolean] = None,
      grace: Option[String | ChJsNumber] = None
  ): LinearAxisOptions =
    cleanObject(Map(
      "type" -> `type`.orUndefined,
      "backgroundColor" -> backgroundColor.orUndefined,
      "stacked" -> stacked.orUndefined,
      "min" -> min.orUndefined,
      "max" -> max.orUndefined,
      "suggestedMin" -> suggestedMin.orUndefined,
      "suggestedMax" -> suggestedMax.orUndefined,
      "beginAtZero" -> beginAtZero.orUndefined,
      "grace" -> grace.orUndefined
    )).asInstanceOf[LinearAxisOptions]
}


object Scales {

}
