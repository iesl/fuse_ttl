package main.scala.edu.umass.cs

import cc.factorie.util.CmdOptions
import java.io.{FileWriter, BufferedWriter, FileInputStream}


class MapKmeansClustersOpts extends CmdOptions {
  val outputFile = new CmdOption("output-file","", "FILENAME...", "File which contains cluster id to word mapping")
  val mappingFile = new CmdOption("mapping-file","", "FILENAME...", "File with mappings from ids to tokens")
  val clusterFile = new CmdOption("cluster-file","", "FILENAME...", "File with clustering info <clusterid> <tokenid>")
}


object MapKmeansClusters {
   def main(args:Array[String]){
     val opts = new MapKmeansClustersOpts
     opts.parse(args)
     val outputFile = new BufferedWriter(new FileWriter(opts.outputFile.value))
     val idWordMap = new collection.mutable.HashMap[Int, String]()
     val mappingFile = io.Source.fromInputStream(new FileInputStream(opts.mappingFile.value)).getLines
     while (mappingFile.hasNext) {
       val line = mappingFile.next
       val wordLine = line.split("\t")
       idWordMap(wordLine(0).toInt)=wordLine(1)
     }

     val clusterFile = io.Source.fromInputStream(new FileInputStream(opts.clusterFile.value)).getLines
     var prevCId = -1
     while(clusterFile.hasNext){
       val line= clusterFile.next
       val wordLine=line.split("\t")
       val clusterId = wordLine(0).toInt
       if(clusterId!=prevCId){
         outputFile.newLine()
         outputFile.write(clusterId.toString+"----")
       }
       outputFile.write(" "+idWordMap(wordLine(1).toInt))
       prevCId=clusterId
     }
     outputFile.close()
   }
}
