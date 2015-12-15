/* Copyright (C) 2015 University of Massachusetts Amherst.
   This file is part of "fuse_ttl"
   https://github.com/iesl/fuse_ttl
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */




package edu.umass.cs

import cc.factorie.util.CmdOptions
import cc.factorie.variable.CategoricalDomain
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable
import java.io.{FileInputStream, FileWriter, BufferedWriter}
import scala.util.control.Breaks._
import cc.factorie.app.strings.{Stopwords, StringSegmenter, alphaSegmenter}

/**
 * Evaluation metric for technical terms
 * Entropy of term is calculated on the distribution over the term contexts' frequencies.
 * A term context is all the terms which occur in the same document as the term.
 */


class entropyEvalOpts extends CmdOptions{
  val docFile =  new CmdOption("doc-file","", "FILENAME...", "Input documents with which terms have to be evaluated")
  val termFile = new CmdOption("term-file","", "FILENAME...", "File which contains list of terms one per line")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File which contains entropy values for each terms")
  val alpha = new CmdOption("alpha", 0.01, "DOUBLE", "smoothing constant for laplace smoothing")
}


object entropyEval {
  class TermStats {
    // Technical  terms
    val wordDomain = new CategoricalDomain[String]
    val wordDocMapping = new ArrayBuffer[mutable.BitSet]()
    val termsFoundInCorpus = mutable.HashSet[Int]()
    // Technical , Global - Freq
    val termFreq = mutable.HashMap[Int, mutable.HashMap[Int, Int]]()
  }

  val segmenter = new cc.factorie.app.strings.RegexSegmenter("\\p{Alpha}+".r)



  /**
   * Calculate entropy of terms based on contexts provided by the documents in which they occur
   * @param stats
   * @param vocab
   * @param domainSize
   * @param alpha
   * @param entropyFile
   */

  def calculateEntropy(stats: TermStats,vocab:CategoricalDomain[String],domainSize:Int,alpha:Double,entropyFile:String) = {
    val outputFile = new BufferedWriter(new FileWriter(entropyFile))
    val entropies = new mutable.HashMap[String, (Double)]()
    val vocabSize = domainSize - stats.termsFoundInCorpus.size
    val alphaV = alpha * vocabSize
    for ((term, mapVal) <- stats.termFreq) {
      val arrayB = new Array[Double](mapVal.size)
      var counter = 0
      val iter = mapVal.valuesIterator
      while (iter.hasNext) {
        val freq = iter.next()

        arrayB(counter) = freq
        counter += 1
      }
      val sum = arrayB.sum
      var maxIndex = 0
      var maxVal = 0.0
      val den =  sum + alphaV
      for (i <- 0 until arrayB.size) {
        if (arrayB(i) > maxVal) {
          maxVal = arrayB(i)
          maxIndex = i
        }

        val num = arrayB(i) + alpha
        val p = num / den
        arrayB(i) = p * Math.log(p)

      }
      var distSum =  arrayB.sum
      for(j<- 0 until (vocabSize-arrayB.size)){
        distSum += (alpha / den) * Math.log(alpha / den)
      }

      entropies put(stats.wordDomain.category(term), (- distSum / Math.log(2)))
    }
    println("Writing entropy values for technical terms ")
    entropies.toSeq.sortBy(_._2).foreach{e =>
      outputFile.write(e._1+"\t"+e._2+"\n")

    }
    outputFile.close()
  }





  def main(args: Array[String]) {
    val opts = new entropyEvalOpts
    opts.parse(args)
    val docFileName = opts.docFile.value
    val terms = new TermStats()
    val domain = new CategoricalDomain[String]()

    var t1 =  System.currentTimeMillis
    readTerms(opts.termFile.value, terms)

    val docDomain = new CategoricalDomain[String]
    var count=0
    val docFile = io.Source.fromInputStream(new FileInputStream(docFileName), "UTF-8").getLines
    breakable {
      while (docFile.hasNext) {
        val line = docFile.next.stripLineEnd
        val id = docDomain.index(count.toString)
        index(domain, terms, line, alphaSegmenter, id)
        count+=1
      }
    }

    docDomain.freeze()

    println("No of Technical Terms :"+terms.wordDomain.size)
    println("No of Technical terms found :"+terms.termsFoundInCorpus.size)

    println("Starting calc")
    // entropy calculation here
    val entropyFile = opts.outputFile.value
    val alpha = opts.alpha.value.toDouble
    calculateEntropy(terms, domain,domain.size,alpha,entropyFile)
    val t2 =  System.currentTimeMillis
    println("Time taken for entropy calculation "+(t2-t1)/1000.0 +" seconds")

  }


  /**
   * Read technical terms and add it to word domain where each term is stored as integer
   * @param fileName
   * @param terms
   */
  def readTerms(fileName: String, terms: TermStats): Unit = {
    for (line <- io.Source.fromInputStream(new FileInputStream(fileName), "UTF-8").getLines if line.trim.length != 0) {

      terms.wordDomain.index(line.stripLineEnd)
      terms.wordDocMapping += new mutable.BitSet()
    }

    terms.wordDomain.freeze()
  }



  /**
   * Construct necessary data structures
   * @param vocab
   * @param termStats
   * @param doc
   * @param segmenter
   * @param docIndex
   */
  def index(vocab: CategoricalDomain[String], termStats: TermStats, doc: String, segmenter: StringSegmenter = alphaSegmenter, docIndex: Int): Unit = {

    val localCount = new mutable.HashMap[Int, Int]()
    val tempBitSet = new mutable.BitSet()

    for (word <- doc.split(" ")) {
      //val w = word.toLowerCase
      val w = word
      if (!Stopwords.contains(w)) {
        val index = vocab.index(w)
        if (localCount.contains(index))
          localCount(index) += 1
        else
          localCount put(index, 1)
        val termIndex = termStats.wordDomain.value(w)

        if (termIndex != null && termIndex.intValue >= 0) {
          termStats.termsFoundInCorpus += termIndex.intValue
          tempBitSet += termIndex.intValue
          val docMapping = termStats.wordDocMapping(termIndex.intValue)

          if (docMapping != None) {
            docMapping += docIndex
          } else {
            val bitset = new mutable.BitSet()
            bitset.add(docIndex)
            termStats.wordDocMapping(termIndex.intValue) = bitset
          }
        }
      }

    }

    if (tempBitSet.size != 0) {
      localCount.foreach(indexVal => {
        tempBitSet.foreach(termIndex => {
          if (!vocab.category(indexVal._1).equals(termStats.wordDomain.category(termIndex)))
          {
            if (termStats.termFreq.get(termIndex) == None) {
              termStats.termFreq.put(termIndex, new mutable.HashMap[Int, Int]())
            }

            val map = termStats.termFreq.get(termIndex).get
            if (map.contains(indexVal._1))
              map(indexVal._1) += indexVal._2
            else {
              map put(indexVal._1, indexVal._2)
            }
          }
        })

      })
    }
  }
}
