package be.vub

import java.io.{File, FileWriter}

import org.eclipse.jgit.util.FileUtils
import java.io.IOException

object Main {

  val folder = new File("out/json")
  val debugFiles = new File("files.csv")
  val debugEdits = new File("edits.csv")
  val debugInfo = new File("debug.txt")

  def writeToFile(p: String, s: String): Unit = {
    val pw = new FileWriter(new File(p),true)
    try pw.write(s) finally pw.close()
  }

  def main(args: Array[String]): Unit = {

    // PATH dump NUMBER_OF_COMMITS
    // E.g. my/path/to/repo.git dump 100
    if(args.length >= 2 && args(0).endsWith(".git") && args(1) == "dump"){
      val repo = new Repository(args(0))
      val commits = repo.getCommits("master", if(args.length == 3) args(2).toInt else Integer.MAX_VALUE)
      val changedFiles = commits.flatMap(c => c.getFiles)

      val dir = new File("out/dump")
      if(dir.exists()) FileUtils.delete(dir, FileUtils.RECURSIVE)

      if(!dir.mkdir && !dir.exists()) {
        println(s"Unable to create ${dir.getAbsolutePath}")
      } else {
        changedFiles.filter(_.path.contains(".cs")).foreach(c =>{
          val content = s"{\n" +
            s"""  "id": "${c.commitIdHash}",\n""" +
            s"""  "path": "${c.path}",\n""" +
            s"""  "before": "${c.oldProgram.replaceAll("\n", "")}",\n""" +
            s"""  "after": "${c.newProgram.replaceAll("\n", "")}"\n""" +
            s"}"
            val idx = c.path.lastIndexOf("/")
            val file = c.path.substring(idx + 1, c.path.length - 3)
            Main.writeToFile(s"out/dump/${file}-${c.commitIdHash}.json", content)
        })
        println(s"Dumped ${changedFiles.size} changed files to ${dir.getAbsolutePath}")
      }
    }
    // PATH CLUSTER_TYPE NUMBER_OF_COMMITS
    // E.g. my/path/to/repo.git RelevantCluster 100
    // E.g. my/path/to/repo.git BigCluster 100
    else if(args.length == 3){
      println("Start")

      try {
        if(folder.exists()) FileUtils.delete(folder, FileUtils.RECURSIVE)
        if(debugFiles.exists()) FileUtils.delete(debugFiles)
        if(debugEdits.exists()) FileUtils.delete(debugEdits)
        if(debugInfo.exists()) FileUtils.delete(debugInfo)

        Main.writeToFile("files.csv", "commitIdHash,diffs,csFiles,currentChangedFiles\n")
        Main.writeToFile("edits.csv", "commitIdHash,path,mappings,modified,unmodified,concreteEdits\n")
      }
      catch {
        case ioe: IOException =>
          ioe.printStackTrace()
          throw ioe
      }

      println("Get commits: ")
      val start = java.lang.System.currentTimeMillis()

      val repo = new Repository(args(0))
      val commits = repo.getCommits("master", args(1).toInt)
      println("Commits="+commits.size)
      println("getting edits...")
      val edits = commits.flatMap(_.getAllConcreteEdits)
      println("Amount of edits : " + edits.length)

      val end = java.lang.System.currentTimeMillis()
      val diff = end-start
      println(s"Took ${diff/1000}secs ${diff/60000}mins")
      Main.writeToFile("debug.txt", s"${edits.length}\n${diff/1000}secs")

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
    } else {
      println("Unknown command")
      println("available commands are : ")
      println("- path (BigCluster|RelevantCluster)")
      println("- path amount (BigCluster|RelevantCluster)")
    }

  }
}
