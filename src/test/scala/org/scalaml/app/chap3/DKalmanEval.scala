/**
 * Copyright 2013, 2014, 2015  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Version 0.96
 */
package org.scalaml.app.chap3

import scala.util.{Random, Try, Success, Failure}
import org.scalaml.core.Types.ScalaMl
import org.scalaml.filtering.{DKalman, QRNoise}
import org.scalaml.core.XTSeries
import org.scalaml.workflow.data.{DataSource, DataSink}
import org.scalaml.trading.YahooFinancials
import scala.annotation.implicitNotFound
import org.apache.log4j.Logger
import org.scalaml.util.Display
import org.scalaml.app.Eval

		/**
		 * <p>Class to evaluate the Kalman filter algorithm on a time series using
		 * a simple 2-step lag formulation<br>
		 * x(t+1) = alpha.x(t) +(1-alpha).x(t-1)<br>
		 * x(t) = x(t)<br>
		 * 
		 * @author Patrick Nicolas
		 * @since February 10, 2014
		 * @note Scala for Machine Learning
		 */
@implicitNotFound("Kalman filter require implicit conversion Double to String")
final class DKalmanEval extends FilteringEval {
	import YahooFinancials._, ScalaMl._
	val name: String = "DKalmanEval"
    val maxExecutionTime: Int = 25000
    
	private val logger = Logger.getLogger(name)
     
		// Noise has to be declared implicitly
	implicit val qrNoise = QRNoise((0.7, 0.3), (m: Double) => m*Random.nextGaussian)   
		// Contract extractor
	private val extractor = YahooFinancials.adjClose :: List[Array[String] =>Double]()
	
		/**
		 * <p>Execution of the scalatest for <b>DKalman</b> class 
		 * This method is invoked by the  actor-based test framework function, ScalaMlTest.evaluate</p>
		 * @param args array of arguments used in the test
		 * @return -1 in case error a positive or null value if the test succeeds. 
		 */
	override def run(args: Array[String]): Int = {
		require(args != null && args.size > 0, s"$name Command line DKalmanEval ticker symbol")
     
		Display.show(s"\n\n *****  test#${Eval.testCount} $name Evaluation Kalman filter with no control matrix", logger)
     
			// H and P0 are the only components that are independent from
			// input data and smoothing factor. The control matrix B is not defined
			// as there is no external control on the time series.
		val H: DblMatrix = ((0.9, 0.0), (0.0, 0.1))
		val P0: DblMatrix = ((0.4, 0.3), (0.5, 0.4))
     
		/**
		 * Inner function that updates the parameters/matrices for a two-step lag
		 * Kalman filter.
		 */
		def twoStepLagSmoother(zSeries: DblVector, alpha: Double): Int = {
			require(alpha > 0.0 && alpha < 1.0, s"$name smoothing factor $alpha is out of range")
    	 
			// Generates the A state transition matrix from the times series updating equation
			val A: DblMatrix = ((alpha, 1.0-alpha), (1.0, 0.0))

			// Generate the state as a time series of pair [x(t+1), x(t)]
			val zt_1 = zSeries.drop(1)
			val zt = zSeries.dropRight(1)
         
			// Applied the Kalman smoothing for [x(t+1), x(t)]
			val filtered = DKalman(A, H, P0) |> XTSeries[(Double, Double)](zt_1.zip(zt))
	     
			// Dump results in output file along the original time series
			val output = s"output/chap3/kalman_${alpha.toString}.csv"
			val results: XTSeries[Double] = filtered.map(_._1)
			DataSink[Double](output) |> results :: XTSeries[Double](zSeries) :: List[XTSeries[Double]]()
			val displayedResults: DblVector = results.toArray.take(256)
			
			display(zSeries, results.toArray, alpha)
			Display.show(s"$name results ${ScalaMl.toString(displayedResults, "2-step lag smoother", false)}", logger)

		}
      
		Try {
			val symbol = args(0)
			val source = DataSource("resources/data/chap3/" + symbol + ".csv", false)
			val zt = (source |> YahooFinancials.adjClose).toArray  

			twoStepLagSmoother(zt, 0.5)
			twoStepLagSmoother(zt, 0.8)
		} 
		match {
			case Success(n) => n
			case Failure(e) => Display.error(s"$name Failed", logger, e)
		}
	}
	
	private def display(z: DblVector, x: DblVector, alpha: Double): Unit =   {
		import org.scalaml.plots.{LinePlot, LightPlotTheme}
		
		val plot = new LinePlot(("Kalman filter", s"Kalman with alpha $alpha", "y"), new LightPlotTheme)
		val data = (z, "price") :: (x, "Filtered") :: List[(DblVector, String)]()
		plot.display(data, 340, 280)
	}
}

object DKalmanEval {
	def apply: DKalmanEval = new DKalmanEval
}


// --------------------------------------  EOF -------------------------------