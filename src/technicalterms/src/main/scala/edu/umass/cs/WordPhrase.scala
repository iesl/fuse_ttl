/* Copyright (C) 2015 University of Massachusetts Amherst.
   This file is part of “fuse_ttl”
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
import cc.factorie.app.nlp.embeddings.VocabBuilder
import java.io._


class WordPhraseOpts extends CmdOptions {
  val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which phrases have to be generated")
  val minCount = new CmdOption("min-count", 20, "INT", "used to discard words that occur less than <int> times")
  val scoreThreshold =  new CmdOption("threshold",100, "INT", "discards phrases which have score less than <int>")
  val phraseFile =  new CmdOption("phrase-file","", "FILENAME...", "File to which phrases generated are written")
  val vocabSize = new CmdOption("max-vocab-size", 2e6.toInt, "INT", "Max Vocabulary Size. Default Value is 2M . Reduce to 200k or 500k is you learn embeddings on small-data-set")
  val vocabHashSize = new CmdOption("vocab-hash-size", 14.3e6.toInt, "INT", "Vocabulary hash size")
  val ignoreStopWords = new CmdOption("ignore-stopwords", 1, "BOOLEAN", "use <bool> to include or discard stopwords.")
  val encoding = new CmdOption("encoding", "UTF8", "STRING", "use <string> for encoding option.")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File to which modified vocabulry is written")
}

/**
 * Generates phrases from input data based on Pointwise Mutual Information(PMI) metric
 */


object WordPhrase {
  def main(args:Array[String]){
    val opts = new WordPhraseOpts
    opts.parse(args)
    val inputFile = opts.trainFile.value
    val vocab = new VocabBuilder(opts.vocabHashSize.value)
    val threshold = opts.scoreThreshold.value
    val minCount=opts.minCount.value
    val encoding  = opts.encoding.value

    val trainFile = io.Source.fromInputStream(new FileInputStream(inputFile), encoding).getLines
    val outputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(opts.outputFile.value),encoding))
    val phraseFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(opts.phraseFile.value),encoding))
    var t1 =  System.currentTimeMillis
    while (trainFile.hasNext) {
      var prevWord=""
      val line = trainFile.next
      // ensure that document id and year are not included
      line.stripLineEnd.split(' ').drop(2).foreach{word =>
        vocab.addWordToVocab(word)
        if(prevWord!="") vocab.addWordToVocab(prevWord+"_"+word)
        prevWord=word

      }
    }
    vocab.sortVocab(opts.minCount.value, opts.ignoreStopWords.value, opts.vocabSize.value)
    val totalTokens=vocab.trainWords()
    generatePhrases(inputFile,outputFile,phraseFile,totalTokens,vocab,minCount,threshold,encoding)
    println("Vocab Size "+vocab.size())
    println("Total words in training file "+totalTokens)
    val t2 =  System.currentTimeMillis
    println("Time taken for phrase generation "+(t2-t1)/1000.0 +" seconds")


  }

  def generatePhrases(inputFile:String,outputFile:BufferedWriter,phraseFile:BufferedWriter,totalTokens:Long,vocab:VocabBuilder,minCount:Int,threshold:Int, encoding:String){

    val corpusFile = io.Source.fromInputStream(new FileInputStream(inputFile), encoding).getLines
    while (corpusFile.hasNext) {
      var prevWord=""
      var pab=0
      var pb=0
      var pa=0
      var li = -1
      var score:Long =0
      var first=true
      var skip=false
      val line = corpusFile.next.stripLineEnd.split(' ')
      outputFile.write(line(0)+" "+line(1))
      line.drop(2).foreach{word =>
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
          if(first) {outputFile.write(" "+word);first=false}
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
