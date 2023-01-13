package org.big.pete.charts

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.JSConverters._
import org.scalajs.dom.Element


//@nowarn
//@js.native
//@JSImport("chart.js/auto", JSImport.Default)
//class Chart[DS <: Dataset[_]](element: Element, settings: js.Object) extends js.Object {
//  @nowarn var data: ChartData[DS] = js.native
//  @nowarn var options: ChartOptions[DS] = js.native
//
//  def destroy(): Unit = js.native
//  def clear(): Chart[DS] = js.native
//  def stop(): Chart[DS] = js.native
//  @nowarn def resize(width: js.UndefOr[Int], height: js.UndefOr[Int]): Unit = js.native
//  def reset(): Unit = js.native
//  @nowarn def update(mode: js.UndefOr[String]): Unit = js.native
//  def render(): Unit = js.native
//
//  @nowarn def hide(datasetIndex: Int, dataIndex: js.UndefOr[Int]): Unit = js.native
//  @nowarn def show(datasetIndex: Int, dataIndex: js.UndefOr[Int]): Unit = js.native
//
//  def draw(): Unit = js.native
//}

@nowarn
@js.native
@JSImport("chart.js/auto", JSImport.Default)
class Chart[CD <: ChartData[_], CO <: ChartOptions[_]](element: Element, settings: js.Object) extends js.Object {
  @nowarn var data: CD = js.native
  @nowarn var options: CO = js.native

  def destroy(): Unit = js.native
  def clear(): Chart[CD, CO] = js.native
  def stop(): Chart[CD, CO] = js.native
  @nowarn def resize(width: js.UndefOr[Int], height: js.UndefOr[Int]): Unit = js.native
  def reset(): Unit = js.native
  @nowarn def update(mode: js.UndefOr[String]): Unit = js.native
  def render(): Unit = js.native

  @nowarn def hide(datasetIndex: Int, dataIndex: js.UndefOr[Int]): Unit = js.native
  @nowarn def show(datasetIndex: Int, dataIndex: js.UndefOr[Int]): Unit = js.native

  def draw(): Unit = js.native
}

@js.native
@JSImport("chart.js/auto", JSImport.Default)
object Chart extends js.Object {
  @nowarn var defaults: ChartOptions[PieDataset] = js.native

  @nowarn def register(items: js.Object*): Unit = js.native
  @nowarn def unregister(items: js.Object*): Unit = js.native
  def aspectRatio(): js.UndefOr[Float] = js.native
}

//trait MyOptions extends ChartOptions with ElementChartOptions with PluginChartOptions

//@js.native
//trait ChConfig[DS <: Dataset[_]] extends js.Object {
//  @nowarn var `type`: String = js.native
//  @nowarn var data: ChartData[DS] = js.native
//  @nowarn var options: js.UndefOr[ChartOptions[DS]] = js.native
//  @nowarn var plugins: js.UndefOr[js.Object] = js.native
//}

@js.native
trait ChConfig[CD <: ChartData[_], CO <: ChartOptions[_]] extends js.Object {
  @nowarn var `type`: String = js.native
  @nowarn var data: CD = js.native
  @nowarn var options: js.UndefOr[CO] = js.native
  @nowarn var plugins: js.UndefOr[js.Object] = js.native
}

object ChConfig {
  def apply[DS <: Dataset[_]](
      `type`: String,
      data: ChartData[DS],
      options: Option[ChartOptions[DS]] = None,
      plugins: Option[js.Object] = None
  ): ChConfig[DS] =
    js.Dynamic.literal(
      `type` = `type`,
      data = data,
      options = options.orUndefined,
      plugins = plugins.orUndefined
    ).asInstanceOf[ChConfig[DS]]
}


//@js.native
//@JSImport("chart.js/auto", JSImport.Default)
//class Config(config: ChConfig) extends ChConfig



//@js.native
//trait ParsingOptions extends js.Object {
//  var parsing: js.Object | Boolean = js.native
//  var normalized: Boolean = js.native
//}
//
//@js.native
//trait ChartArea extends js.Object {
//  var top: Int = js.native
//  var left: Int = js.native
//  var right: Int = js.native
//  var bottom: Int = js.native
//  var width: Int = js.native
//  var height: Int = js.native
//}
//
//@js.native
//trait TRBL extends js.Object {
//  var top: Int = js.native
//  var right: Int = js.native
//  var bottom: Int = js.native
//  var left: Int = js.native
//}
//
//@js.native
//trait Point extends js.Object {
//  var x: Int = js.native
//  var y: Int = js.native
//}
//
//@js.native
//trait ControllerDatasetOption extends ParsingOptions {
//  var indexAxis: String = js.native
//  var clip: Int | ChartArea | Boolean = js.native
//  var label: String = js.native
//  var order: Int = js.native
//  var stack: String = js.native
//  var hidden: Boolean = js.native
//}
//
//trait SimpleData extends js.Object {
//  var data: Array[Int]
//}
//
//trait TestDataset extends ControllerDatasetOption with SimpleData
//
//trait ChartData extends js.Object {
//  var labels: js.UndefOr[Array[String]] = js.native
//  var datasets: Array[TestDataset] = js.native
//}
//
//
//
//@js.native
//trait AnimationSpec extends js.Object {
//  var duration: js.UndefOr[Int] = js.native
//  var easing: js.UndefOr[String] = js.native
//  var delay: js.UndefOr[Int] = js.native
//  var loop: js.UndefOr[Boolean] = js.native
//}
//
//@js.native
//trait AnimationEvent extends js.Object {
//  var chart: Chart = js.native
//  var numSteps: Int = js.native
//  var initial: Boolean = js.native
//  var currentStep: Int = js.native
//}
//
//trait AnimationCallbacks extends js.Object {
//  var onProgress: js.UndefOr[(Chart, AnimationEvent) => Unit] = js.native
//  var onComplete: js.UndefOr[(Chart, AnimationEvent) => Unit] = js.native
//}
//
//trait AnimationsSpecOptions[T] extends AnimationSpec {
//  var properties: Array[String] = js.native
//  var `type`: String = js.native
//  var fn: (T, T, Int) => T
//  var from: T | Boolean
//  var to: T | Boolean
//}
//
//trait AnimationsSpec extends js.Object {
//  var x: js.UndefOr[AnimationsSpecOptions[Int]] = js.native
//  var y: js.UndefOr[AnimationsSpecOptions[Int]] = js.native
//  var borderWidth: js.UndefOr[AnimationsSpecOptions[Int]] = js.native
//  var radius: js.UndefOr[AnimationsSpecOptions[Int]] = js.native
//  var tension: js.UndefOr[AnimationsSpecOptions[Int]] = js.native
//
//  var color: js.UndefOr[AnimationsSpecOptions[String | CanvasGradient | CanvasPattern]] = js.native
//  var borderColor: js.UndefOr[AnimationsSpecOptions[String | CanvasGradient | CanvasPattern]] = js.native
//  var backgroundColor: js.UndefOr[AnimationsSpecOptions[String | CanvasGradient | CanvasPattern]] = js.native
//}
//
//@js.native
//trait TransitionSpec extends js.Object {
//  var animation: AnimationSpec = js.native
//  var animations: AnimationsSpec = js.native
//}
//
//trait TransitionsSpec extends js.Object {
//  var active: js.UndefOr[TransitionSpec] = js.native
//  var resize: js.UndefOr[TransitionSpec] = js.native
//  var show: js.UndefOr[TransitionSpec] = js.native
//  var hide: js.UndefOr[TransitionSpec] = js.native
//  var reset: js.UndefOr[TransitionSpec] = js.native
//}
//
//@js.native
//trait AnimationOptions extends js.Object {
//  var animation: Boolean | AnimationSpec with AnimationCallbacks = js.native
//  var animations: AnimationsSpec = js.native
//  var transitions: TransitionsSpec = js.native
//}
//
//@js.native
//trait FontSpec extends js.Object {
//  var family: String = js.native
//  var size: Int = js.native
//  var style: String = js.native
//  var weight: js.UndefOr[String] = js.native
//  var lineHeight: String | Int = js.native
//}
//
//@js.native
//trait CoreInteractionOptions extends js.Object {
//  var mode: String = js.native
//  var intersect: Boolean = js.native
//  var axis: String = js.native
//  var includeInvisible: Boolean = js.native
//}
//
//trait LayoutSpec extends js.Object {
//  var autoPadding: Boolean = js.native
//  var padding: TRBL | Int | Point = js.native
//}
//
//@js.native
//trait ChartOptions extends ParsingOptions with AnimationOptions {
//  var indexAxis: String = js.native
//  var clip: Int | ChartArea | Boolean = js.native
//  var color: String | CanvasGradient | CanvasPattern = js.native
//  var backgroundColor: String | CanvasGradient | CanvasPattern = js.native
//  var borderColor: String | CanvasGradient | CanvasPattern = js.native
//  var font: FontSpec = js.native
//  var responsive: Boolean = js.native
//  var maintainAspectRatio: Boolean = js.native
//  var resizeDelay: Int = js.native
//  var aspectRatio: Float = js.native
//  var locale: String = js.native
//  var devicePixelRatio: Float = js.native
//  var interaction: CoreInteractionOptions = js.native
//  var hover: CoreInteractionOptions = js.native
//  var events: Array[String] = js.native
//  var layout: LayoutSpec = js.native
//}
//
//@js.native
//trait CommonElementOptions extends js.Object {
//  var borderWidth: Int | TRBL = js.native
//  var borderColor: String | CanvasGradient | CanvasPattern = js.native
//  var backgroundColor: String | CanvasGradient | CanvasPattern = js.native
//}
//
//@js.native
//trait ArcBorderRadius extends js.Object {
//  var outerStart: Int = js.native
//  var outerEnd: Int = js.native
//  var innerStart: Int = js.native
//  var innerEnd: Int = js.native
//}
//
//@js.native
//trait ArcOptions extends CommonElementOptions {
//  var borderAlign: String = js.native
//  var borderJoinStyle: String = js.native
//  var borderRadius: Int | ArcBorderRadius = js.native
//  var offset: Int = js.native
//  var circular: Boolean = js.native
//  var spacing: Int = js.native
//}
//
//@js.native
//trait CommonHoverOptions extends js.Object {
//  var hoverBorderWidth: Int = js.native
//  var hoverBorderColor: String | CanvasGradient | CanvasPattern = js.native
//  var hoverBackgroundColor: String | CanvasGradient | CanvasPattern = js.native
//}
//
//@js.native
//trait ArcHoverOptions extends CommonHoverOptions {
//  var hoverOffset: Int = js.native
//}
//
//@js.native
//trait BarOptions extends CommonElementOptions {
//  var base: Int = js.native
//  var borderSkipped: String | Boolean = js.native
//  var borderRadius: Int | BorderRadius = js.native
//  var inflateAmount: Float | String = js.native
//}
//
//@js.native
//trait BarHoverOptions extends CommonHoverOptions {
//  var hoverBorderRadius: Int | BorderRadius
//}
//
//
//@js.native
//trait ComplexFillTarget extends js.Object {
//  var target: Int | String | Boolean = js.native
//  var above: String | CanvasGradient | CanvasPattern = js.native
//  var below: String | CanvasGradient | CanvasPattern = js.native
//}
//
//trait LineSegmentOptions extends js.Object {
//  var backgroundColor: String | CanvasGradient | CanvasPattern = js.native
//  var borderColor: String | CanvasGradient | CanvasPattern = js.native
//  var borderCapStyle: String = js.native
//  var borderDash: Array[Float] = js.native
//  var borderDashOffset: Float = js.native
//  var borderJoinStyle: String = js.native
//  var borderWidth: Int = js.native
//}
//
//@js.native
//trait LineOptions extends CommonElementOptions {
//  var borderCapStyle: String = js.native
//  var borderDash: Array[Float] = js.native
//  var borderDashOffset: Float = js.native
//  var borderJoinStyle: String = js.native
//  var capBezierPoints: Boolean = js.native
//  var cubicInterpolationMode: String = js.native
//  var tension: Float = js.native
//  var stepped: String | Boolean = js.native
//  var fill: Int | String | Boolean | ComplexFillTarget = js.native
//  var spanGaps: Boolean | Int = js.native
//  var segment: LineSegmentOptions = js.native
//}
//
//@js.native
//trait LineHoverOptions extends CommonHoverOptions {
//  var hoverBorderCapStyle: String = js.native
//  var hoverBorderDash: Array[Float] = js.native
//  var hoverBorderDashOffset: Float = js.native
//  var hoverBorderJoinStyle: String = js.native
//}
//
//@js.native
//trait PointOptions extends CommonElementOptions {
//  var radius: Int = js.native
//  var hitRadius: Int = js.native
//  var pointStyle: String | Boolean | HTMLImageElement | HTMLCanvasElement = js.native
//  var rotation: Int = js.native
//  var drawActiveElementsOnTop: Boolean = js.native
//}
//
//@js.native
//trait PointHoverOptions extends CommonHoverOptions {
//  var hoverRadius: Int = js.native
//}
//
//@js.native
//trait ElementOptionsByType extends js.Object {
//  var arc: ArcOptions with ArcHoverOptions = js.native
//  var bar: BarOptions with BarHoverOptions = js.native
//  var line: LineOptions with LineHoverOptions = js.native
//  var point: PointOptions with PointHoverOptions = js.native
//}
//
//@js.native
//trait ElementChartOptions extends js.Object {
//  var elements: ElementOptionsByType = js.native
//}
//
//
//
//@js.native
//trait BaseDecimationOptions extends js.Object {
//  var enabled: Boolean = js.native
//  var threshold: js.UndefOr[Float] = js.native
//  var algorithm: String = js.native
//}
//
//@js.native
//trait LttbDecimationOptions extends BaseDecimationOptions {
//  var samples: js.UndefOr[Int] = js.native
//}
//
//@js.native
//trait FillerOptions extends js.Object {
//  var drawTime: String = js.native
//  var propagate: Boolean = js.native
//}
//
//trait LabelsOptions extends js.Object {
//  var boxWidth: Int = js.native
//  var boxHeight: Int = js.native
//  var boxPadding: Int = js.native
//  var color: String | CanvasGradient | CanvasPattern = js.native
//  var font: FontSpec = js.native
//  var padding: Int = js.native
//
//  var generateLabels: Chart => Array[LegendItem] = js.native
//  var filter: (LegendItem, ChartData) => Boolean = js.native
//  var sort: (LegendItem, LegendItem, ChartData) => Int = js.native
//
//  var pointStyle: String | Boolean | HTMLImageElement | HTMLCanvasElement = js.native
//  var textAlign: js.UndefOr[String] = js.native
//  var usePointStyle: Boolean = js.native
//  var useBorderRadius: Boolean = js.native
//  var borderRadius: Int = js.native
//}
//
//trait LegendTitleOptions extends js.Object {
//  var display: Boolean = js.native
//  var color: String | CanvasGradient | CanvasPattern = js.native
//  var font: FontSpec = js.native
//  var position: String = js.native
//  var padding: js.UndefOr[Int | ChartArea] = js.native
//  var text: String = js.native
//}
//
//@js.native
//trait LegendOptions extends js.Object {
//  var display: Boolean = js.native
//  var position: String | js.Object = js.native
//  var align: String = js.native
//  var maxHeight: Int = js.native
//  var maxWidth: Int = js.native
//  var fullSize: Boolean = js.native
//  var reverse: Boolean = js.native
//
////  onClick(this: LegendElement<TType>, e: ChartEvent, legendItem: LegendItem, legend: LegendElement<TType>): void;
////  onHover(this: LegendElement<TType>, e: ChartEvent, legendItem: LegendItem, legend: LegendElement<TType>): void;
////  onLeave(this: LegendElement<TType>, e: ChartEvent, legendItem: LegendItem, legend: LegendElement<TType>): void;
//
//  var labels: LabelsOptions = js.native
//  var rtl: Boolean = js.native
//  var textDirection: String = js.native
//  var title: LegendTitleOptions = js.native
//}
//
//@js.native
//trait TitleOptions extends js.Object {
//  var align: String = js.native
//  var display: Boolean = js.native
//  var position: String = js.native
//  var color: String | CanvasGradient | CanvasPattern = js.native
//  var font: FontSpec = js.native
//  var fullSize: Boolean = js.native
//  var padding: Int | TRBL = js.native
//  var text: String | Array[String] = js.native
//}
//
//@js.native
//@JSImport("chart.js/auto", "Element")
//class ChElement extends js.Object {
//  var x: Int = js.native
//  var y: Int = js.native
//  var active: Boolean = js.native
//  var options: js.Object = js.native
//  var $animations: js.Object = js.native
//
//  def tooltipPosition(useFinalPosition: Boolean): Point = js.native
//  def hasValue(): Boolean = js.native
//  def getProps(props: Array[String], `final`: Option[Boolean]): js.Object = js.native
//}
//
//@js.native
//trait TooltipItem extends js.Object {
//  var chart: Chart = js.native
//  var label: String = js.native
//  var parsed: js.Object = js.native
//
//  var raw: js.Any = js.native
//  var formattedValue: String = js.native
//  var dataset: js.Object = js.native
//  var datasetIndex: Int = js.native
//  var dataIndex: Int = js.native
//  var element: ChElement = js.native
//}
//
//@js.native
//trait TooltipOptions extends CoreInteractionOptions {
//  var enabled: Boolean
////  def external(this: TooltipModel, args: {chart: Chart; tooltip: TooltipModel}): Unit
//
//  var position: String = js.native
//  var xAlign: String = js.native
//  var yAlign: String = js.native
//
//  var itemSort: (TooltipItem, TooltipItem, ChartData) => Int = js.native
//  var filter: (TooltipItem, Int, Array[TooltipItem], ChartData) => Boolean = js.native
//
//  var backgroundColor: String | CanvasGradient | CanvasPattern = js.native
//  var boxPadding: Int = js.native
//  var titleColor: String | CanvasGradient | CanvasPattern = js.native
//  var titleFont: FontSpec = js.native
//  var titleSpacing: Int = js.native
//  var titleMarginBottom: Int = js.native
//  var titleAlign: String = js.native
//  var bodySpacing: Int = js.native
//  var bodyColor: String | CanvasGradient | CanvasPattern = js.native
//  var bodyFont: FontSpec = js.native
//  var bodyAlign: String = js.native
//  var footerSpacing: Int = js.native
//  var footerMarginTop: Int = js.native
//  var footerColor: String | CanvasGradient | CanvasPattern = js.native
//  var footerFont: FontSpec = js.native
//  var footerAlign: String = js.native
//  var padding: Int | TRBL = js.native
//  var caretPadding: Int = js.native
//  var caretSize: Int = js.native
//  var cornerRadius: Int | BorderRadius = js.native
//  var multiKeyBackground: String | CanvasGradient | CanvasPattern = js.native
//  var displayColors: Boolean = js.native
//  var boxWidth: Int = js.native
//  var boxHeight: Int = js.native
//  var usePointStyle: Boolean = js.native
//  var borderColor: String | CanvasGradient | CanvasPattern = js.native
//  var borderWidth: Int = js.native
//  var rtl: Boolean = js.native
//  var textDirection: String = js.native
//
//  var animation: AnimationSpec | Boolean = js.native
//  var animations: AnimationsSpec | Boolean = js.native
//  var callbacks: js.Object = js.native //TooltipCallbacks
//}
//
//@js.native
//trait PluginOptionsByType extends js.Object {
//  var decimation: LttbDecimationOptions = js.native
//  var filler: FillerOptions = js.native
//  var legend: LegendOptions = js.native
//  var subtitle: TitleOptions = js.native
//  var title: TitleOptions = js.native
//  var tooltip: TooltipOptions = js.native
//}
//
//@js.native
//trait PluginChartOptions extends js.Object {
//  var plugins: PluginOptionsByType = js.native
//}
