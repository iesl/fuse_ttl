package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import scala.collection.mutable.HashSet
import java.io.{FileWriter, BufferedWriter, FileInputStream}
import cc.factorie.app.nlp


class FilterClustersOpts extends CmdOptions {
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File which contains clusters filtered using output from scientific extractor")
  val scientificExtractorOutput = new CmdOption("extractor-file","", "FILENAME...", "Files with noun phrases extracted from titles")
  val clusterFile = new CmdOption("cluster-file","", "FILENAME...", "File with clusters produced by using gaussian mixtures")
}

object FilterClusters {
  def main(args:Array[String]){
    val opts = new FilterClustersOpts
    opts.parse(args)
    var nounPhrases = new HashSet[String]
    var ClusterIds = new HashSet[Integer]
    // load hashset with nounphrases
    val nounPhrasesFile = io.Source.fromInputStream(new FileInputStream(opts.scientificExtractorOutput.value), "UTF-8").getLines
    while (nounPhrasesFile.hasNext) {
      val line = nounPhrasesFile.next

      //if(line=="") println("empty line")
      if(line!="") {

        line.stripLineEnd.split("\t").foreach{phrase =>
        val phrasesArray = phrase.toLowerCase().split(" ")

        if(nlp.lexicon.StopWords.containsWord(phrasesArray(0)) ) phrasesArray.drop(1)
        nounPhrases+= phrasesArray.mkString("_")
      }
     }
    }
    println("Loaded "+nounPhrases.size+" noun phrases")
    println(nounPhrases)
    var clusterFile =   io.Source.fromInputStream(new FileInputStream(opts.clusterFile.value), "UTF-8").getLines
    while (clusterFile.hasNext) {
      val line = clusterFile.next
      val lineSplit = line.split(":")
      val clusterId = lineSplit(0).split(" ")(1).toInt

      lineSplit(1).split(",").foreach{phrase =>
        val ngram = phrase.split("_").dropRight(1).mkString("_").drop(1).stripLineEnd

        if(nounPhrases.contains(ngram)){

          ClusterIds += clusterId
        }
      }
    }
    println("No of filtered clusters ="+ClusterIds.size.toString)

    println("Printing the output")
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    clusterFile =   io.Source.fromInputStream(new FileInputStream(opts.clusterFile.value), "UTF-8").getLines
    while (clusterFile.hasNext) {
      val line = clusterFile.next
      val lineSplit = line.split(":")
      val clusterId = lineSplit(0).split(" ")(1).toInt
      if(ClusterIds.contains(clusterId)){
        outputFile.write(line+"\n")
      }
    }


    outputFile.close()
  }
}
