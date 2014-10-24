package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{File, FileWriter, BufferedWriter}
import scala.collection.mutable.HashSet
import cc.factorie.app.nlp.segment.DeterministicTokenizer
import cc.factorie.app.nlp.Document


class ReplaceVocabOpts extends CmdOptions{
   val phraseFile =  new CmdOption("phrase-file","", "FILENAME...", "File to which phrases generated are written")
   val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which phrases are generated")
   val outputFile = new CmdOption("output-file","", "FILENAME...", "Updated training file with phrases replaced by unique tokens")
}

object ReplaceVocab {
  def main(args:Array[String]){
    val opts = new ReplaceVocabOpts
    opts.parse(args)
    var t1 =  System.currentTimeMillis
    var phraseHashSet =new HashSet[String]()
    val fileBufferWriter = new BufferedWriter(new FileWriter(opts.outputFile.value))


    val phraseFile = scala.io.Source.fromFile(new File(opts.phraseFile.value)).getLines()
    phraseFile foreach{phrase=>
      val phraseLine=phrase.split("\t")
      phraseHashSet+=phraseLine(0)


    }

    /*val trainFile = scala.io.Source.fromFile(new File(opts.trainFile.value)).getLines()
    trainFile foreach{line=>
      val doc = DeterministicTokenizer.process(new Document(line))
      var finalTok =""
      val docIter = doc.tokens.iterator
      while (docIter.hasNext ) {
        val token=docIter.next()
        token.getPrev.foreach(prev => {
          val ph = prev.string +" "+token.string

          if (phraseHashSet.contains(ph)){
            fileBufferWriter.write(ph.replace(" ","_")+" ")
            if(docIter.hasNext) docIter.next()
          }
          else{
            fileBufferWriter.write(prev.string+" ")
          }

        })
        finalTok=token.string

      }
      fileBufferWriter.write(finalTok)
      fileBufferWriter.newLine()
    }
    fileBufferWriter.close() */

    val trainFile = scala.io.Source.fromFile(new File(opts.trainFile.value)).getLines()
    trainFile foreach{line=>
      var finalTok =""
      val splitLine = line.split(" ")
      val len =  splitLine.length

      if(len>1){
       val iter = splitLine.toList.sliding(2)
       while(iter.hasNext){
         val pair = iter.next()
         val ph = pair(0) +"_"+pair(1)
         if (phraseHashSet.contains(ph)){
           fileBufferWriter.write(ph+" ")
           if(iter.hasNext) iter.next()
         }
         else{
           fileBufferWriter.write(pair(0)+" ")
         }
       finalTok = pair(1)
      }
     }
     else{

         finalTok= splitLine(0)
     }
     fileBufferWriter.write(finalTok)
     fileBufferWriter.newLine()

    }

    fileBufferWriter.close()



    val t2 =  System.currentTimeMillis
    println("Time taken for generating new vocab "+(t2-t1)/1000.0 +" seconds")
  }
}
