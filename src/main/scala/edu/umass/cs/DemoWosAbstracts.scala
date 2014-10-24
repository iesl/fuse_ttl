package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{FileWriter, BufferedWriter, FileInputStream}
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

class DemoWosAbstractsOpts extends CmdOptions{
  val listFile =  new CmdOption("list-file","", "FILENAME...", "File which contains list of abstracts to be used")
  val dataFile = new CmdOption("data-file","", "FILENAME...", "File from which abstracts have to be extracted")
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File containing abstracts used for demo")
  val listNotFoundFile =  new CmdOption("list-notfound-file","", "FILENAME...", "File containing lits of abstracts not found")
}


object DemoWosAbstracts {
  def main(args:Array[String]){
    val opts = new DemoWosAbstractsOpts
    opts.parse(args)
    val listFile = io.Source.fromInputStream(new FileInputStream(opts.listFile.value), "UTF-8").getLines
    val dataFile = io.Source.fromInputStream(new FileInputStream(opts.dataFile.value), "UTF-8").getLines
    val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
    val listNotFoundFile =  new BufferedWriter(new FileWriter(opts.listNotFoundFile.value))
    var t1 =  System.currentTimeMillis
    var listHashSet =new HashSet[String]()
    var listFoundCount =new HashMap[String,Int]()
    while (listFile.hasNext) {
      val abstractId = listFile.next
      listHashSet+=abstractId

    }

    println("loaded list of files "+listHashSet.size)
    while(dataFile.hasNext){
      val data =  dataFile.next
      val dataLine = data.stripLineEnd.split(" ")
      val id = dataLine(0).split(":")(1)
      if(listHashSet.contains(id)){
        println(id)
        listFoundCount(id) = 1 + listFoundCount.getOrElse(id, 0)
        if(listFoundCount(id)==1){
          outputFile.write(dataLine.drop(2).mkString(" "))
          outputFile.newLine()
        }
      }
    }
    outputFile.close()
    listFoundCount.foreach({case ((value,count))=>
      listNotFoundFile.write(value+" "+count.toString())
      listNotFoundFile.newLine()

    })
    listNotFoundFile.close()

    println("finished extracting documents")
    val t2 =  System.currentTimeMillis
    println("Time taken for phrase generation and vocabulary building "+(t2-t1)/1000.0 +" seconds")
  }
}
