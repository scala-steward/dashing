package dashing.client.components

import japgolly.scalajs.react.{Callback, ScalaComponent}
import japgolly.scalajs.react.vdom.html_<^._

import org.scalajs.dom.raw.HTMLCanvasElement
import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.scalajs.js.annotation.JSGlobal

@js.native
trait ChartDataset extends js.Object {
  def label: String = js.native
  def data: js.Array[Double] = js.native
  def fillColor: String = js.native
  def strokeColor: String = js.native
}

object ChartDataset {
  def apply(
    data: Seq[Double],
    label: String,
    borderColor: String = "#408080",
    backgroundColor: String = "rgba(0, 0, 0, 0)"
  ): ChartDataset =
    js.Dynamic.literal(
      label = label,
      data = data.toJSArray,
      borderColor = borderColor,
      backgroundColor = backgroundColor
    ).asInstanceOf[ChartDataset]
}

@js.native
trait ChartData extends js.Object {
  def labels: js.Array[String] = js.native
  def datasets: js.Array[ChartDataset] = js.native
}

object ChartData {
  def apply(labels: Seq[String], datasets: Seq[ChartDataset]): ChartData =
    js.Dynamic.literal(
      labels = labels.toJSArray,
      datasets = datasets.toJSArray
    ).asInstanceOf[ChartData]
}

@js.native
trait ChartOptions extends js.Object {
  def responsive: Boolean = js.native
}

object ChartOptions {
  def apply(title: String, responsive: Boolean): ChartOptions = {
    val t = js.Dynamic.literal(display = true, text = title)
    js.Dynamic.literal(responsive = responsive, title = t).asInstanceOf[ChartOptions]
  }
}

@js.native
trait ChartConfiguration extends js.Object {
  def `type`: String = js.native
  def data: ChartData = js.native
  def options: ChartOptions = js.native
}

object ChartConfiguration {
  def apply(`type`: String, data: ChartData, options: ChartOptions): ChartConfiguration =
    js.Dynamic.literal(`type` = `type`, data = data, options = options)
      .asInstanceOf[ChartConfiguration]
}

@js.native
@JSGlobal("Chart")
class JSChart(ctx: js.Dynamic, config: ChartConfiguration) extends js.Object

object Chart {
  sealed trait ChartStyle
  case object LineChart extends ChartStyle
  case class ChartProps(name: String, style: ChartStyle, data: ChartData)

  def draw(ctx: js.Dynamic, props: ChartProps): Callback = Callback {
    props.style match {
      case LineChart =>
        new JSChart(ctx, ChartConfiguration("line", props.data, ChartOptions(props.name, true)))
      case _ => throw new IllegalArgumentException
    }
  }

  val chart = ScalaComponent.builder[ChartProps]("Chart")
    .render_P { p =>
      <.canvas(VdomAttr("width") := "100%", VdomAttr("height") := "60%")
    }
    .componentDidMount { scope =>
      // can't be factored out
      scope.getDOMNode match {
        case Right(n) =>
          val canvas = n.asInstanceOf[HTMLCanvasElement].getContext("2d")
          draw(canvas, scope.props)
        case Left(e) => Callback(println(e))
      }
    }
    .componentWillReceiveProps { scope =>
      // can't be factored out
      scope.getDOMNode match {
        case Right(n) =>
          val canvas = n.asInstanceOf[HTMLCanvasElement].getContext("2d")
          draw(canvas, scope.nextProps)
        case Left(e) => Callback(println(e))
      }
    }
    .build

  def apply(props: ChartProps) = chart(props)
}
