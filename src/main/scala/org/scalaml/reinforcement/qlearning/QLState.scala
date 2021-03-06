/**
 * Copyright 2013, 2014, 2015  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package org.scalaml.reinforcement.qlearning


import scala.util.Random
import org.scalaml.core.Types.ScalaMl._

		/**
		* <p>State in the Q-learning. A state is uniquely defined by its identifier and the list of actions that transition
		* from this state to another state. The list of actions is empty if this state is a goal. A state may have properties of
		* type T that is independent from the state transition.<br>
		* <pre><span style="font-size:9pt;color: #351c75;font-family: &quot;Helvetica Neue&quot;,Arial,Helvetica,sans-serif;">
		* <b>id</b>        Identifier for the state.
		* <b>actions</b>   List of actions for that transition from this state to other states. Each action transition the model to single state.
		* <b>property</b>      Optional property of this state.
		* </span></pre></p>
		* @constructor Create a state for Q-learning. 
		* @throws IllegalArgument is list of actions is undefined or the id negative
		* @param id Identifier for the state.
		* @param actions List of actions for that transition from this state to other states. Each action transition the model to single state.
		* @param property Optional property of this state.
		* @author Patrick Nicolas
		* @since January 17, 2014
		* @note Scala for Machine Learning Chap 11 Reinforcement learning/Q-learning
		*/
protected class QLState[T](val id: Int, val actions: List[QLAction[T]] = List.empty, property: T) {
	import QLState._
	check(id, actions)
		/**
		 * Test if this state is a goal (or has not actions).
		 * @return true if the state has no actions, false otherwise
		 */
	@inline
	final def isGoal: Boolean = !actions.isEmpty
  
	override def toString: String = 
		new StringBuilder(s"state: $id ")
			.append( actions.foldLeft(new StringBuilder)((b,a) => b.append(s"$a ")).toString )
			.toString
}


		/**
		 * Companion object to the State class used for defining constructor and validating its parameters
		 * @author Patrick Nicolas
		 * @since January 17, 2014
		 * @note Scala for Machine Learning Chap 11 Reinforcement learning/Q-learning
		 */
object QLState {
  
		/**
		 * Default constructor for QLState
		 * @param id Identifier for the state.
		 * @param actions List of actions for that transition from this state to other states. Each action transition the model to single state.
		 * @param property Optional property of this state. 
		 */
	def apply[T](id: Int, actions: List[QLAction[T]], property: T): QLState[T] = new QLState(id, actions, property)
  
	protected def check[T](id: Int, actions: List[QLAction[T]]): Unit = {
		require( id >= 0, s"QLState.check id $id is out of range")
		require(actions != null, "QLState.check Cannot create a QLState with undefined list of actions")
	}
}



// ----------------------------  EOF --------------------------------------------------------------

