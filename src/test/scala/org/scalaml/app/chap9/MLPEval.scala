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
package org.scalaml.app.chap9

import org.scalaml.workflow.data.DataSource
import org.scalaml.supervised.nnet._
import org.scalaml.trading.GoogleFinancials
import org.scalaml.core.Types.ScalaMl.{DblVector, DblMatrix}
import GoogleFinancials._
import org.scalaml.util.Display
import org.apache.log4j.Logger
import org.scalaml.app.Eval



		/**
		 * <p>Singleton to evaluate the Multi-layer perceptron by classifying
		 * Exchange Traded funds (ETF)</p>
		 */
object MLPEval extends Eval {
	val name: String = "MLPEval"
    val maxExecutionTime: Int = 45000
    
	private val path = "resources/data/chap9/"
	private val ALPHA = 0.8; 
	private val ETA = 0.01
	private val NUM_EPOCHS = 250
	private val EPS = 1e-3
	private val THRESHOLD = 0.12

	private val symbols = Array[String](
		"FXE", "FXA", "SPY", "GLD", "FXB", "FXF", "FXC", "FXY", "CYB"
	)
	private val logger = Logger.getLogger(name)

		/** <p>Execution of the scalatest for <b>MLP</b> class.
		 * This method is invoked by the  actor-based test framework function, ScalaMlTest.evaluate</p>
		 * @param args array of arguments used in the test
		 * @return -1 in case error a positive or null value if the test succeeds. 
		 */
	private val STUDIES = List[Array[String]](
		Array[String]("FXY", "FXC", "GLD", "FXA"),
		Array[String]("FXE", "FXF", "FXB", "CYB"),
		Array[String]("FXE", "FXC", "GLD", "FXA", "FXY", "FXB"),
		Array[String]("FXC", "FXY", "FXA"),
		Array[String]("CYB", "GLD", "FXY"),
		symbols	
	)
  
  		/**
		 * <p>Execution of the scalatest for <b>MLP</b> class 
		 * This method is invoked by the  actor-based test framework function, ScalaMlTest.evaluate</p>
		 * @param args array of arguments used in the test
		 * @return -1 in case error a positive or null value if the test succeeds. 
		 */
	def run(args: Array[String]): Int =  {
		Display.show(s"\n\n *****  test#${Eval.testCount} $name MLP classifier without SoftMax conversion", logger)
       
		val prices = symbols.map(s => DataSource(s"$path$s.csv", true, true, 1))
							.map( _ |> GoogleFinancials.close )
							.map( _.toArray)
  	 
		Display.show(s"$name size: ${prices(0).size}", logger)
		test(Array[Int](4), prices)
		test(Array[Int](4, 4), prices)
		test(Array[Int](7, 7), prices)
		// test(Array[Int](8, 5, 6), prices)
	}
  

	private def test(hidLayers: Array[Int], prices: DblMatrix): Int = {
		val networkArchitecture = hidLayers.foldLeft(new StringBuilder)((b,n)=>b.append(s"$n ")).toString
		Display.show(s"$name \n${hidLayers.size} layers: ( ${networkArchitecture})", logger)
  
		val startTime = System.currentTimeMillis
		val config = MLPConfig(ALPHA, ETA, hidLayers, NUM_EPOCHS, EPS)
		
		STUDIES.foreach(etfs => eval(prices, config, etfs))
		Display.show(s"$name Duration ${(System.currentTimeMillis - startTime)} msecs.", logger)	
	}

	private def eval(obs: DblMatrix, 
					config: MLPConfig, 
					etfsSet: Array[String]): Int = accuracy(etfsSet, obs, config) match {
	  
		case Some(acc) => Display.show(s"$name accuracy: $acc", logger)
		case None => Display.error(s"$name could not compute the accuracy", logger)
	}
 

	private def accuracy(symbols: Array[String], prices: DblMatrix, config: MLPConfig): Option[Double] = {  	 
		val obs: DblMatrix = symbols.map( sym => index.get(sym).get)
									.map( prices( _ ) )

		val features = obs.drop(1).transpose
		val target = Array[DblVector](obs(0)).transpose

		implicit val mlpObjective = new MLP.MLPBinClassifier
		val classifier = MLP[Double](config, features, target)
		classifier.accuracy(THRESHOLD)
	}
  
    
	private val index: Map[String, Int] = {
		import scala.collection.mutable.HashMap
  		 
		val _index = new HashMap[String, Int]
		symbols.zipWithIndex.foldLeft(new HashMap[String, Int])((mp, si) 
				=> {mp.put(si._1, si._2); mp}).toMap
	}

	private def toString(symbols: Array[String]): String = 
		new StringBuilder(symbols.drop(1).foldLeft(new StringBuilder)((b,s) => b.append(s"$s ")).toString)
				.append(s"=> ${symbols(0)}").toString
}

// -------------------------------------  EOF ----------------------------------------------------------