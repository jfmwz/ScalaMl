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
package org.scalaml.supervised.bayes.text

import org.scalaml.workflow.data.DocumentsSource

import org.scalaml.util.Counter
import scala.collection.mutable.HashMap
import scala.annotation.implicitNotFound
import scala.util.{Try, Success, Failure}
import org.scalaml.util.Display
import org.apache.log4j.Logger
import DocumentsSource._
import TermsScore._

		/**
		 * <p>Class to organize news articles as map of relative frequency of specific keywords extracted from those 
		 * articles. The tuple (keywords, rel. frequency) are ranked by their date. Dates are implemented 
		 * as <b>Long</b> for simplicity's sake.</p>
		 * @constructor Create a map of news articles, classified and ordered by their release dates. 
		 * @throws ImplicitNotFound exception is ordering is not defined prior instantiating this class
		 * @param order Implicit ordering instance used in ranking the articles by date.
		 * @author Patrick Nicolas
		 * @since February 16, 2014
		 * @note Scala for Machine Learning Chapter 5 Naive Bayes Models / Naive Bayes and text mining
		 */
@implicitNotFound("NewsArticle. Ordering not explicitly defined")
protected class NewsArticles[T <% Long](implicit order: Ordering[T]) {
	private[this] val articles = new HashMap[T, Map[String, Double]]
	
		/**
		 *  <p>Add a new map of weighted keywords for this specific dates.</p>
		 *  @param date Date as Long, associated to the map of tuples (Keywords, rel. frequency) to be added
		 *  @param weightedTerms map of tuples (Keywords, rel. frequency) extracted from a news article released on a specific date
		 *  @throws IllegalArgumentException if the data or the weighted terms map is undefined.
		 */	
	def += (date: T, weightedTerms: Map[String, Double]): Unit = {
		require( date != null, "NewsArticles.+= Incorrect date format")
		require(weightedTerms != null && weightedTerms.size > 0, "NewsArticles.+= Cannot update daily keywords with undefined document weighted words")
	   
		def merge(m1: Map[String, Double], m2: Map[String, Double]): Map[String, Double] = {
			(m1.keySet ++ m2.keySet).foldLeft(new HashMap[String, Double])( (m, x) => {
				var wt = 0.0
				if(m1.contains(x)) wt += m1(x)
				if(m2.contains(x)) wt += m2(x)

				m.put(x, wt) 
				m 
			}).toMap
		}
	   
		articles.put(date, if( articles.contains(date)) 
								merge(articles(date), weightedTerms) 
							else 
								weightedTerms )
	}
	
	
		/**
		* <p>Order the existing map of weighted map per their associated date</p>
		* @return Array of weighted terms ordered by their release date
		*/
	final def toOrderedArray: Array[(T, Map[String, Double])] = articles.toArray.sortWith( _._1 < _._1)
	
		/** 
		 *  <p>Generates a textual description of the labels and the weighted terms associated
		 *  to a specific date.</p>
		 */
	private def toString(labels: Iterable[String], el: (T, Map[String, Double])): String = {
		val idx = el._1.toString
		val buf = labels.foldLeft(new StringBuilder)((buf, lbl) => 
			buf.append( if( el._2.contains(lbl) ) el._2.get(lbl).get.toString else "0.0").append(",") )
			
		s"${idx.charAt(0)}/${idx.charAt(1)}${idx.charAt(2)},${buf.toString}"
	}
	
		
		/** 
		 *  <p>Generates a textual description of the labels (titles) and the weighted terms.</p>
		 *  @param labels List of labels (or news article title) and their associated weighted terms
		 *  @throws IllegalArgumentException if the labels are not defined
		 *  @return Textual representation of the NewsArticles ranked by their date
		 */
	def toString(labels: Iterable[String]): String = {
		require(labels != null && labels.size > 0, "NewsArticles.toString: labels for the news articles are undefined")
		import scala.collection.mutable.HashSet

		val mapped = labels.foldLeft(new HashSet[String])((set, lbl) => {set.add(lbl); set})

		val buf = new StringBuilder("id")
		buf.append(mapped.foldLeft(new StringBuilder)((buf, lbl) => 
			buf.append(s",$lbl")).toString).append("\n")
		buf.append(toOrderedArray.foldLeft(new StringBuilder)((buf, dk) => 
			buf.append(toString(mapped, dk)).append("\n")).toString)
		buf.toString
	}
}


		/**
		 * Companion object for the NewsArticle Container. This singleton is used
		 * to define the constructor of the class NewsArticle and generating a
		 * textual description of its content
		 * @author Patrick Nicolas
		 * @since February 16, 2014
		 * @note Scala for Machine Learning Chapter 5 Naive Bayes Models / Naive Bayes and text mining
		 */
object NewsArticles {
		
		/**
		 * Default constructor for the class NewsArticles
		 * @param date Date as Long, associated to the map of tuples (Keywords, rel. frequency) to be added
		 * @param weightedTerms map of tuples (Keywords, rel. frequency) extracted from a news article released on a specific date 
		 */
	def apply[T <% Long](implicit order: Ordering[T]): NewsArticles[T] = new NewsArticles[T]

		/**
		 * Function that create a textual representation of weighted terms frequency 
		 * @param arr Array of tuples (date, Map(term, weights)) for all documents associated to a specific date of type T
		 */
	def toString[T](arr: Array[(T, Map[String, Double])]): String =  {
		val buf = new StringBuilder
		arr.foreach(x => { 
			val weightedTerms = x._2.foldLeft(new StringBuilder)((b, wt) => b.append(s"(${wt._1},${wt._2}) ")).toString
			buf.append(s"${x._1.toString} => ${weightedTerms}\n" )
		})
		buf.toString
	}
}

// ----------------------------  EOF ------------------------------------------