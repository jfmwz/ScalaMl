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
package org.scalaml.app.chap6

import org.scalaml.workflow.data.{DataSource, DataSink}
import org.scalaml.trading.YahooFinancials
import YahooFinancials._
import org.scalaml.core.XTSeries
import org.scalaml.core.Types.ScalaMl
import org.scalaml.supervised.regression.linear.RidgeRegression
import org.scalaml.util.Display
import org.apache.log4j.Logger
import scala.util.{Try, Success, Failure}
import org.scalaml.app.Eval
import ScalaMl._




object RidgeRegressionEval extends Eval {
	val name: String = "RidgeRegressionEval"
	val maxExecutionTime: Int = 7000
	
	final val path = "resources/data/chap6/CU.csv"
	final val dataInput = "output/chap6/CU_input.csv"
    
	private val logger = Logger.getLogger(name)	 
	
	
		 /**
		 * <p>Execution of the scalatest for <b>RidgeRegression</b> class.
		 * This method is invoked by the  actor-based test framework function, ScalaMlTest.evaluate</p>
		 * @param args array of arguments used in the test
		 * @return -1 in case error a positive or null value if the test succeeds. 
		 */
	def run(args: Array[String]): Int = {
		Display.show(s"\n\n *****  test#${Eval.testCount} $name Evaluation of Ridge regression", logger)
  	   	 
		Try {
			val src = DataSource(path, true, true, 1)
			val price = src |> YahooFinancials.adjClose
			val volatility = src |> YahooFinancials.volatility 
			val volume = src |> YahooFinancials.volume
		
			val deltaPrice = XTSeries[Double](price.drop(1).zip(price.dropRight(1)).map( z => z._1 - z._2))
		
			DataSink[Double](dataInput) |> deltaPrice :: 
										volatility :: 
										volume :: List[XTSeries[Double]]()
			val data =  volatility.zip(volume).map(z => Array[Double](z._1, z._2))
		
			val features = XTSeries[DblVector](data.dropRight(1))
			val regression = new RidgeRegression[Double](features, deltaPrice, 0.5)

			regression.weights match {
				case Some(w) => w.zipWithIndex.foreach( wi => Display.show(s"$name ${wi._1}${ScalaMl.toString(wi._2, ": ", true)}", logger))
				case None => Display.error(s"$name Ridge regression could not be trained", logger)
			}
		    
			regression.rss match {
				case Some(rss) => Display.show(s"$name ${ScalaMl.toString(rss, "rss =", false)}", logger)
				case None => Display.error(s"$name Ridge regression could not be trained", logger)
			}
		
			
			val y1 = predict(0.2, deltaPrice, volatility, volume)
			val y2 = predict(5.0, deltaPrice, volatility, volume)
			display(deltaPrice, y1, y2, 0.2, 5.0)
			
			if( regression.isModel ) {
				(2 until 10 by 2).foreach( n => { 
					val lambda = n*0.1
					val y = predict(lambda, deltaPrice, volatility, volume)
					Display.show(s"Lambda  $lambda", logger )
					Display.show(ScalaMl.toString(y, "", true), logger)
				})
				1
			}
			else
				-1
		} match {
			case Success(n) => n
			case Failure(e) => Display.error(s"$name.run Could not load data for Ridge regression", logger, e)
		}
 	}
   
 	private def rss(lambda: Double, deltaPrice: DblVector, volatility: DblVector, volume: DblVector): Double = {
		val data =  volatility.zip(volume).map(z => Array[Double](z._1, z._2))
		
		val features = XTSeries[DblVector](data.dropRight(1))
		val regression = new RidgeRegression[Double](features, deltaPrice, lambda)
		regression.rss.get
	}
   
	private def predict(lambda: Double, deltaPrice: DblVector, volatility: DblVector, volume: DblVector): DblVector = {
		val data =  volatility.zip(volume).map(z => Array[Double](z._1, z._2))
		
		val features = XTSeries[DblVector](data.dropRight(1))
		val regression = new RidgeRegression[Double](features, deltaPrice, lambda)
		features.map( regression |> _)
 	}
	
	private def display(z: DblVector, y1: DblVector, y2: DblVector, lambda1: Double, lambda2: Double): Unit = {
		import org.scalaml.plots.{LinePlot, LightPlotTheme}
	  
		val plot = new LinePlot(("Ridge Regression", s" L2 lambda impact", "y"), new LightPlotTheme)
		val data = (z, "Delta price") :: 
					(y1, s"L2 lambda $lambda1") :: 
					(y2, s"L2 lambda $lambda2") :: List[(DblVector, String)]()
		plot.display(data, 340, 280)
	}
}


// ----------------------------  EOF ----------------------------------