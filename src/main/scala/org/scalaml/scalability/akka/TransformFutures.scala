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
package org.scalaml.scalability.akka


import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.mutable.ArrayBuffer
import org.scalaml.core.Types.ScalaMl._
import org.scalaml.scalability.akka.message._
import akka.actor._
import akka.util.Timeout
import org.scalaml.core.XTSeries
import org.scalaml.core.design.PipeOperator
import XTSeries._
import org.scalaml.util.Display
import org.apache.log4j.Logger
import scala.concurrent.duration.Duration


		/**
		 * <p>Generic implementation of the distributed transformation of time series using blocking futures.</p>
		 * @constructor Create a distributed transformation for time series. 
		 * @throws IllegalArgumentException if the class parameters are either undefined or out of range.
		 *  @param xt Time series to be processed
		 *  @param fct Data transformation of type PipeOperator
		 *  @param partitioner Methodology to partition a time series in segments or partitions to be processed by workers.
		 * 
		 *  @author Patrick Nicolas
		 *  @since March 30, 2014
		 *  @note Scala for Machine Learning Chapter 12 Scalable Frameworks/Akka/Futures
		 */			
abstract class TransformFutures(_xt: DblSeries, _fct: PipeOperator[DblSeries, DblSeries], _partitioner: Partitioner)(implicit timeout: Timeout) 
					extends Controller(_xt, _fct, _partitioner) {
		
	private val logger = Logger.getLogger("TransformFutures")

		/**
		 * <p>Message handling for the future-based controller for the transformation of time series.</p>
		 * <b>Start</b> to initiate the future computation of transformation of time series.<.p>
		 */
	override def receive = {
		case s: Start => compute(transform)
		case _ => Display.show("TransformFutures.receive Message not recognized", logger)
	}
  
	
	private def transform: Array[Future[DblSeries]] = {   
		val partIdx = partitioner.split(xt)
		val partitions: Iterable[DblSeries] = partIdx.map(n => XTSeries[Double](xt.slice(n - partIdx(0), n).toArray))

		val futures = new Array[Future[DblSeries]](partIdx.size)
		partitions.zipWithIndex.foreach(pi => {
			futures(pi._2) = Future[DblSeries] { 
				fct |> pi._1
			}
		}) 
		futures
	}
	
		/**
		 * <p>Executes the aggregation of the results for all the future execution of 
		 * the transformation of time series, using the transform fct.</p>
		 * @param futures Set of future transformation of time series using the transform fct.
		 * @throws IllegalArgumentException if futures are undefined
		 */
	private def compute(futures: Array[Future[DblSeries]]): Seq[Double] = {
		require(futures != null && futures.size > 0, "Cannot delegate computation to undefined futures")
  	  
		val results = futures.map(Await.result(_, timeout.duration))
		aggregate(results)
	}
	
	
		/**
		 * <p>Executes the aggregation of the results for all the future execution of 
		 * the transformation of time series, using the transform fct.</p>
		 * @param results of the distributed processing of the time series by futures
		 * @throws IllegalArgumentException if the results are undefined
		 */
	protected def aggregate(results: Array[DblSeries]): Seq[Double] 
}


// ------------------------  EOF -----------------------------------------------------------------------------