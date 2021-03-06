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
package org.scalaml.ga

import scala.util.Random

import org.apache.log4j.Logger

import org.scalaml.core.design.PipeOperator
import org.scalaml.core.XTSeries
import org.scalaml.core.Types.ScalaMl.{DblVector, DblMatrix}
import org.scalaml.ga.state._
import org.scalaml.util.Display

import Chromosome._


		/**
		 * <p>Class to select the best solution or Chromosome from an initial population
		 * or genes pool using a set of policies for selection, mutation and crossover of
		 * chomosomes. The client code initialize the GA solver with either an initialized
		 * population or a function () => Population{T] that initialize the population. THe
		 * class has only one public method search.<br>
		 * Reference: http://www.kddresearch.org/Publications/Book-Chapters/Hs5.pdf</p>
		 * @constructor Create a generic GA-based solver. [state] Configuration parameters for the GA algorithm, [population] Initialized population of chromosomes (solution candidates)
		 * @param config  Configuration parameters for the GA algorithm
		 * @param score Scoring method for the chromosomes of this population
		 * @throws IllegalArgumenException if the configuration is undefined or the population is not initialized
		 * 
		 * @author Patrick Nicolas
		 * @since August 29, 2013
		 * @note Scala for Machine Learning Chapter 10 Genetic Algorithm
		 */
final protected class GASolver[T <: Gene](config: GAConfig, score: Chromosome[T] => Unit) 
			extends PipeOperator[Population[T], Population[T]] {

	require(config != null, "GASolver configuration  is undefined")
	require(score != null, "GASolver Cannot execute reproduction cycles with undefined scoring function")
   
	private var state: GAState = GA_NOT_RUNNING
	private val logger = Logger.getLogger("GASolvee")
	
		/**
		 * <p>Method to resolve any optimization problem using a function to generate
		 * a population of Chromosomes, instead an existing initialized population
		 * @param initialize Function to generate the chromosomes of a population
		 * @throws IllegalArgumenException If the initialization or chromosome generation function is undefined
		 */
	def |>(initialize: () => Population[T]): Population[T] = this.|>(initialize())
   
   
		/**
		 * <p>Uses the genetic algorithm reproduction cycle to select the fittest
		 * chromosomes (or solutions candidate) after a predefined number of reproduction cycles.</p>
		 * @throws MatchError if the population is emptry
		 * @return PartialFunction with a parameterized population as input and the population containing the fittest chromosomes as output.
		 */
	override def |> : PartialFunction[Population[T], Population[T]] = {	
		case population: Population[T] if(population.size > 1) => {
			val reproduction = Reproduction[T](score)
			state = GA_RUNNING
	          
			Range(0, config.maxCycles).find(n => {  		 
				reproduction.mate(population, config, n) match {
					case true => converge(population, n) != GA_RUNNING
					case false => {
						if(population.size == 0) 
							state = GA_FAILED(s"GASolver.PartialFunction reproduction failed after $n cycles") 
						else 
							state = GA_SUCCEED(s"GASolver.PartialFunction Completed in $n cycles")
						true
					}
				}
		   
			}) match {
				case Some(n) => population
				case None => state = GA_NO_CONVERGENCE(s"GASolver.PartialFunction Failed to converge"); population
			}
		}
	}
   

   
	private[this] def converge(population: Population[T], cycle: Int): GAState = {
		if(population == null) 
			GA_FAILED(s"GASolver.converge Reproduction failed at $cycle")
		else if(cycle >= config.maxCycles)
			GA_NO_CONVERGENCE(s"GASolver.converge Failed to converge at $cycle ")
		else
			GA_RUNNING
	}
}


		/**
		 * Object companion for the Solve that defines the two constructors
		 * @author Patrick Nicolas
		 * @since August 29, 2013
		 * @note Scala for Machine Learning Chapter 10 Genetic Algorithm
		 */
object GASolver {

		/**
		 * Default constructor for the Genetic Algorithm (class GASolver)
		 * @param config  Configuration parameters for the GA algorithm
		 * @param score Scoring method for the chromosomes of this population
		 */
	def apply[T <: Gene](config: GAConfig, score: Chromosome[T] =>Unit): GASolver[T] = 
		new GASolver[T](config, score)

		/**
		 * Constructor for the Genetic Algorithm (class GASolver) with undefined scoring function
		 * @param config  Configuration parameters for the GA algorithm
		 */
	def apply[T <: Gene](config: GAConfig): GASolver[T] = 
		new GASolver[T](config, null)
}

// ---------------------------  EOF -----------------------------------------------