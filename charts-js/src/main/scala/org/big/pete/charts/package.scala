package org.big.pete

import org.scalajs.dom.{CanvasGradient, CanvasPattern}

import scala.scalajs.js
import scala.scalajs.js.special.objectLiteral
import scala.scalajs.js.|


package object charts {
  type ChJsNumber = Int | Float | Long | Double
  type BackgroundColor = Color | js.Array[Color]
  type Color = String | CanvasGradient | CanvasPattern
  type Padding = Int | LTRB

  implicit class ToSome[A](value: A) {
    def some: Option[A] = Some(value)
  }

  implicit def strToColor(str: String): Color =
    str.asInstanceOf[Color]

  implicit class StringToolz(value: String) {
    def color: Color = value.asInstanceOf[Color]
  }

  def cleanObject(attr: Map[String, scala.Any]): js.Object = {
    val cleanAttr = attr.filter { case (_, value) => !js.isUndefined(value) }
    objectLiteral(cleanAttr.toSeq: _*)
  }

  def labelsWithValues(chart: FullChart): js.Array[LegendItem] = {
    val labels = chart.data.labels.getOrElse(new js.Array[String]()).toList
    chart.data.datasets.zipWithIndex.flatMap { case (dataset, indexDataset) =>
      dataset.data.zipWithIndex.map { case (item, index) =>
        val text = (item: Any) match {
          case complex: ComplexData =>
            List(
              complex.labelCreator.flatMap(fn => fn(complex.y)).toOption,
              complex.label.flatMap(label => s"$label${complex.y}").toOption,
              if (labels.length > index) Some(s"${labels(index)}${complex.y}") else None
            ).flatten.head
          case number =>
            if (labels.length > index) s"${labels(index)}$number" else s"$number"
        }

        val color = List(
          chart.options.plugins.flatMap(_.legend)
            .flatMap(_.labels)
            .flatMap(_.color)
            .toOption,
          Chart.defaults.plugins.flatMap(_.legend)
            .flatMap(_.labels)
            .flatMap(_.color)
            .toOption,
          "black".asInstanceOf[Color].some
        ).flatten.head

        val backgroundColor: Option[Color] = {
          if (dataset.hasOwnProperty("backgroundColor"))
            dataset.backgroundColor.toOption.flatMap { bgColor =>
              (bgColor: Any) match {
                case arr if js.Array.isArray(arr) && arr.asInstanceOf[js.Array[Color]].length > index =>
                  arr.asInstanceOf[js.Array[Color]](index).some
                case clr if !js.Array.isArray(clr) =>
                  clr.asInstanceOf[Color].some
                case _ =>
                  None
              }
            }
          else
            None
        }

        LegendItem(text, datasetIndex = indexDataset.some, index = index.some, fontColor = color.some, fillStyle = backgroundColor)
      }
    }
  }
}
