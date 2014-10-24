package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{BufferedInputStream, FileWriter, BufferedWriter, FileInputStream}
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import java.util.zip.GZIPInputStream
import java.util


class PreprocessKmeansOpts extends CmdOptions {
  val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which data should be used for kmeans clustering")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "Input File in the SVM-light format")
  val embeddingFile = new CmdOption("embedding-file","", "FILENAME...", "File which contains embeddings")
  val mappingFile = new CmdOption("mapping-file","", "FILENAME...", "File which mappings from ids to tokens")
}

object PreprocessKmeans {
  def main(args:Array[String]){
    val opts = new PreprocessKmeansOpts
    opts.parse(args)
    var uniqueWordSet = new HashSet[String]
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    val mappingFile = new BufferedWriter(new FileWriter(opts.mappingFile.value))

    println("Finding unique word set")
    val trainFile = io.Source.fromInputStream(new FileInputStream(opts.trainFile.value), "UTF-8").getLines

    while (trainFile.hasNext) {
      val line = trainFile.next
      line.stripLineEnd.split(" ").foreach{word =>
        uniqueWordSet+=word
      }
    }
    println("Finished finding unique word set")
    println("No of unique words "+uniqueWordSet.size)

    println("Start reading embeddings")
    val embedFile =  io.Source.fromInputStream(new GZIPInputStream(new FileInputStream(opts.embeddingFile.value)),"ISO-8859-1").getLines
    var wordId=1

    while (embedFile.hasNext) {
      val line = embedFile.next
      val wordLine=line.split("\t")
      if(wordLine.length==2) {
        if(uniqueWordSet.contains(wordLine(0))){
          mappingFile.write(wordId+"\t"+wordLine(0))
          outputFile.write(wordId.toString)
          var count=1
          wordLine(1).split(" ").foreach{dimValue=>
            outputFile.write(" "+count+":"+dimValue)
            count+=1
          }
          wordId+=1
          outputFile.newLine()
          mappingFile.newLine()
        }
      }
    }
    outputFile.close()
    mappingFile.close()
    println("Finish reading embeddings")
    println("No of words found in embeddings "+wordId)

  }
}
