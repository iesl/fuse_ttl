package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import cc.factorie.app.nlp.embeddings.VocabBuilder
import java.io.{FileWriter, BufferedWriter, FileInputStream}


class WordPhraseOpts extends CmdOptions {
  val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which phrases have to be generated")
  val minCount = new CmdOption("min-count", 5, "INT", "used to discard words that occur less than <int> times")
  val scoreThreshold =  new CmdOption("threshold",100, "INT", "discards phrases which have score less than <int>")
  val phraseFile =  new CmdOption("phrase-file","", "FILENAME...", "File to which phrases generated are written")
  val vocabSize = new CmdOption("max-vocab-size", 2e6.toInt, "INT", "Max Vocabulary Size. Default Value is 2M . Reduce to 200k or 500k is you learn embeddings on small-data-set")
  val vocabHashSize = new CmdOption("vocab-hash-size", 14.3e6.toInt, "INT", "Vocabulary hash size")
  val ignoreStopWords = new CmdOption("ignore-stopwords", 1, "BOOLEAN", "use <bool> to include or discard stopwords.")
  val encoding = new CmdOption("encoding", "UTF8", "STRING", "use <string> for encoding option.")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File to which modified vocabulry is written")
  val writeReducedFile = new CmdOption("reduced-file", false, "BOOLEAN", "Finally write only the non-stopwords and high frequency words.This would be used for final clustering.")
}




object WordPhrase {
  def main(args:Array[String]){
    val opts = new WordPhraseOpts
    opts.parse(args)
    val inputFile = opts.trainFile.value
    val vocab = new VocabBuilder(opts.vocabHashSize.value)
    val threshold = opts.scoreThreshold.value
    val minCount=opts.minCount.value

    val trainFile = io.Source.fromInputStream(new FileInputStream(inputFile), "UTF-8").getLines
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    val phraseFile = new BufferedWriter(new FileWriter(opts.phraseFile.value))
    var t1 =  System.currentTimeMillis
    while (trainFile.hasNext) {
      var prevWord=""
      val line = trainFile.next
      line.stripLineEnd.split(' ').foreach{word =>
        vocab.addWordToVocab(word)
        if(prevWord!="") vocab.addWordToVocab(prevWord+"_"+word)
        prevWord=word

      }
    }
    vocab.sortVocab(opts.minCount.value, opts.ignoreStopWords.value, opts.vocabSize.value)
    val totalTokens=vocab.trainWords()
    generatePhrases(inputFile,outputFile,phraseFile,totalTokens,vocab,minCount,threshold)
    println("Vocab Size "+vocab.size())
    println("Total words in training file "+totalTokens)
    val t2 =  System.currentTimeMillis
    println("Time taken for phrase generation and vocabulary building "+(t2-t1)/1000.0 +" seconds")


  }

  def generatePhrases(inputFile:String,outputFile:BufferedWriter,phraseFile:BufferedWriter,totalTokens:Long,vocab:VocabBuilder,minCount:Int,threshold:Int){

    val corpusFile = io.Source.fromInputStream(new FileInputStream(inputFile), "UTF-8").getLines
    while (corpusFile.hasNext) {
      var prevWord=""
      var pab=0
      var pb=0
      var pa=0
      var li = -1
      var score:Long =0
      var first=true
      var skip=false
      val line = corpusFile.next
      line.stripLineEnd.split(' ').foreach{word =>
        var oov=false

        val id = vocab.getId(word)
        if(id == -1) oov=true  else pb=vocab.getCount(id)
        if(li == -1) oov=true  // this implies that both words in the bigram should be in vocab
        li=id
        if(prevWord!="" && !skip) {
          var bigram=prevWord+"_"+word
          val bid =  vocab.getId(bigram)
          if(bid== -1) oov=true else pab=vocab.getCount(bid)
          if(pa < minCount) oov=true
          if(pb < minCount) oov=true

          if(oov) score=0 else score=totalTokens * ((pab-minCount).toLong)/((pa * pb).toLong)
          if(score>threshold){
            outputFile.write("_"+word)
            phraseFile.write(bigram+"\t"+score+"\n")
            skip=true
          }else  outputFile.write(" "+word)

        }
        else{
          if(first) {outputFile.write(word);first=false}
          else {outputFile.write(" "+word);skip=false }
        }
        pa=pb
        prevWord=word
      }
      outputFile.newLine()
    }
    outputFile.close()
    phraseFile.close()
  }

}
