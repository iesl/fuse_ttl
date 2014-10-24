package main.scala.edu.umass.cs

import java.io._

import cc.factorie.app.strings.{Stopwords, alphaSegmenter, StringSegmenter}
import cc.factorie.la.{SparseIndexedTensor1, DenseTensor1}
import cc.factorie.util.{DoubleSeq, ArrayDoubleSeq}
import cc.factorie.variable.{CategoricalValue, CategoricalDomain, CategoricalSeqDomain, CategoricalSeqVariable}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._
import scala.io.Source

/**
 * Created by A on 9/27/2014.
 */
object Evaluation {


  class TermStats {
    // Technical
    val wordDomain = new CategoricalDomain[String]
    val wordDocMapping = new ArrayBuffer[mutable.BitSet]()
    // Technical , Global - Freq
    val termFreq = mutable.HashMap[Int, mutable.HashMap[Int, Int]]()
  }

  val segmenter = new cc.factorie.app.strings.RegexSegmenter("\\p{Alpha}+".r)

  def calculateEntropy(stats: TermStats,vocab:CategoricalDomain[String],entropyFile:String) = {
    val outputFile = new BufferedWriter(new FileWriter(entropyFile))
    val entropies = new mutable.HashMap[String, (Double)]()
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
      for (i <- 0 until arrayB.size) {
        if (arrayB(i) > maxVal) {
          maxVal = arrayB(i)
          maxIndex = i
        }
        arrayB(i) = arrayB(i) / sum * Math.log(arrayB(i) / sum)
      }
      //entropies put(stats.wordDomain.category(term), (-arrayB.sum / Math.log(2), vocab.category(maxIndex)))
      entropies put(stats.wordDomain.category(term), (-arrayB.sum / Math.log(2)))
    }
    println("Entropy values for technical terms ")
    entropies.toSeq.sortBy(_._2).foreach{e =>
      outputFile.write(e._1+"\t"+e._2+"\n")

    }
    outputFile.close()
  }

  def calculateDistributions(stats: TermStats, domain: CategoricalDomain[String]): mutable.HashMap[Int, SparseIndexedTensor1] = {

    val dist = new mutable.HashMap[Int, SparseIndexedTensor1]()
    for ((term, mapVal) <- stats.termFreq) {

      val tensor = new SparseIndexedTensor1(domain.size)

      val iter = mapVal.iterator
      while (iter.hasNext) {
        val (key, value) = iter.next()
        tensor(key) = value
      }
      val sum = tensor.sum
      for (i <- 0 until tensor.size) {
        tensor(i) = (tensor(i) + 0.1) / (sum + domain.size / 10)
      }
      dist put(term, tensor)
    }
    dist
  }

  def calculateJSDistance(distributions: mutable.HashMap[Int, SparseIndexedTensor1],stats:TermStats): Seq[((String, String), Double)] = {
    val keys = distributions.keySet.toList
    val jsDist = new mutable.HashMap[(String, String), Double]()
    for (i <- 0 until keys.size) {
      val dist = distributions(keys(i))
      for (j <- i + 1 until keys.size) {

        jsDist put((stats.wordDomain.category(keys(i)), stats.wordDomain.category(keys(j))), dist.jsDivergence(distributions(keys(j))))

      }
    }

    jsDist.toSeq.sortBy(_._2)
  }


  def calculateTermEntropy(stats: TermStats, vocab: CategoricalDomain[String]): Seq[(String, Double)] = {

    val entropies = new mutable.HashMap[String, (Double)]()
    for ((term, mapVal) <- stats.termFreq) {
      val arrayB = new ArrayBuffer[Double]
      var filterVal = mapVal.filterKeys((key) => {
        stats.wordDomain.value(vocab.category(key)) != null
      }).valuesIterator
      while (filterVal.hasNext) {
        arrayB += filterVal.next()
      }
      val sum = arrayB.sum
      var maxIndex = 0
      var maxVal = 0.0
      for (i <- 0 until arrayB.size) {
        if (arrayB(i) > maxVal) {
          maxVal = arrayB(i)
          maxIndex = i
        }
        arrayB(i) = arrayB(i) / sum * Math.log(arrayB(i) / sum)
      }
      entropies put(stats.wordDomain.category(term), (-arrayB.sum / Math.log(2)))
    }
    println(entropies.size)
    entropies.toSeq.sortBy(_._2)
  }

  def main(args: Array[String]) {
    val docFileName = args(0)
    val terms = new TermStats()
    val domain = new CategoricalDomain[String]()
    readTerms(args(1), terms)

    val docDomain = new CategoricalDomain[String]
    var count=0
    val docFile = io.Source.fromInputStream(new FileInputStream(docFileName), "UTF-8").getLines
    breakable {
      while (docFile.hasNext) {
        val line = docFile.next
        val id = docDomain.index(count.toString)
        index(domain, terms, line, alphaSegmenter, id)
        count+=1

      }
    }
    val docCount = args(2).toInt
    val termCount = args(3).toInt
    val entropyFile = args(4)
    /*breakable {
      for (file <- new File(directory).listFiles; if file.isFile) {
        val id = docDomain.index(file.getName)
        index(domain, terms, Source.fromFile(file).mkString, alphaSegmenter, id)
        if (docDomain.size > 100000) break()
      }
    } */
    docDomain.freeze()
    domain.freeze()
    //println("Doc List : "+docCoverage(terms, docCount, docDomain).mkString(","));
    println("No of Technical Terms :"+terms.wordDomain.size)
    println("No of documents "+docDomain.size)
    docCoverage(terms, docCount, docDomain)
    println()
    //println("Term Coverage ie % of Technical terms which occur in at least "+termCount+ " documents : "+termCoverage(terms, termCount)/(terms.wordDomain.size).toFloat)
    println()
    //println("Terms Frequency :"+terms.termFreq.size)
    //calculateEntropy(terms, domain,entropyFile)
    println()
    //println("Entropy for technical terms: "+calculateTermEntropy(terms, domain))
    /*val dist = calculateDistributions(terms, domain)
    println("JS Distance : "+calculateJSDistance(dist,terms)) */
  }


  /**
   * Read technical terms
   * @param fileName
   * @param terms
   */
  def readTerms(fileName: String, terms: TermStats): Unit = {
    for (line <- Source.fromFile(fileName).getLines() if line.trim.length != 0) {
      terms.wordDomain.index(line)
      terms.wordDocMapping += new mutable.BitSet()
    }
    terms.wordDomain.freeze()
  }

  /**
   * Get list of all docs with coverage with higher than minNumTerms
   * @param vocab
   * @param minNumTerms
   * @param docDomain
   * @return
   */
  def docCoverage(vocab: TermStats, minNumTerms: Int, docDomain: CategoricalDomain[String]) {
    var docs = new ArrayBuffer[String]()

    for (docID <- 0 until docDomain.size) {
      var counter = 0
      breakable {
        vocab.wordDomain.foreach((catDomain) => {

          if (vocab.wordDocMapping(catDomain.intValue).contains(docID))
            counter += 1

          if (counter >= minNumTerms)
            break()
        })
      }

      if (counter >= minNumTerms) {
        docs += docDomain.category(docID);
      }

    }
    println("No of documents which have at least "+minNumTerms+" input technical terms is "+docs.size)
    println("Percent of documents which have at least "+minNumTerms+" input technical terms is "+ docs.size/docDomain.size.toFloat)
    //docs
  }

  def termCoverage(stats: TermStats, minNumDocs: Int): Int = {
    var termsCov = new ArrayBuffer[String]()
    stats.wordDomain.foreach((term) => {

      if (stats.wordDocMapping(term.intValue).size > minNumDocs) {
        termsCov += stats.wordDomain.categories(term.intValue)
      }
    })
    termsCov.size
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
      val w = word.toLowerCase

      if (!Stopwords.contains(w)) {
        val index = vocab.index(w)
        if (localCount.contains(index))
          localCount(index) += 1
        else
          localCount put(index, 1)
        val termIndex = termStats.wordDomain.value(w)
        if (termIndex != null && termIndex.intValue >= 0) {
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
            //println((termStats.termFreq.get(termIndex)))
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
