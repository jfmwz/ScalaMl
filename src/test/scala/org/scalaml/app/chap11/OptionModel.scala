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
package org.scalaml.app.chap11

import org.scalaml.reinforcement.qlearning
import org.scalaml.plots.{ScatterPlot, LinePlot, LightPlotTheme}
import org.scalaml.workflow.data.DataSource
import org.scalaml.core.XTSeries
import org.scalaml.trading.YahooFinancials
import org.scalaml.core.Types.ScalaMl.DblVector
import org.scalaml.util.{Counter, NumericAccumulator, Display}

import org.apache.log4j.Logger


		/**
		 * <p>Class that defines an option as a symbol, a strike price, a source
		 * for the pricing, the minimum time to expiration of the options and the number
		 * of steps for encoding.</p>
		 * @constructor Create an option 
		 * @throws IllegalArgumenException if symbol, data source are undefined or the minimum time to expiration or number of steps is out of range.
		 * @param symbol Symbol for the security or stock, this option is derived
		 * @param strikePrice  Strike price for the option
		 * @param src Data source for the price of the underlying security
		 * @param minExpT Minimum expiration time for the option to be considered for analysis
		 * @param nSteps Number of steps used in the discretization of the feature
		 * 
		 * @author Patrick Nicolas
		 * @since January 27, 2014
		 * @note Scala for Machine Learning Appendix / Finances 101 / Option trading
		 */
class OptionModel(symbol: String, strikePrice: Double, src: DataSource, minExpT: Int, nSteps: Int) {
	import scala.collection.mutable.ArrayBuffer
	import scala.collection.mutable.HashMap
	import YahooFinancials._, OptionModel._
	
	check(symbol, src, minExpT, nSteps)
	private val price = src |> adjClose
	private val futurePrice = price.drop(2)
	
	private val propsList: List[OptionProperty] = {
		val rVolatility = XTSeries.normalize((src |> relVolatility).toArray).get
		val volByVol = XTSeries.normalize((src |> volatilityByVol).toArray).get
		val relPriceToStrike = XTSeries.normalize(price.map(p => 1.0 - strikePrice/p)).get

		rVolatility.zipWithIndex
					.foldLeft(List[OptionProperty]())((xs, e) => {
			val normDecay = (e._2+minExpT).toDouble/(price.size+minExpT)
			new OptionProperty(normDecay, e._1, volByVol(e._2), relPriceToStrike(e._2)) :: xs
		}).drop(2).reverse
	}

		/**
		 * <p>Method that generate a map of state value (as array of Int) 
		 * as key and price variation as value. The method is called approximate
		 * because the key is generated by approximating the option price.</p>
		 * @param y price of the option
		 * @return map of tuple (array of discretized value, variation of security price)
		 * @throws IllegalArgumentException if the option price is undefined
		 */
	def approximate(y: DblVector): Map[Array[Int], Double] = {
		require(y != null && y.size > 0, "OptionModel.approximate option price history undefined")
		val mapper = new HashMap[Int, Array[Int]]
  	    
		val acc = new NumericAccumulator[Int]
		propsList.map( _.toArray)
				.map( toArrayInt( _ ))
				.map(ar => { 
					val enc = encode(ar)
					mapper.put(enc, ar)
					enc
				})
				.zip(y)
				.foldLeft(acc)((acc, t) => {
					acc += (t._1, t._2)
					acc 
				})
  	  
		acc.map(kv => (kv._1, kv._2._2/kv._2._1))
			.map(kv => (mapper(kv._1), kv._2)).toMap
	}
   
  
	private def encode(arr: Array[Int]): Int = 
		arr.foldLeft((1, 0))((s, n) => {
			val np = s._1*n
			(s._1*nSteps, s._2 + np)
		})._2

    	  
	private def toArrayInt(feature: DblVector): Array[Int] = feature.map(x => (nSteps*x).floor.toInt)
}


		/**
		 * Class that encapsulate the properties of an option (time to expiration of the option,
		 * relative volatility of the price of the underlying security, volatility of the price
		 * of the security relative to the trading session volume, price of the security relative
		 * to the strike price of the option)
		 * @param timeToExp time to expiration of the option 
		 * @param relVolatility relative volatility of the price of the underlying security
		 * @param volatilityByVol volatility of the price of the security relative to the trading session volume
		 * @param relPriceToStrike  price of the security relative to the strike price of the option
		 * @constructor Create a option property instance
		 * 
		 * @author Patrick Nicolas
		 * @since January 27, 2014
		 * @note Scala for Machine Learning Appendix / Finances 101 / Option trading
		 */
class OptionProperty(timeToExp: Double, relVolatility: Double, volatilityByVol: Double, relPriceToStrike: Double) {
   val toArray = Array[Double](timeToExp, relVolatility, volatilityByVol, relPriceToStrike)
}

		/**
		 * Companion object for the OptionModel used to validate the class parameters.
		 * @author Patrick Nicolas
		 * @since January 27, 2014
		 * @note Scala for Machine Learning Appendix / Finances 101 / Option trading
		 */
object OptionModel {
	private val MAX_MINEXPT = 365
	private val MAX_NSTEPS = (2 <<16)
	
	private def check(symbol: String, src: DataSource, minExpT: Int, nSteps: Int): Unit =  {
		require(symbol != null && symbol.length > 1, "OptionModel.check symbol undefined")
		require(src != null, "OptionModel.check data source undefined")
		require(minExpT > 1 && minExpT < MAX_MINEXPT, s"OptionModel.check minExpT $minExpT is out of range")
		require(nSteps > 0 && nSteps < MAX_NSTEPS, s"OptionModel.check nSteps $nSteps is out of range")
	}
}

// ------------------------------------  EOF ----------------------------------