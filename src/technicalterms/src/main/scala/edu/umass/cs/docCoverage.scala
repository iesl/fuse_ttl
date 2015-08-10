package edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{FileOutputStream, OutputStreamWriter, BufferedWriter, File}
import scala.collection.mutable.HashSet


/**
 * Calculates evaluation metric for technical terms list
 * Document Coverage = Percentage of documents with atleast one term from the list
 */

class docCoverageOpts extends CmdOptions {
  val termFile =  new CmdOption("term-file","", "FILENAME...", "File which has the terms generated from trainfile")
  val trainFile = new CmdOption("train-file","", "FILENAME...", "File from which phrases are generated")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File to coverage measure is written")
}

object docCoverage {
  def main(args:Array[String]){
    val opts = new docCoverageOpts
    opts.parse(args)
    val phraseFile = scala.io.Source.fromFile(new File(opts.termFile.value),"UTF-8").getLines()
    val trainFile = scala.io.Source.fromFile(new File(opts.trainFile.value),"UTF-8").getLines()
    val outputFile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(opts.outputFile.value),"UTF8"))
    var coverageCount = 0
    var phraseHashSet =new HashSet[String]()
    phraseFile foreach{phrase=>
       phraseHashSet += phrase.stripLineEnd.split(" ").mkString("_")
    }

    var docCount = 0
    trainFile foreach{line=>
       docCount+=1
       var found=0
       val splitLine = line.stripLineEnd.split(" ")
       splitLine.drop(2).foreach{s=>
       if(phraseHashSet.contains(s)){
          found=1
        }
       }
      if(found==1){
        coverageCount+=1
      }
    }
    outputFile.write("Total number of terms : "+phraseHashSet.size +"\n")
    outputFile.write("No of documents in input data : "+docCount +"\n")
    outputFile.write("No of documents with at least one term : "+coverageCount +"\n")
    outputFile.write("Document Coverage : " + ((coverageCount.toDouble)/(docCount.toDouble))*100 +"\n")
    outputFile.close()
  }
}
