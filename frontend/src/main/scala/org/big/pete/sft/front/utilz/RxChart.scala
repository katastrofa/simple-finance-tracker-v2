package org.big.pete.sft.front.utilz

import japgolly.scalajs.react.ScalaComponent.BackendScope
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.all.VdomTagOf
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, Ref, Reusability}
import org.big.pete.charts.{ChFont, Chart, ChartData, ChartOptions, Color, LegendLabelSpec, LegendSpec, OptionsPlugins, TitleSpec, labelsWithValues}
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.|


case class LegendOptions(
    display: Boolean = true,
    boxWidth: Option[Int] = None,
    useCustomLabels: Boolean = false,
    fontSize: Option[Int] = None
)
case class TitleOptions(
    display: Option[Boolean] = None,
    text: Option[String] = None,
    color: Option[String] = None,
    fontSize: Option[Int] = None
)

object RxChartImplicits {
  implicit val legendOptionsReuse: Reusability[LegendOptions] = Reusability.derive[LegendOptions]
  implicit val titleOptionsReuse: Reusability[TitleOptions] = Reusability.derive[TitleOptions]
}

trait RxChart[P, S, D <: ChartData[_], O <: ChartOptions, CH <: Chart[D, O]] extends OnUnmount {
  val $: BackendScope[P, S]

  private var chart: Option[CH] = None
  private val canvasRef: Ref.ToVdom[html.Canvas] = Ref.toVdom[html.Canvas]

  def createChart(canvas: html.Canvas, props: P): CH
  def updateOptions(chart: CH): Callback

  protected def generateTitle(options: TitleOptions): TitleSpec = {
    val font = options.fontSize.map(size => ChFont(size = Some(size)))
    TitleSpec(display = options.display, text = options.text, color = options.color, font = font)
  }

  private def generateLegendLabelSpec(options: LegendOptions): LegendLabelSpec =
    LegendLabelSpec(
      boxWidth = options.boxWidth,
      generateLabels = if (options.useCustomLabels) Some(labelsWithValues) else None,
      font = ChFont(size = options.fontSize).some
    )

  protected def generateLegend(options: LegendOptions): LegendSpec = {
    import org.big.pete.charts.ToSome

    LegendSpec(display = options.display.some, labels = generateLegendLabelSpec(options).some)
  }

  protected def updateTitle(chart: CH, conf: TitleOptions): Unit = {
    if (chart.options.plugins.isEmpty)
      chart.options.plugins = OptionsPlugins(title = generateTitle(conf).some)
    else if (chart.options.plugins.get.title.isEmpty)
      chart.options.plugins.get.title = generateTitle(conf).some.orUndefined
    else {
      val title = chart.options.plugins.get.title.get
      title.display = conf.display.getOrElse(false)
      title.text = conf.text.getOrElse("").asInstanceOf[String | js.Array[String]]
      title.color = conf.color.map(_.asInstanceOf[Color]).getOrElse(Chart.defaults.color)
      title.font.size = conf.fontSize.orUndefined
    }
  }

  protected def updateLegend(chart: CH, conf: LegendOptions): Unit = {
    if (chart.options.plugins.isEmpty)
      chart.options.plugins = OptionsPlugins(legend = generateLegend(conf).some)
    else if (chart.options.plugins.get.legend.isEmpty)
      chart.options.plugins.get.legend = generateLegend(conf)
    else {
      val legend = chart.options.plugins.get.legend.get
      legend.display = conf.display
      if (legend.labels.isEmpty)
        legend.labels = generateLegendLabelSpec(conf)
      else {
        legend.labels.get.boxWidth = conf.boxWidth.orUndefined
        legend.labels.get.generateLabels = if (conf.useCustomLabels) js.Any.fromFunction1(labelsWithValues) else js.undefined
        if (legend.labels.get.font.isEmpty)
          legend.labels.get.font = ChFont(size = conf.fontSize)
        else
          legend.labels.get.font.get.size = conf.fontSize.orUndefined
      }
    }
  }

  def mount: Callback = $.props.flatMap { props =>
    canvasRef.foreach { canvas =>
      chart = Some(createChart(canvas, props))
    }
  }

  def beforeUnmount: Callback = Callback {
    chart.foreach(_.destroy())
    chart = None
  }

  def onUpdate: Callback = {
    chart.map { chartObj =>
      updateOptions(chartObj) >> Callback(chartObj.update(js.undefined))
    }.getOrElse(Callback.empty)
  }

  def renderDiv(classes: Set[String]): VdomTagOf[html.Div] = {
    val classesSeq = classes.map(_ -> true).toSeq
    <.div(^.classSet(classesSeq: _*),
      <.canvas().withRef(canvasRef)
    )
  }
}
