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
package org.scalaml.app.chap5


import org.scalaml.supervised.bayes._
import org.scalaml.core.XTSeries
import org.scalaml.core.Types.ScalaMl
import org.scalaml.workflow.data.{DataSource,DocumentsSource}
import scala.collection.mutable.ArrayBuffer
import ScalaMl._
import org.scalaml.filtering.SimpleMovingAverage
import SimpleMovingAverage._
import scala.collection.immutable.HashSet
import org.scalaml.supervised.bayes.NaiveBayes
import scala.util.{Try, Success, Failure}
import org.apache.log4j.Logger
import org.scalaml.util.Display
import org.scalaml.app.Eval


		/**
		 * <p>Generic trait that implements the extraction of data associated to a stock
		 * or ETF ticker symbol, located into a directory
		 */
trait BayesEval extends Eval {
	import org.scalaml.trading.YahooFinancials._
	final val path = "resources/data/chap5/"

	  
	override def run(args: Array[String]): Int
  
	protected val extractor = toDouble(CLOSE) :: 
								ratio (HIGH, LOW) :: 
								toDouble(VOLUME) ::
								List[Array[String] =>Double]()
	protected def symbolFiles = DataSource.listSymbolFiles(path)
}



			/**
			 * <p>Singleton to evaluate the Binomial Naive Bayes classifier.</p>
			 */
object BinomialBayesEval extends BayesEval {
	val name: String = "BinomialBayesEval"
	val maxExecutionTime: Int = 5000
	private val logger = Logger.getLogger(name)
	
		/**
		 * <p>Execution of the scalatest for <b>NaiveBayes</b> class
		 * This method is invoked by the  actor-based test framework function, ScalaMlTest.evaluate</p>
		 * @param args array of arguments used in the test
		 * @return -1 in case error a positive or null value if the test succeeds. 
		 */
	override def run(args: Array[String]): Int = {
		require(args != null && args.size > 2, s"$name.run incorrect arguments list")
		Display.show(s"\n\n *****  test#${Eval.testCount} $name Evaluation multinomial Naive Bayes", logger)	
		
		val trainValidRatio = args(1).toDouble
		val period = args(2).toInt
		val description = s"symbol: ${args(0)} smoothing period:$period"

		Try {
			val input = load(args(0), period)
			val labels = XTSeries[(Array[Int], Int)](input.map( x => 
				(x._1.toArray, x._2)).toArray)
				
			val numObsToTrain  = (trainValidRatio*labels.size).floor.toInt
			val nb = NaiveBayes[Int](labels.take(numObsToTrain))
			validate(labels.drop(numObsToTrain+1), nb)
		} 
		match {
			case Success(res) => Display.show(s"$name results for ${args(0)} $description F1 = ${res.get}", logger)
			case Failure(e) => Display.error(s"$name.run failed", logger, e)
		}
	}
	
	private def validate(input:  XTSeries[(Array[Int], Int)], nb: NaiveBayes[Int]): Option[Double] = 
		nb.validate(input, 0)
	
	
	private def load(ticker: String, period: Int): List[(List[Int], Int)] = {
		val symbol = s"${ticker}.csv"
		val xs = DataSource(symbol, path, true) |> extractor
		val mv = SimpleMovingAverage[Double](period)
     
		val ratios: List[Array[Int]] = xs.map(x => {
			val xt = mv get x.toArray
			val zValues: Array[(Double, Double)] = x.drop(period).zip(xt.drop(period))
			zValues.map(z => if(z._1 > z._2) 1 else 0).toArray
		})
	          
			// Compute the difference in price. If the price(t+1) > price(t) then 
			// the label is set to 1, 0 otherwise. We need to shift the index by +1
			// by dropping the first period +1 price points.
		var prev = xs(0)(period)
		val label = xs(0).drop(period+1).map( x => {
			val y = if( x > prev) 1 else 0
			prev = x
			y	
		}).toArray 
		
		// Transpose the list of ratios and zip with the label
		ratios.transpose.take(label.size).zip(label) 
	}
}


// -----------------------------  EOF ------------------------------------