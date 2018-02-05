package com.cibo.evilplot.plot

import com.cibo.evilplot.colors.HTMLNamedColors
import com.cibo.evilplot.geometry._
import com.cibo.evilplot.numeric.AxisDescriptor
import com.cibo.evilplot.oldplot.Chart

object Axes {

  val defaultTickCount: Int = 10
  val defaultTickThickness: Double = 1
  val defaultTickLength: Double = 5

  /** Function to render a tick on the x axis.
    * @param length The length of the tick line.
    * @param thickness The thickness of the tick line.
    * @param rotateText The rotation of the label.
    */
  def xAxisTickRenderer[X](
    length: Double = defaultTickLength,
    thickness: Double = defaultTickThickness,
    rotateText: Double = 0
  )(labelOpt: Option[X]): Drawable = {
    val line = Line(length, thickness).rotated(90)
    labelOpt match {
      case Some(label) => Align.center(line, Text(label.toString).rotated(rotateText).padTop(2)).reduce(above)
      case None        => line
    }
  }

  /** Function to render a tick on the y axis.
    * @param length The length of the tick line.
    * @param thickness The thickness of the tick line.
    */
  def yAxisTickRenderer[Y](
    length: Double = defaultTickLength,
    thickness: Double = defaultTickThickness
  )(labelOpt: Option[Y]): Drawable = {
    val line = Line(length, thickness)
    labelOpt match {
      case Some(label) => Align.middle(Text(label.toString).padRight(2).padBottom(2), line).reduce(beside)
      case None        => line
    }
  }

  def xGridLineRenderer(
    thickness: Double = defaultTickThickness
  )(label: String, extent: Extent): Drawable = {
    Line(extent.height, thickness).colored(HTMLNamedColors.white).rotated(90)
  }

  def yGridLineRenderer(
    thickness: Double = defaultTickThickness
  )(label: String, extent: Extent): Drawable = {
    Line(extent.width, thickness).colored(HTMLNamedColors.white)
  }

  private sealed abstract class AxisPlotComponent extends PlotComponent with Plot.Transformer {
    val tickCount: Int
    val tickRenderer: Option[String] => Drawable

    final override val repeated: Boolean = true

    protected def ticks(descriptor: AxisDescriptor): Seq[Drawable] = {
      for {
        i <- 0 until descriptor.numTicks
        x = descriptor.axisBounds.min + i * descriptor.spacing
        label = Chart.createNumericLabel(x, descriptor.numFrac)
      } yield tickRenderer(Some(label))
    }
  }

  private case class XAxisPlotComponent(
    tickCount: Int,
    tickRenderer: Option[String] => Drawable
  ) extends AxisPlotComponent {
    val position: PlotComponent.Position = PlotComponent.Bottom

    override def size[T](plot: Plot[T]): Extent =
      ticks(AxisDescriptor(plot.xbounds, tickCount)).maxBy(_.extent.height).extent

    def render[T](plot: Plot[T], extent: Extent): Drawable = {
      val descriptor = AxisDescriptor(plot.xbounds, tickCount)
      val scale = extent.width / descriptor.axisBounds.range
      ticks(descriptor).zipWithIndex.map { case (tick, i) =>
        val offset = i * descriptor.spacing * scale - tick.extent.width / 2.0
        Translate(tick, x = offset)
      }.group
    }

    def apply(plot: Plot[_], plotExtent: Extent): Double => Double = {
      val descriptor = AxisDescriptor(plot.xbounds, tickCount)
      val scale = plotExtent.width / descriptor.axisBounds.range
      (x: Double) => (x - descriptor.axisBounds.min) * scale
    }
  }

  private case class YAxisPlotComponent(
    tickCount: Int,
    tickRenderer: Option[String] => Drawable
  ) extends AxisPlotComponent {
    val position: PlotComponent.Position = PlotComponent.Left

    override def size[T](plot: Plot[T]): Extent =
      ticks(AxisDescriptor(plot.ybounds, tickCount)).maxBy(_.extent.width).extent

    def render[T](plot: Plot[T], extent: Extent): Drawable = {
      val descriptor = AxisDescriptor(plot.ybounds, tickCount)
      val scale = extent.height / descriptor.axisBounds.range
      val ts = ticks(descriptor)
      val maxWidth = ts.maxBy(_.extent.width).extent.width
      ts.zipWithIndex.map { case (tick, i) =>
        val offset = i * descriptor.spacing * scale + tick.extent.height / 2.0
        Translate(tick, x = maxWidth - tick.extent.width, y = extent.height - offset)
      }.group
    }

    def apply(plot: Plot[_], plotExtent: Extent): Double => Double = {
      val descriptor = AxisDescriptor(plot.ybounds, tickCount)
      val scale = plotExtent.height / descriptor.axisBounds.range
      (y: Double) => plotExtent.height - (y - descriptor.axisBounds.min) * scale
    }
  }

  private sealed abstract class GridComponent extends PlotComponent {
    val lineRenderer: (String, Extent) => Drawable

    final val position: PlotComponent.Position = PlotComponent.Background
    override final val repeated: Boolean = true

    protected def lines(descriptor: AxisDescriptor, extent: Extent): Seq[Drawable] = {
      for {
        i <- 0 until descriptor.numTicks
        value = descriptor.axisBounds.min + i * descriptor.spacing
        label = Chart.createNumericLabel(value, descriptor.numFrac)
      } yield lineRenderer(label, extent)
    }
  }

  private case class XGridComponent(
    lineCount: Int,
    lineRenderer: (String, Extent) => Drawable
  ) extends GridComponent {
    def render[T](plot: Plot[T], extent: Extent): Drawable = {
      val descriptor = AxisDescriptor(plot.xbounds, lineCount)
      val scale = extent.width / descriptor.axisBounds.range
      lines(descriptor, extent).zipWithIndex.map { case (line, i) =>
        val offset = i * descriptor.spacing * scale - line.extent.width / 2.0
        Translate(line, x = offset)
      }.group
    }
  }

  private case class YGridComponent(
    lineCount: Int,
    lineRenderer: (String, Extent) => Drawable
  ) extends GridComponent {
    def render[T](plot: Plot[T], extent: Extent): Drawable = {
      val descriptor = AxisDescriptor(plot.ybounds, lineCount)
      val scale = extent.height / descriptor.axisBounds.range
      val ls = lines(descriptor, extent)
      val maxWidth = ls.maxBy(_.extent.width).extent.width
      ls.zipWithIndex.map { case (line, i) =>
        val offset = i * descriptor.spacing * scale + line.extent.height / 2.0
        Translate(line, x = maxWidth - line.extent.width, y = extent.height - offset)
      }.group
    }
  }

  trait AxesImplicits[T] {
    protected val plot: Plot[T]

    /** Add an X axis to the plot.
      * @param tickCount    The number of tick lines.
      * @param tickRenderer Function to draw a tick line/label.
      */
    def xAxis(
      tickCount: Int = defaultTickCount,
      tickRenderer: Option[String] => Drawable = xAxisTickRenderer()
    ): Plot[T] = {
      val component = XAxisPlotComponent(tickCount, tickRenderer)
      component +: plot.copy(xtransform = component)
    }

    /** Add a Y axis to the plot.
      * @param tickCount    The number of tick lines.
      * @param tickRenderer Function to draw a tick line/label.
      */
    def yAxis(
      tickCount: Int = defaultTickCount,
      tickRenderer: Option[String] => Drawable = yAxisTickRenderer()
    ): Plot[T] = {
      val component = YAxisPlotComponent(tickCount, tickRenderer)
      component +: plot.copy(ytransform = component)
    }

    def xGrid(
      lineCount: Int = defaultTickCount,
      lineRenderer: (String, Extent) => Drawable = xGridLineRenderer()
    ): Plot[T] = {
      plot :+ XGridComponent(lineCount, lineRenderer)
    }

    def yGrid(
      lineCount: Int = defaultTickCount,
      lineRenderer: (String, Extent) => Drawable = yGridLineRenderer()
    ): Plot[T] = {
      plot :+ YGridComponent(lineCount, lineRenderer)
    }
  }
}
