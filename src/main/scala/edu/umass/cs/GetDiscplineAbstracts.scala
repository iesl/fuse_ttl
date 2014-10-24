package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{FileWriter, BufferedWriter, FileInputStream}
import scala.collection.mutable.{HashMap, HashSet}


class GetDiscplineAbstractsOpts extends CmdOptions{
  val listFile =  new CmdOption("list-file","", "FILENAME...", "Tab separated file which contains list of patent names to be used")
  val dataFile = new CmdOption("data-file","", "FILENAME...", "File from which abstracts have to be extracted")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File containing abstracts used for demo")
  val outputFileOrig = new CmdOption("output-file-orig","", "FILENAME...", "File containing abstracts used for demo with patent ids")
  //val listNotFoundFile =  new CmdOption("list-notfound-file","", "FILENAME...", "File containing lits of abstracts not found")
}


object GetDiscplineAbstracts {
  def main(args:Array[String]){
    val opts = new GetDiscplineAbstractsOpts
    opts.parse(args)
    val listFile = io.Source.fromInputStream(new FileInputStream(opts.listFile.value), "UTF-8").getLines
    val dataFile = io.Source.fromInputStream(new FileInputStream(opts.dataFile.value), "UTF-8").getLines
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    val outputFileOrig = new BufferedWriter(new FileWriter(opts.outputFileOrig.value))
    var t1 =  System.currentTimeMillis
    var listHashSet =new HashSet[String]()
    val firstLine = listFile.next
    while (listFile.hasNext) {
      val line = listFile.next
      val tabLine = line.split("\t")
      val year = tabLine(3).toInt
      if(year<=2007)
          listHashSet+=tabLine(1)
    }
    println("loaded list of files "+listHashSet.size)
    while(dataFile.hasNext){
      val data =  dataFile.next
      val dataLine = data.stripLineEnd.split(" ")
      val id = dataLine(0).split(":")(1)
      if(listHashSet.contains(id)){
          //println(id)
          outputFileOrig.write(data+"\n")
          outputFile.write(dataLine.drop(2).mkString(" "))
          outputFile.newLine()

      }
    }
    outputFile.close()
    outputFileOrig.close()
    println("finished extracting documents")
    val t2 =  System.currentTimeMillis
    println("Time taken for phrase generation and vocabulary building "+(t2-t1)/1000.0 +" seconds")

  }
}
