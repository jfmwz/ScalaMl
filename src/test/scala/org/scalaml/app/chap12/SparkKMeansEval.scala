/**
 * Copyright 2013, 2014, 2015  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Version 0.96d
 */
package org.scalaml.app.chap12


import org.scalaml.core.XTSeries
import scala.util.Random
import scala.io.Source
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.Config
import akka.actor.Props
import scala.concurrent.{Await, duration}
import org.scalaml.core.Types.ScalaMl._
import org.scalaml.scalability.spark._
import org.apache.spark.SparkContext
import org.apache.spark.storage.StorageLevel
import org.scalaml.scalability.akka._
import org.scalaml.scalability.scala._
import org.scalaml.util.Display
import org.apache.log4j.{Logger, Level}
import scala.util.{Try, Success, Failure}
import org.scalaml.app.Eval
import org.apache.spark.SparkConf


object SparkKMeansEval extends Eval {
	val name: String = "SparkKMeansEval"
	private val logger = Logger.getLogger(name)
	val maxExecutionTime: Int = 5000
	
	private val K = 8
	private val NRUNS = 16
	private val MAXITERS = 200
	private val PATH = "resources/data/chap12/CSCO.csv"
	private val CACHE = true
	
		/**
		 * <p>Execution of the scalatest for SparkKMeans class. This method is invoked by the 
		 * actor-based test framework function, ScalaMlTest.evaluate</p>
		 * @param args array of arguments used in the test
		 * @return -1 in case error a positive or null value if the test succeeds. 
		 */
	def run(args: Array[String]): Int = {
		Display.show(s"\n\n *****  test#${Eval.testCount} $name MLLib K-means on Spark framework", logger)
  	
		Try {
			val input = extract
			val volatilityVol = input(0).zip(input(1)).map( x => Array[Double](x._1, x._2))
			
				// Disable Info for the Spark logger.
			Logger.getRootLogger.setLevel(Level.ERROR)
			val sparkConf = new SparkConf().setMaster("local[8]")
											.setAppName("SparkKMeans")
											.set("spark.executor.memory", "2048m")
											
			val config = new SparkKMeansConfig(K, MAXITERS, NRUNS)
			implicit val sc = new SparkContext(sparkConf)  // no need to load additional jar file
	
			val rddConfig = RDDConfig(CACHE, StorageLevel.MEMORY_ONLY)
			val sparkKMeans = SparkKMeans(config, rddConfig, XTSeries[DblVector](volatilityVol))
			
			Display.show(s"\n${sparkKMeans.toString}\nPrediction:\n", logger)
			val obs = Array[Double](0.23, 0.67)
			val clusterId1 = sparkKMeans |> obs
			Display.show(s"(${obs(0)},${obs(1)}) => Cluster #$clusterId1", logger)

			val obs2 = Array[Double](0.56, 0.11)
			val clusterId2 = sparkKMeans |> obs2 
			Display.show(s"(${obs2(0)},${obs2(1)}) => Cluster #$clusterId2", logger)
			
			// SparkContext is cleaned up gracefully
			sc.stop
			Display.show("Completed", logger)
		}
		match {
			case Success(n) => n
			case Failure(e) => Display.error(s"$name run failed", logger, e)
		}
	}
  
  
	private def extract: List[DblVector] = {
		import org.scalaml.trading.YahooFinancials
		import org.scalaml.workflow.data.DataSource

		val extractors = List[Array[String] => Double](
			YahooFinancials.volatility, YahooFinancials.volume 
		)	
		DataSource(PATH, true) |> extractors
	}
}

// ---------------------------------  EOF -------------------------