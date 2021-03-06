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
import scala.annotation.implicitNotFound

import Gene._
import Chromosome._


		/**
		 * <p>Class that implements a parameterized chromosome using an encoding scheme and
		 * an objective/fitness function. A chromosome is a container or list of Gene that
		 * represents candidate solution to a problem or candidate model to a dataset.<br>
		 * This particular implementation score the chromosome unfitness. The fitness value of
		 * a chromosome is randomly selected as a high value.</p>
		 * @constructor Create a chromosome with the parameterized sbutype of Gene. [code]: Code genetic code or list of Gene that is to be encoded with 0,1 bits
		 * @throws IllegalArgumentException if the genetic code is undefined or empty
		 * @param code List of Genes or sub types composing this chromosomes.
		 * @author Patrick Nicolas
		 * @since August 27, 2013
		 * @note Scala for Machine Learning Chapter 10 Genetic Algorithm / Genetic algorithm components
		 */
final class Chromosome[T <: Gene](val code: List[T]) {  
	require(code != null && code.size > 1, "Chromosome Cannot create a chromosome from undefined genetic code")
	var unfitness: Double = 1000*(1.0 + Random.nextDouble)
   
   
		/**
		 * <p>Define the cross-over operator to be applied on this chromosome. The cross-over
		 * is hierarchical. The algorithm selects the gene associated to the cross-over index, 
		 * swap all the genes of higher index (below or after the cross-over gene) between
		 * the two parents and finally swap the bits within the cross-over gene.<br>
		 * The cross over operation generates two off springs from the two original parents. The off-springs
		 * are added to the current population along with the parents.</p>
		 * @param that other parent chromosome
		 * @param gIdx Genetic index for the cross-over.
		 * @throws IllegalArgumentException if the other chromosome is undefined, or have a different size 
		 * or if the cross-over factor is out of range.
		 * @return the pair of offspring chromosomes
		 */
	def +- (that: Chromosome[T], gIdx: GeneticIndices): (Chromosome[T], Chromosome[T]) = {
		require(that != null, "Chromosome.+- Cannot cross-over this chromosome with an undefined parent")
		require(this.size == that.size, s"Chromosome.+- Cannot cross-over chromosomes of different size this ${size} and that ${that.size}")
     
			// First use the global index (module the number of gene
		val xoverIdx = gIdx.chOpIdx
		val xGenes =  spliceGene(gIdx, that.code(xoverIdx) ) 

		val offSprng1 = code.slice(0, xoverIdx) ::: xGenes._1 :: that.code.drop(xoverIdx+1)
		val offSprng2 = that.code.slice(0, xoverIdx) ::: xGenes._2 :: code.drop(xoverIdx+1)
		(Chromosome[T](offSprng1), Chromosome[T](offSprng2))
	}
   
   		
		/**
		 * <p>Mutation operator that flip a gene selected through a mutation index.
		 * The mutated gene is added to the population (gene pool).</p>
		 * @param gIdx Genetic index
		 * @throws IllegalArgumentException if mu is out of range
		 * @return A new mutated chromosome
		 */
	def ^ (gIdx: GeneticIndices): Chromosome[T] = {
			// Get the mutation index in the gene to mutate, chOpIdx
		val mutated = code(gIdx.chOpIdx) ^ gIdx
		val xs = Range(0, code.size).map(i => 
			if(i == gIdx.chOpIdx) mutated.asInstanceOf[T] else code(i)
		).toList
		Chromosome[T](xs)
	}
     
     
		/**
		 * <p>Normalize the fitness of this chromosome with a factor. This 
		 * operation is required by the selection algorithm.</p>
		 * @param normalizedFactor normalization factor
		 * @throws IllegalArgumentException if the normalization factor is less than EPS
		 */
	def /= (normalizeFactor: Double): Unit = {
		require( Math.abs(normalizeFactor) > Chromosome.EPS, s"Chromosome./= Cannot normalize with $normalizeFactor > ${Chromosome.EPS}")
		unfitness /= normalizeFactor
	}

		/**
		 * <p>Decode this chromosome by applying a type conversion from Gene to T</p>
		 * @param d implicit conversion of Gene to the parameterized type T which is a sub-class of Gene
		 * @throws ImplicitNotFoundException if the implicit conversion d is undefined.
		 */
	@implicitNotFound("Chromosome.decode Conversion from Gene to parameterized type undefined in decoding chromosome")
	def decode(implicit d: Gene => T): List[T] = code.map( d(_)) 

		/**
		 * Clone this chromosome by duplicating its genes
		 * @return Duplicate of this chromosome
		 */
	override def clone: Chromosome[T] = Chromosome[T](code)
    
		/**
		 * <p>Returns the size of this chromosome as the number of genes it contains,.</p>
		 * @return Number of genes
		 */
	@inline
	final def size: Int = code.size
    
		/**
		 * <p>Stringize the genetic code of this chromosome</p>
		 * @return Genetic code {0, 1} for this chromosome
		 */
	override def toString: String = String.valueOf(code.toString)
     
	
		/**
		 * Symbolic representation of the chromosome as a sequence of symbolic representation
		 * of the genes it contains
		 * @param comment Optional comment for the symbolic representation
		 * @return sequence of symbolic representation of the genes of this chromosomes
		 */
	final def symbolic(comment: String = ""): String = 
		new StringBuilder(comment)
			.append(code.foldLeft(new StringBuilder)((buf,gene) => buf.append(s"${gene.symbolic} ")).toString)
			.append(s" score: $unfitness").toString
    
	private[this] def spliceGene(gIdx: GeneticIndices, thatCode: T): (T, T) = {
		((this.code(gIdx.chOpIdx) +- (thatCode, gIdx)).asInstanceOf[T], 
		(thatCode +- (code(gIdx.chOpIdx), gIdx)).asInstanceOf[T] )
	}
}


		/**
		 * Companion object to a Chromosome used to define the constructors
		 * @author Patrick Nicolas
		 * @since September 2, 2013
		 * @note Scala for Machine Learning Chapter 10 Genetic Algorithm/Genetic algorithm components
		 */
object Chromosome {
	import scala.collection.mutable.ArrayBuffer
	import java.util.BitSet
  
	private val EPS = 1e-10

		/**
		 * Default (Generic code) constructor for the Chromosome
		 * @param code List of Genes or sub types composing this chromosomes.
		 */
	def apply[T <: Gene](code: List[T]): Chromosome[T] = new Chromosome[T](code)
	
		/**
		 * Symbolic constructor for the Chromosome
		 * @param predicates List of predicates of type T for this chromosome
		 * @param encode Function that convert a predicate to a Gene
		 * @throws IllegalArgumentException if either the predicates are undefined or the encoding function is undefined
		 */
	def apply[T <: Gene](predicates: List[T], encode: T => Gene): Chromosome[T] = {
		require(predicates != null && predicates.size > 0, "Chromosome.apply List of predicates is undefined")
		require(encode != null, "Chromosome.apply Encoding function is undefined")

		new Chromosome[T](if(predicates.size == 1) 
			List[T](encode(predicates(0)).asInstanceOf[T])
		else 
			predicates.foldLeft(List[T]()) ((xs, t) => encode(t).asInstanceOf[T] :: xs))
	}
			
		/**
		 * Type for the pool of chromosomes. A Pool of chromosomes is an arbitrary
		 * array of chromosomes.
		 */
	type Pool[T <: Gene] = ArrayBuffer[Chromosome[T]]
}

// ------------------------  EOF --------------------------------------