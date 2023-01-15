package org.big.pete.sft.front.helpers

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, Reusability, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._
import org.big.pete.charts
import org.big.pete.charts.{BackgroundColor, ChConfig, ChJsNumber, ChartData, ChartOptions, Dataset, OptionsPlugins, PieChartOptions, PieDataset}
import org.big.pete.sft.front.utilz.{LegendOptions, RxChart, RxChartImplicits, TitleOptions}
import org.scalajs.dom.{console, html}
import org.scalajs.dom.html.Canvas

import scala.scalajs.js.JSConverters._
import scala.scalajs.js.JSON


object PieChart {
  case class PieChartData(value: Float, label: String, bgColor: String)
  case class Props(
      data: List[PieChartData],
      borderColor: Option[String] = None,
      hoverLabel: Option[String] = None,
      legend: Option[LegendOptions] = None,
      title: Option[TitleOptions] = None,
      cutout: Option[String] = None,
      useHalf: Boolean = false,
      aspectRatio: Option[Double] = None,
      classes: Set[String] = Set.empty
  )

  import RxChartImplicits._
  import org.big.pete.react.Implicits._

  implicit val pieChartDataReuse: Reusability[PieChartData] = Reusability.derive[PieChartData]
  implicit val propsReuse: Reusability[Props] = Reusability.derive[Props]


  class Backend(override val $: BackendScope[Props, Unit]) extends RxChart[Props, Unit, ChartData[PieDataset], PieChartOptions, charts.PieChart] {
    override def createChart(canvas: Canvas, props: Props): charts.PieChart = {
      import org.big.pete.charts.ToSome

      val chartData = ChartData[PieDataset](
        datasets = Array(Dataset.pie(
          data = props.data.map(_.value.asInstanceOf[ChJsNumber]).toArray,
          backgroundColor = props.data.map(_.bgColor).toJSArray.asInstanceOf[BackgroundColor].some,
          borderColor = props.borderColor,
          label = props.hoverLabel
        )),
        labels = props.data.map(_.label).toArray.some
      )

      val chartOptions = ChartOptions.pie(
        plugins = OptionsPlugins(
          title = props.title.map(generateTitle),
          legend = props.legend.map(generateLegend),
        ).some,
        cutout = props.cutout,
        rotation = if (props.useHalf) Some(-90) else None,
        circumference = if (props.useHalf) Some(180) else None,
        aspectRatio = props.aspectRatio
      ).some

      console.log("Creating chart")
      console.log(JSON.parse(JSON.stringify(chartData)))
      console.log(JSON.parse(JSON.stringify(chartOptions.get)))

      new charts.PieChart(canvas, ChConfig("pie", chartData, chartOptions))
    }

    override def updateOptions(chart: charts.PieChart): Callback = $.props.map { props =>
      chart.data.labels = props.data.map(_.label).toJSArray
      chart.data.datasets.head.data = props.data.map(_.value.asInstanceOf[ChJsNumber]).toJSArray
      chart.data.datasets.head.backgroundColor = props.data.map(_.bgColor).toJSArray.asInstanceOf[BackgroundColor]
      chart.data.datasets.head.borderColor = props.borderColor.orUndefined
      chart.data.datasets.head.label = props.hoverLabel.orUndefined

      props.cutout.foreach(x => chart.options.cutout = x)
      if (props.useHalf) {
        chart.options.rotation = -90
        chart.options.circumference = 180
      } else {
        chart.options.rotation = 0
        chart.options.circumference = 360
      }
      props.aspectRatio.foreach(x => chart.options.aspectRatio = x)

      props.title.foreach(conf => updateTitle(chart, conf))
      props.legend.foreach(conf => updateLegend(chart, conf))
    }

    def render(props: Props): VdomTagOf[html.Div] = {
      renderDiv(props.classes)
    }
  }

  val component: Component[Props, Unit, Backend, CtorType.Props] = ScalaComponent.builder[Props]
    .stateless
    .renderBackend[Backend]
    .componentDidMount(_.backend.mount)
    .componentWillUnmount(_.backend.beforeUnmount)
    .componentDidUpdate(_.backend.onUpdate)
    .configure(Reusability.shouldComponentUpdate)
    .build
}
