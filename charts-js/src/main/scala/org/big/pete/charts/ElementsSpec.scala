package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.JSConverters._


@js.native
trait ElementsSpec extends js.Object {
  @nowarn var arc: js.UndefOr[ArcSpec] = js.native
}

object ElementsSpec {
  def apply(arc: Option[ArcSpec] = None): ElementsSpec =
    cleanObject(Map("arc" -> arc.orUndefined)).asInstanceOf[ElementsSpec]
}

@js.native
trait ArcSpec extends js.Object {
  @nowarn var borderColor: js.UndefOr[Color] = js.native
  @nowarn var borderWidth: js.UndefOr[Int] = js.native
  @nowarn var circular: js.UndefOr[Boolean] = js.native
}

object ArcSpec {
  def apply(
      borderColor: Option[Color] = None,
      borderWidth: Option[Int] = None,
      circular: Option[Boolean] = None
  ): ArcSpec =
    cleanObject(Map(
      "borderColor" -> borderColor.orUndefined,
      "borderWidth" -> borderWidth.orUndefined,
      "circular" -> circular.orUndefined
    )).asInstanceOf[ArcSpec]
}
