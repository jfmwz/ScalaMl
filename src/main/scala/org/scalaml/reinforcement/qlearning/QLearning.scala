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
package org.scalaml.reinforcement.qlearning

import org.apache.log4j.Logger

import org.scalaml.util.{Matrix, Display}
import org.scalaml.core.Types.ScalaMl._
import org.scalaml.core.design.{Config, PipeOperator, Model}


		/**
		 * <p>Define a model for the Q-learning algorithm as the tuple <optimum policy, training epoch coverage>.</p>
		 * @constructor Model created during training of Q-learning. 
		 * @param bestPolicy Best policy computed or estimated during training.
		 * @param coverage  Ratio of training trial or epochs that reach a predefined goal state.
		 * @see org.scalaml.core.design.Model
		 * 
		 * @author Patrick Nicolas
		 * @since January 22, 2014
		 * @note Scala for Machine Learning Chap 11 Reinforcement learning/Q-learning
		 */
final class QLModel[T](val bestPolicy: QLPolicy[T], val coverage: Double)  extends Model {
	
		/**
		 * Name of the file that persists the model for Q-learning
		 */
	protected val persists = "model/qlearning"
	override def toString: String = s"Optimal policy: ${bestPolicy.toString}\nTraining coverage: $coverage" 
}

		/**
		 * <p>Generic parameterized class that implements the QLearning algorithm. The Q-Learning
		 * model is initialized and trained during the instantiation of the class so it can be 
		 * in the correct state for the run-time prediction. Therefore the class instances have only
		 * two states successfully trained and failed training.<br>
		 * The implementation does not assume that every episode (or training cycle) is successful. 
		 * At completion of the training, the ratio of labels over initial training set is computed.
		 * The client code is responsible to evaluate the quality of the model by testing the ratio
		 * gamma a threshold.
		 * <pre><span style="font-size:9pt;color: #351c75;font-family: &quot;Helvetica Neue&quot;,Arial,Helvetica,sans-serif;">
		 *  Q-Learning value action formula: Q'(t) = Q(t) + alpha.[r(t+1) + gamma.max{Q(t+1)} - Q(t)</span></pre></p>
		 * @constructor Create a Q-learning algorithm. [config] Configuration for Q-Learning algorithm. [qlSpace] Initial search space. [qlPolicy] Initial set of policies.
		 * @throws IllegalArgumentException if the configuration, the search space or the initial policies are undefined
		 * @param config Configuration for the Q-learning algorithm
		 * @param qlSpace Initial search space of states
		 * @param qlPolicy Initial policy for the search
		 * @seep org.scalaml.core.design.PipeOperator
		 * 
		 * @author Patrick Nicolas
		 * @since January 22, 2014
		 * @note Scala for Machine Learning Chap 11 Reinforcement learning / Q-learning
		 */
final class QLearning[T](config: QLConfig, qlSpace: QLSpace[T], qlPolicy: QLPolicy[T]) 
								extends PipeOperator[QLState[T], QLState[T]]  {

	import scala.collection.mutable.ArrayBuffer
	import scala.util.{Random, Try, Success, Failure}
	import QLearning._
	
	private val logger = Logger.getLogger("QLearning")
	check(config, qlSpace, qlPolicy)
   
		/**
		 * Model parameters for Q-learning of type QLModel that is defined as
		 * a list of policies and training coverage 
		 */
	val model: Option[QLModel[T]] = {
		val r = new Random(System.currentTimeMillis)
		Try {
			val completions = Range(0, config.numEpisodes).foldLeft(0)((s, n) => {
				val completed = train(r)
		
				Display.show(s"Episode # $n completed: $completed", logger)
				s + (if(completed) 1 else 0)
			})
			val coverage = completions.toDouble/config.numEpisodes

			if( coverage >= config.minCoverage )
				Some(new QLModel[T](qlPolicy, coverage))
			else 
				None
		}
		match {
			case Success(qModel) => qModel
			case Failure(e) => Display.none("QLearning.model could not be created", logger, e)
		}
	}

		/**
		 * <p>Prediction method using Q-Learning model. The model is automatically 
		 * created (or trained) during instantiation of the class. It overrides the
		 * data transformation method (PipeOperator) with a transformation of a state to a predicted goal state.</p>
		 * @throws MatchError if the model is undefined or the input state is undefined 
		 * @return PartialFunction of state of type QLState[T] as input to a predicted goal state of type QLState[T]
		 */
	override def |> : PartialFunction[QLState[T], QLState[T]] = {
		case state: QLState[T] if(state != null && state.isGoal && model != None) => 
			nextState(state, 0)._1
	}

	override def toString: String = qlPolicy.toString + qlSpace.toString


	@scala.annotation.tailrec
	private def nextState(st: (QLState[T], Int)): (QLState[T], Int) =  {
		val states = qlSpace.nextStates(st._1)
		if( states.isEmpty || st._2 >= config.episodeLength) 
			st
		else
			nextState( (states.maxBy(s => model.get.bestPolicy.R(st._1.id, s.id)), st._2+1))
	}

	
	private[this] def train(r: Random): Boolean = {

		@scala.annotation.tailrec
		def search(st: (QLState[T], Int)): (QLState[T], Int) = {
			val states = qlSpace.nextStates(st._1)
			
			if( states.isEmpty || st._2 >= config.episodeLength ) 
				(st._1, -1)		
			else {
				val state = states.maxBy(s => qlPolicy.R(st._1.id, s.id) )
					
				if( qlSpace.isGoal(state) )
					(state, st._2)
				else {
					val r = qlPolicy.R(st._1.id, state.id)   
					val q = qlPolicy.Q(st._1.id, state.id)

					val nq = q + config.alpha*(r + config.gamma*qlSpace.maxQ(state, qlPolicy) - q)
					qlPolicy.setQ(st._1.id, state.id,  nq)
					search((state, st._2+1))
				}
			}
		}	
        
		r.setSeed(System.currentTimeMillis*Random.nextInt)
		val finalState = search((qlSpace.init(r), 0))
		if( finalState._2 == -1) 
			false
		else
			qlSpace.isGoal(finalState._1)
	}
  }


		/**
		 * <p>Input to the Q-learning search space (QLSpace) and policy (QLPolicy).</p>
		 * @constructor Create an action input to Q-learning
		 * @param from Identifier for the source state
		 * @param to Identifier for the target or destination state
		 * @param reward reward (credit or penalty) to transition from state with id <b>from</b> to the state with id <b>to</b>
		 * @param probability Probability to transition from state <b>from</b> to state <b>to</b>
		 * @author Patrick Nicolas
		 * @since January 22, 2014
		 * @note Scala for Machine Learning Chap 11 Reinforcement learning/Q-learning
		 */
final class QLInput(val from: Int, val to: Int, val reward: Double = 1.0, val probability: Double = 1.0)



		/**
		 * Companion object to the Q-Learning class used to define constructors 
		 * and validate their input parameters
		 * @author Patrick Nicolas
		 * @since January 22, 2014
		 * @note Scala for Machine Learning Chap 11 Reinforcement learning Q-learning
		 */
object QLearning {  
		/**
		 * Default constructor for Q-learning
		 * @param config Configuration for the Q-learning algorithm
		 * @param qlSpace Initial search space of states
		 * @param qlPolicy Initial policy for the search
		 */
	def apply[T](config: QLConfig, qlSpace: QLSpace[T], qlPolicy: QLPolicy[T]): QLearning[T] =
		new QLearning[T](config, qlSpace, qlPolicy)

		/** 
		 * Constructor of the Q-learning with a predefined number of states, index of goals,
		 * input (or initial) states and a features set.
		 * @param config Configuration for the Q-learning algorithm
		 * @param numStates Number of states in the model 
		 * @param goals array of indices of states which are selected as goal state
		 * @param input Input to the search space as tuple of rewards and probability for each action between states
		 * @param features Features set as a set of variables of type Array[Int] generated through discretization of signal (floating point values)
		 * @throws IllegalArgumentException if one of the parameters is undefined or improperly initialized
		 */
	def apply[T](config: QLConfig, numStates: Int, goals: Array[Int], input: Array[QLInput], features: Set[T]): QLearning[T] = {
		require(input != null && input.size > 0, "QLearning Cannot initialize with undefine input")
		require(numStates > 2, s"QLearning Cannot initialize with $numStates states")
		require(goals != null && goals.size > 0, "QLearning Cannot initialize with undefined goals")
		require(input != null && input.size > 0, "QLearning Cannot initialize with undefined input rewards")
		require(features != null && features.size > 0, "QLearning Cannot initialize with undefined features")
		
		new QLearning[T](config, QLSpace[T](numStates, goals, features, config.neighbors), new QLPolicy[T](numStates, input))
	}
   

		/** 
		 * Constructor of the Q-learning with a predefined number of states, index of the goal state,
		 * input (or initial) states and a features set.
		 * @param config Configuration for the Q-learning algorithm
		 * @param numStates Number of states in the model 
		 * @param goal Index of the goal state
		 * @param input Input to the search space
		 * @param features Features set as a set of variables of type Array[Int] generated through discretization of signal (floating point values)
		 */
	def apply[T](config: QLConfig, numStates: Int, goal: Int, input: Array[QLInput], features: Set[T]): QLearning[T] = 
		apply[T](config, numStates, Array[Int](goal), input, features)
  
	protected def check[T](config: QLConfig, qlSpace: QLSpace[T], qlPolicy: QLPolicy[T]): Unit = {
		require(config != null, "QLearning.check Cannot create a Q-Learning model with undefined configuration")
		require(qlSpace != null, "QLearning.check Cannot create a Q-Learning model with undefined state space")
		require(qlPolicy != null, "QLearning.check Cannot create a Q-Learning model with undefined policy")
	}
}


// ----------------------------  EOF --------------------------------------------------------------
