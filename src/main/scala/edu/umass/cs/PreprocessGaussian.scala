package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import cc.factorie.app.nlp.embeddings.VocabBuilder
import java.io.{File, FileWriter, BufferedWriter, FileInputStream}



class PreprocessGaussianOpts extends CmdOptions{
  val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which phrases have to be generated")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "Updated training file after removing stop words and low frequency words")
  val vocabSize = new CmdOption("max-vocab-size", 2e6.toInt, "INT", "Max Vocabulary Size. Default Value is 2M . Reduce to 200k or 500k is you learn embeddings on small-data-set")
  val vocabHashSize = new CmdOption("vocab-hash-size", 14.3e6.toInt, "INT", "Vocabulary hash size")
  val ignoreStopWords = new CmdOption("ignore-stopwords", 1, "BOOLEAN", "use <bool> to include or discard stopwords.")
  val minCount = new CmdOption("min-count", 1, "INT", "used to discard words that occur less than <int> times")
}


object PreprocessGaussian {
  def main(args:Array[String]){
    val opts = new PreprocessGaussianOpts
    opts.parse(args)
    val trainFile = io.Source.fromInputStream(new FileInputStream(opts.trainFile.value), "UTF-8").getLines
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    val vocab = new VocabBuilder(opts.vocabHashSize.value)
    while (trainFile.hasNext) {
      val line = trainFile.next
      line.stripLineEnd.split(' ').foreach{word =>
        vocab.addWordToVocab(word)
    }

   }
   vocab.sortVocab(opts.minCount.value, opts.ignoreStopWords.value, opts.vocabSize.value)
   val corpusFile = io.Source.fromInputStream(new FileInputStream(opts.trainFile.value), "UTF-8").getLines
   while(corpusFile.hasNext){
     val line = corpusFile.next
     line.stripLineEnd.split(' ').foreach{word =>
         if(vocab.getId(word)!= -1){
           outputFile.write(word+" ")
         }

    }
    outputFile.newLine()

  }
  outputFile.close()
 }
}
