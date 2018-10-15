/*
 * Copyright (c) 2018, CiBO Technologies, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.cibo.evilplot.demo

import com.cibo.evilplot.colors._
import com.cibo.evilplot.geometry._
import com.cibo.evilplot.numeric._
import com.cibo.evilplot.{geometry, plot}
import com.cibo.evilplot.plot._
import com.cibo.evilplot.plot.aesthetics.DefaultTheme.{DefaultFonts, DefaultTheme}
import com.cibo.evilplot.plot.aesthetics.Theme
import com.cibo.evilplot.plot.components.{Legend, Marker, Position}
import com.cibo.evilplot.plot.renderers._

import scala.util.Random

object FastTest extends App{

  // val data = (0.0 to 3 by .25) ++ (3.0 to 5 by .05) ++ (5.0 to 8 by 1.0)


  implicit val theme: Theme = DefaultTheme.copy(
    fonts = DefaultFonts
      .copy(tickLabelSize = 14, legendLabelSize = 14, fontFace = "'Lato', sans-serif")
  )


  // val render = Histogram(data)//, 20)
  //   .standard()
  //   // .xbounds(-75, 225)
  //   // .ybounds(0, 15)
  //   // .vline(3.5, HTMLNamedColors.blue)
  //   .render(plotAreaSize)


  val plotAreaSize: Extent = Extent(1000, 600)

  def hist(data:Seq[Double], bins:Int, label:String):Unit = {
    // val renderOld = Histogram(data,bins).standard().ybounds(0,data.size/bins + 2).render(plotAreaSize)


    val render = {
      val histogramPlot = BinnedPlot.continuous[Double](  // creates a histogram
            data,
            _.continuousBins(identity, bins)
          )(_.histogram())

      histogramPlot.standard().ybounds(0, data.size).render(plotAreaSize)
    }

    val file = new java.io.File(s"FastTest-$label-${bins}bins.png")
    render.write(file)
  }



  // -- bugs
  // 1. binning edge case
  // 2. standard breaks the edge cases???
  // 3. ybounds is related to all this
  //  Its is all broken because the view bounds if filtering the data that is binned.  This shouldn't be possible.
  //   lets try Chris's to see if it fails under the same problems
  //  Chris has the same problem
  //
  //  the fix should be deconflate plot range and data range (hard when both are auto too)
  //  1. do the binning
  //  2. do the *viewing* NOT filtering data for binning
  //  3. validate that the axis are correct.  maybe the shape and clipping are fixed in aaron and bill's hack but axis is off

  //---
  val data = 1d to 10d by 1d
  val moreData = Seq.fill(10)(data).flatten
  hist(moreData, 2, "1-10")
  hist(moreData, 5, "1-10")

  val uniform = Seq.fill(10000)(Random.nextDouble()*10)
  hist(uniform, 5, "uniform")

}

