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
package org.scalaml.supervised.svm

import libsvm._
import org.scalaml.core.design.Config
import org.scalaml.supervised.svm.kernel.SVMKernel
import org.scalaml.supervised.svm.formulation.SVMFormulation


		/**
		 * <p>Generic configuration item for support vector machine.</p>
		 * 
		 * @author Patrick Nicolas
		 * @since April 28, 2014
		 * @note Scala for Machine Learning Chapter 8 Kernel models and support vector machines
		 */
trait SVMConfigItem {
		/**
		 * <p>Update the LIBSVM configuration parameter.</p>
		 * @param param LIBSVM parameter to update.
		 */
	 def update(param: svm_parameter): Unit
}



		/**
		 * <p>Generic configuration manager for any category of SVM algorithm. The configuration of a SVM has
		 * three elements: SVM formulation, Kernel function and the execution parameters.</p>
		 * @constructor Create a configuration for this SVM with a given formulation, kernel function and execution parameters.
		 * @see LIBSVM
		 * @throws IllegalArgumentException if the formulation, kernel or execution parameter are undefined.
		 * @param formulation Formulation of the SVM problem (type and parameters of the formulation of the SVM algorithm)
		 * @param kernel Kernel function used for non-separable training sets (type and parameter(s) of the Kernel function used for non-linear problems
		 * @param exec Execution parameters for the training of the SVM model.
		 * 
		 * @author Patrick Nicolas
		 * @since April 30, 2014
		 * @note Scala for Machine Learning Chapter 8 Kernel models and support vector machines.
		 */
final protected class SVMConfig(formulation: SVMFormulation, kernel: SVMKernel, exec: SVMExecution) extends Config {
	import SVMConfig._
	
	check(formulation, kernel, exec)
	
		/**
		 * Name of the file that persists the configuration of the support vector machine algorithm
		 */
	protected val persists = "config/svm"

		/**
		 * Configuration parameters set used in LIBSVM
		 */
	val  param = new svm_parameter
	formulation.update(param)
	kernel.update(param)
	exec.update(param)
    	
	override def toString: String = {
		val buf = new StringBuilder
		buf.append(s"\nSVM Formulation: ${formulation.toString}\n${kernel.toString}")
    	             
		if( param.weight != null) {
			buf.append("\nweights: ")
			for( w <- param.weight)
				buf.append(s"$w,")
		}
		buf.toString
	}

		/**
		 * Retrieve the convergence criteria of SVMExecution class
		 * @return convergence criteria
		 */
	@inline 
	final def eps: Double = exec.eps
    
		/**
		 * Test if SVM is configured for cross validation
		 * @return true if number of folds > 0, false otherwise
		 */
	@inline 
	final def isCrossValidation: Boolean = exec.nFolds > 0

		/**
		 * <p>Retrieve the number of folds used in the cross-validation
		 * @return Number of folds used in the cross-validation
		 */
	@inline 
	final def nFolds: Int = exec.nFolds
}



		/**
		 * <p>Companion object for SVM configuration manager used for defining the constructors of SVMConfig class.</p>
		 * @author Patrick Nicolas
		 * @since April 30, 2014
		 * @note Scala for Machine Learning Chapter 8 Kernel models and support vector machines.
		 */
object SVMConfig {
	import SVMExecution._
	
		/**
		 * Default constructor for the configuration of the support vector machine
		 * @param formulation Formulation of the SVM problem (type and parameters of the formulation of the SVM algorithm)
		 * @param kernel Kernel function used for non-separable training sets (type and parameter(s) of the Kernel function used for non-linear problems
		 * @param exec Execution parameters for the training of the SVM model.
		 */
	def apply(svmType: SVMFormulation, kernel: SVMKernel, svmParams: SVMExecution): SVMConfig = 
		new SVMConfig(svmType, kernel, svmParams)

		/**
		 * Constructor for the configuration of the support vector machine with a predefined execution parameters
		 * @param formulation Formulation of the SVM problem (type and parameters of the formulation of the SVM algorithm)
		 * @param kernel Kernel function used for non-separable training sets (type and parameter(s) of the Kernel function used for non-linear problems
		 */
	def apply(svmType: SVMFormulation, kernel: SVMKernel): SVMConfig = 
		new SVMConfig(svmType, kernel, SVMExecution.apply)
   
    private def check(formulation: SVMFormulation, kernel: SVMKernel, svmParams: SVMExecution): Unit =  {
		require(formulation != null, "Formulation in the configuration of SVM is undefined")
		require(kernel != null, "Kernel function in the configuration of SVM is undefined")
		require(svmParams != null, "The training execution parameters in the configuration of SVM is undefined")	
	}
}


// --------------------------- EOF ------------------------------------------