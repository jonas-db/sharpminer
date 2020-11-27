package be.vub

import java.io.{File, FileWriter}

object Main {

  val folder = new File("out")
  val debugFiles = new File("files.csv")
  val debugEdits = new File("edits.csv")


  def writeToFile(p: String, s: String): Unit = {
    val pw = new FileWriter(new File(p),true)
    try pw.write(s) finally pw.close()
  }

  def main(args: Array[String]): Unit = {
    if(args.length == 3){
      println("Start")


      import org.eclipse.jgit.util.FileUtils
      import java.io.IOException
      try {
        if(folder.exists()) FileUtils.delete(folder, FileUtils.RECURSIVE)
        if(debugFiles.exists()) FileUtils.delete(debugFiles)
        if(debugEdits.exists()) FileUtils.delete(debugEdits)

        Main.writeToFile("files.csv", "commitIdHash,diffs,csFiles,currentChangedFiles\n")
        Main.writeToFile("edits.csv", "commitIdHash,path,mappings,modified,unmodified,concreteEdits\n")
      }
      catch {
        case ioe: IOException =>
          // log the exception here
          ioe.printStackTrace()
          throw ioe
      }


      println("Get commits: ")
      val repo = new Repository(args(0))
      val commits = repo.getCommits("master", args(1).toInt)
      println("Commits="+commits.size)
      println("getting edits...")
      val edits = commits.flatMap(_.getAllConcreteEdits)
      println("Amount of edits : " + edits.length)
      println("start clustering: ")
      val cluster = new HierarchicalCluster(edits)
      println("creating json: ")
      val mode = args(2)
      if(mode == "BigCluster"){
        cluster.resultAsJSON()
      } else if(mode == "RelevantCluster"){
        cluster.relevantClustersToJSON()
      }
      println("V")
    } else if(args.length == 2){
      println("Start")
      println("Get commits: ")
      val edits = new Repository(args(0)).getCommits("master", Int.MaxValue).flatMap(_.getAllConcreteEdits)
      println("V")
      println("Amount of edits : " + edits.length)
      println("start clustering: ")
      val cluster = new HierarchicalCluster(edits)
      println("V")
      println("create json: ")
      val mode = args(1)
      if(mode == "BigCluster"){
        cluster.resultAsJSON()
      } else if(mode == "RelevantCluster"){
        cluster.relevantClustersToJSON()
      }
      println("V")
    } else {
      println("Unknown command")
      println("available commands are : ")
      println("- path (BigCluster|RelevantCluster)")
      println("- path amount (BigCluster|RelevantCluster)")
    }

  }
}
