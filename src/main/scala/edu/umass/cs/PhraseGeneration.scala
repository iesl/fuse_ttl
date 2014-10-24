package main.scala.edu.umass.cs

import cc.factorie.app.nlp.Document
import cc.factorie.app.nlp.segment.DeterministicTokenizer
import cc.factorie.util.CmdOptions
import java.io.{BufferedWriter, FileWriter, File}
import scala.collection.mutable.ArrayBuffer

class PhraseGenerationOpts extends CmdOptions{
  val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which phrases have to be generated")
  val nThreads= new CmdOption("threads", 16, "INT", "use <int> threads")
  val minCount = new CmdOption("min-count", 5, "INT", "used to discard words that occur less than <int> times")
  val scoreThreshold =  new CmdOption("threshold",100, "INT", "discards phrases which have score less than <int>")
  val phraseFile =  new CmdOption("phrase-file","", "FILENAME...", "File to which phrases generated are written")
}


class BigramStatistics {
  val wordCounts = new collection.mutable.LinkedHashMap[Int, Int]()
  val bigramCounts = new collection.mutable.LinkedHashMap[(String,String),Int]()
  var totalTokens = 0

  def process(doc:String):Unit={
    val docSplit=doc.split(" ")
    val len = docSplit.length
    var finaltok=""
    docSplit.toList.sliding(2).foreach{pair=>
      totalTokens += 1
      wordCounts(pair(0).hashCode) = 1 + wordCounts.getOrElse(pair(0).hashCode, 0)

      if(len>1){
        bigramCounts((pair(0),pair(1))) = 1 + bigramCounts.getOrElse((pair(0),pair(1)), 0)
        finaltok=pair(1)
      }
    }

    if(len>1){
      totalTokens += 1
      wordCounts(finaltok.hashCode) = 1 + wordCounts.getOrElse(finaltok.hashCode, 0)
    }
  }

  def process(document: Document): Unit = {
    for (token <- document.tokens) {
      totalTokens += 1
      wordCounts(token.string.hashCode) = 1 + wordCounts.getOrElse(token.string.hashCode, 0)
      token.getPrev.foreach(prev => {
        bigramCounts((prev.string,token.string)) = 1 + bigramCounts.getOrElse((prev.string,token.string), 0)
      })
    }

  }
  def process(documents: Iterable[Document]): Unit = documents.foreach(process)

  def aggregateCounts(others: Iterable[BigramStatistics]): Unit = {

    for (other <- others) {
      for ((unigram,value) <- other.wordCounts) {
        wordCounts(unigram) = wordCounts.getOrElse(unigram, 0) + value
      }
      for ((bigram,value) <- other.bigramCounts) {
        bigramCounts(bigram) = bigramCounts.getOrElse(bigram, 0) + value
      }
      totalTokens += other.totalTokens
    }
  }

  def processParallel(documents: Iterable[Document], nThreads: Int = Runtime.getRuntime.availableProcessors()): Unit = {

    val others = new cc.factorie.util.ThreadLocal[BigramStatistics](new BigramStatistics)
    cc.factorie.util.Threading.parForeach(documents, nThreads) { doc =>
      others.get.process(doc)
    }
    aggregateCounts(others.instances)
  }

  def getLikelyPhrases(countThreshold: Int = 5, scoreThreshold: Double = 100.0): Seq[Seq[String]] = {

    val bigramPhrases = collection.mutable.LinkedHashSet[Seq[String]]()
    //val phraseStarts = collection.mutable.HashMap[String,ArrayBuffer[String]]()
    bigramCounts.foreach({ case ((prev,token),count) =>

      val pc = wordCounts(prev.hashCode)
      val pt = wordCounts(token.hashCode)
      if (pc > countThreshold && pt > countThreshold) {
        // Pointwise mutual information is defined as P(A,B) / P(A) P(B).
        // In this case P(A,B) = bigramCounts(A,B)/totalTokens ,
        // P(A) = wordCounts(A) / totalTokens, P(B) = wordCounts(B) / totalTokens
        // Hence we can write PMI = bigramCounts(A,B) * totalTokens / (wordCounts(A) * wordCounts(B))
        val score = totalTokens * (count.toDouble - countThreshold) / (pc * pt)

        if (score > scoreThreshold) {
          //println(score+" "+prev+" "+token)
          bigramPhrases += Seq(prev,token)
          //phraseStarts.getOrElseUpdate(prev, new ArrayBuffer[String]).append(token)
        }
      }
    })
    // now we should have all interesting bigrams. I'll make the assumption that
    // if A B and B C are interesting phrases then A B C is interesting without checking.
    /*val trigramPhrases = collection.mutable.HashSet[Seq[String]]()
    bigramPhrases.foreach({ case Seq(prev,token) =>
      phraseStarts.getOrElse(token, Seq()).foreach(last => trigramPhrases += Seq(prev, token, last))
    })
    bigramPhrases.toSeq ++ trigramPhrases.toSeq  */
    bigramPhrases.toSeq
  }

  def topMutualInformationBigrams(threshold: Int = 5): Seq[(String,String,Double)] = {
    bigramCounts.toSeq.filter(_._2 > threshold).map({ case ((prev,token),count) =>
      ((prev,token),totalTokens * count.toDouble / (wordCounts(prev.hashCode) * wordCounts(token.hashCode)))
    }).sortBy(-_._2).take(100).map({case ((prev,token),score) => (prev,token,score)})
  }
}




object PhraseGeneration {
  def main(args:Array[String]){
    val opts = new PhraseGenerationOpts
    opts.parse(args)


    val phraseBufferWriter = new BufferedWriter(new FileWriter(opts.phraseFile.value))


    val bStatistics = new BigramStatistics()

    var t1 =  System.currentTimeMillis
    var count=0

    val trainFile = scala.io.Source.fromFile(new File(opts.trainFile.value)).getLines()
    trainFile foreach{line=>
      //val doc = DeterministicTokenizer.process(new Document("test line is here"))
      //println(line)

      bStatistics.process(line)
      count+=1
      if(count % 10000 == 0){println("Count "+count)}
    }

    //println(bStatistics.bigramCounts)
    //println(bStatistics.wordCounts)
    println(bStatistics.totalTokens)

    bStatistics.getLikelyPhrases(opts.minCount.value,opts.scoreThreshold.value).foreach{phrase=>
      phraseBufferWriter.write(phrase.mkString(" "))
      phraseBufferWriter.newLine()


    }
    phraseBufferWriter.close()
    val t2 =  System.currentTimeMillis
    println("Time taken for phrase generation "+(t2-t1)/1000.0 +" seconds")

  }

}

