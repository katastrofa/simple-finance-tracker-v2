package org.big.pete.charts

import org.scalajs.dom.{console, document}

//import scala.scalajs.js
import scala.scalajs.js.JSConverters._
//import scala.scalajs.js.|


object TestMain {

  def main(args: Array[String]): Unit = {

    Chart.defaults.plugins.foreach(_.legend.foreach(_.labels.foreach(_.color = "white")))

    val myOptions = ChartOptions(
      scales = ScalesSpec(
        y = LinearAxisOptions(
          beginAtZero = true.some
        ).some
      ).some
    ).some

    new Chart(document.getElementById("fun-canvas"), ChConfig(
      "bar",
      ChartData(
        Array(Dataset(
          Array(12, 19, 3, 5),
          Some("# of Votes")
        )),
        Some(Array("Red", "Blue", "Yellow", "Green"))
      ),
      myOptions
    ))

    val otherOptions = ChartOptions.pie(
      plugins = OptionsPlugins(
        legend = LegendSpec(
          display = true.some,
          labels = LegendLabelSpec(
            boxWidth = 10.some,
            generateLabels = Some(labelsWithValues)
          ).some
        ).some,
        title = TitleSpec(
          display = true.some,
          text = "Finances Balance".some,
          color = "white".some,
          font = ChFont(
            size = 16.some
          ).some
        ).some
      ).some,
      cutout = "20%".some,
      circumference = 180.some,
      rotation = (-90).some
    ).some

    val pieChart = new PieChart(document.getElementById("pie-can"), ChConfig(
      "pie",
      ChartData(
        datasets = Array(Dataset.pie(
          data = Array(439.22, 10.42, 4.12, 3542.30),
          label = "Amount".some,
          backgroundColor = Some(Array("#ff0000".color, "#ee0000".color, "#dd0000".color, "green".color).toJSArray),
          borderColor = "#777777".some
        )),
        labels = Array("Expense - €", "Expense - $", "Expense - £", "Income - €").some
      ),
      otherOptions
//      Some(literal(
//        plugins = literal(
//          title = literal(
//            display = true,
//            text = "Finances Balance",
//            color = "black"
//          )
//        )
//      ))
    ))

    console.log(pieChart)
    console.log(pieChart.options.scales)

    ()
  }
}
