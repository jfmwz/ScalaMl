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
package org.scalaml.supervised.regression.logistic


import org.scalaml.core.XTSeries
import org.scalaml.core.Types.ScalaMl._
import org.scalaml.util.Matrix
import scala.util.Random
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum
import org.apache.commons.math3.optim.{SimpleVectorValueChecker, PointVectorValuePair, ConvergenceChecker}
import org.apache.commons.math3.linear.{RealVector, RealMatrix, MatrixUtils,Array2DRowRealMatrix, ArrayRealVector}
import org.apache.commons.math3.fitting.leastsquares.{LeastSquaresOptimizer, MultivariateJacobianFunction, LeastSquaresBuilder, LeastSquaresProblem}
import org.apache.commons.math3.util.Pair
import org.apache.commons.math3.optim.ConvergenceChecker
import LogisticRegression._
import org.apache.commons.math3.exception.{ConvergenceException, DimensionMismatchException, TooManyEvaluationsException, TooManyIterationsException, MathRuntimeException}
import org.scalaml.core.design.PipeOperator
import org.scalaml.supervised.regression.RegressionModel
import scala.util.{Try, Success, Failure}
import XTSeries._
import org.apache.log4j.Logger
import org.scalaml.util.Display
import org.apache.commons.math3.linear.DiagonalMatrix
import scala.language.implicitConversions

		/**
		 * <p>Logistic regression classifier. This implementation of the logistic regression does not 
		 * support regularization or penalty terms.
		 * <pre><span style="font-size:9pt;color: #351c75;font-family: &quot;Helvetica Neue&quot;,Arial,Helvetica,sans-serif;">
		 * The likelihood (conditional probability is computed as 1/(1 + exp(-(w(0) + w(1).x(1) + w(2).x(2) + .. + w(n).x(n))) </span></pre></p>
		 * @constructor Create a logistic regression classifier model.
		 * @throws IllegalArgumentException if the class parameters are undefined. 
		 * @see org.apache.commons.math3.fitting.leastsquares.
		 * @param xt Input time series observations.
		 * @param labels Labeled class data used during training of the classifier
		 * @param optimizer Optimization method used to minimmize the loss function during training
		 * @author Patrick Nicolas
		 * @since May 11, 2014
		 * @note Scala for Machine Learning Chapter 6 Regression and regularization/Logistic regression
		 */
final class LogisticRegression[T <% Double](xt: XTSeries[Array[T]], 
											labels: Array[Int], 
											optimizer: LogisticRegressionOptimizer) 
					extends PipeOperator[Array[T], Int] {
	import LogisticRegression._
  
	type Feature = Array[T]
	check(xt, labels, optimizer)
    
	private val logger = Logger.getLogger("LogisticRegression")
	
	private[this] val model: Option[RegressionModel] = {
		Try(train)
		match {
			case Success(m) => Some(m)
			case Failure(e) => Display.error("LogisticRegression", logger, e); None
		}
	}
	
		/**
		 * <p>Access the weights of the logistic regression model.</p>
		 * @return Vector of weights if the model has been successfully trained, None otherwise.
		 */
	final def weights: Option[DblVector] = model match {
		case Some(m) => Some(m.weights)
		case None => Display.none("LogisticRegression.weights model is undefined", logger)
	}
	
	
		/**
		 * <p>Access the residual sum of squares of the logistic regression model.</p>
		 * @return rss if the model has been successfully trained, None otherwise.
		 */
	final def rss: Option[Double] = model match {
		case Some(m) => Some(m.rss)
		case None => Display.none("LogisticRegression.tdd model is undefined", logger)
	}
	
		/**
		 * <p>Test if the model has been trained and is defined.</p>
		 * @return true is the model has been trained, false otherwise
		 */
	final def isModel = model != None
			
		/**
		 * <p>Binary predictor using the Binomial logistic regression and implemented
		 * as a data transformation (PipeOperator). The predictor relies on a margin
		 * error to associated to the outcome 0 or 1.</p>
		 * @throws MatchError if the model is undefined or has an incorrect size or the input feature is undefined
		 * @return PartialFunction of feature of type Array[T] as input and the predicted class as output
		 */
	override def |> : PartialFunction[Feature, Int] = {
		case x: Feature  if(x != null && x.size > 0 && model != None && (model.get.size -1 == x.size)) => {				
			val w = model.get.weights
			val z = x.zip(w.drop(1)).foldLeft(w(0))((s,xw) => s + xw._1*xw._2)
			if( logit(z) > 0.5 + MARGIN) 
				1 
			else 
				0
		}
	}
	
	@inline
	private def logit(x: Double): Double = 1.0/(1.0 + Math.exp(-x))
	
	
	final val initWeight = 0.5
	private def train: RegressionModel = {
		val weights0 = Array.fill(xt(0).size +1)(initWeight)
          
		/*
		 * <p>Anonymous Class that defines the computation of the value of
		 * the function and its derivative (Jacobian matrix) for all the data points.
		 */
		val lrJacobian = new MultivariateJacobianFunction {
			override def value(w: RealVector): Pair[RealVector, RealMatrix] = {
				require(w != null, "MultivariateJacobianFunction undefined weight for computing the Jacobian matrix")
				require(w.toArray.length == dimension(xt)+1, 
					s"MultivariateJacobianFunction number of weights ${w.toArray.length} should match the dimension of the time series ${dimension(xt)+1}")

				val _w = w.toArray 	  
					// computes the pair (function value, derivative value)
				val gradient = xt.toArray.map( g => {  
				val exponent = g.zip(_w.drop(1))
									.foldLeft(_w(0))((s,z) => s + z._1*z._2)
					val f = logit(exponent)
					(f, f*(1.0-f))
				})
		  	  
				val jacobian = Array.ofDim[Double](xt.size, weights0.size)
				xt.toArray.zipWithIndex.foreach(xi => {    // 1
					val df: Double = gradient(xi._2)._2
							Range(0, xi._1.size).foreach(j => jacobian(xi._2)(j+1) = xi._1(j)*df)
							jacobian(xi._2)(0) = 1.0
				})
	          
				(new ArrayRealVector(gradient.map(_._1)), new Array2DRowRealMatrix(jacobian))
			}
		}

        	
		val exitCheck = new ConvergenceChecker[PointVectorValuePair] {
			
			override def converged(iteration: Int, prev: PointVectorValuePair, current: PointVectorValuePair): Boolean =  {
				val delta = prev.getValue.zip(current.getValue).foldLeft(0.0)( (s, z) => { 
					val diff = z._1 - z._2
							s + diff*diff 
				})
				Math.sqrt(delta) < optimizer.eps && iteration >= optimizer.maxIters
			}
		}

		val builder = new LeastSquaresBuilder
		val lsp = builder.model(lrJacobian)
							.weight(MatrixUtils.createRealDiagonalMatrix(Array.fill(xt.size)(1.0))) 
							.target(labels)
							.checkerPair(exitCheck)
							.maxEvaluations(optimizer.maxEvals)
							.start(weights0)
							.maxIterations(optimizer.maxIters)
							.build

		val optimum = optimizer.optimize(lsp)
		RegressionModel(optimum.getPoint.toArray, optimum.getRMS)
	}
}


	/**
	 * <p>Companion object for the logistic regression. The singleton is used
	 * for conversion between Apache Common Math Pair Scala tuple and vice versa.
	 * The singleton is also used to define the constructors
	 * @author Patrick Nicolas
	 * @since May 11, 2014
	 * @note Scala for Machine Learning Chapter 6 Regression and regularization/Logistic regression
	 */
object LogisticRegression {
	final val MARGIN = 0.01
   
	implicit def pairToTuple[U, V](pair: Pair[U, V]): (U,V) = (pair._1, pair._2)
	implicit def tupleToPair[RealVector, RealMatrix](pair: (RealVector,RealMatrix)): Pair[RealVector,RealMatrix] 
		= new Pair[RealVector,RealMatrix](pair._1, pair._2)

		/**
		 * Default constructor for the logistic regression
		 * @param xt Input time series observations.
		 * @param labels Labeled class data used during training of the classifier
		 * @param optimizer Optimization method used to minimmize the loss function during training
		 */
	def apply[T <% Double](xt: XTSeries[Array[T]], labels: Array[Int], optimizer: LogisticRegressionOptimizer): LogisticRegression[T] =
		new LogisticRegression[T](xt, labels, optimizer)
  	    	
	private def check[T <% Double](xt: XTSeries[Array[T]], labels: Array[Int], optimizer: LogisticRegressionOptimizer): Unit = {
		require(xt != null && xt.size > 0, "Cannot compute the logistic regression of undefined time series")
		require(xt.size == labels.size, s"Size of input data ${xt.size} is different from size of labels ${labels.size}")
		require(optimizer != null, "Cannot execute a logistic regression with undefined optimizer")
   }
}


// --------------------------------------  EOF -------------------------------------------------------