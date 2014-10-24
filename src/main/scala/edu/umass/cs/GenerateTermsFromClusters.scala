package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{FileWriter, BufferedWriter, FileInputStream}


class GenerateTermsfromClustersOpts extends CmdOptions{
  val clusterFile =  new CmdOption("cluster-file","", "FILENAME...", "File which contains filtered clusters")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File containing abstracts used for demo")
}


object GenerateTermsFromClusters {
  def main(args:Array[String]){
    val opts = new GenerateTermsfromClustersOpts
    opts.parse(args)
    val clusterFile = io.Source.fromInputStream(new FileInputStream(opts.clusterFile.value), "UTF-8").getLines
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    while (clusterFile.hasNext) {

      val line = clusterFile.next
      val lineSplit = line.split(":")


      lineSplit(1).split(",").foreach{phrase =>
        val ngram = phrase.split("_").dropRight(1).mkString("_").drop(1).stripLineEnd
        outputFile.write(ngram+"\n")

      }

    }
    outputFile.close()
  }
}
