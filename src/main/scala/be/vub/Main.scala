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

    // dump PATH BRANCH NUMBER_OF_COMMITS
    // E.g. dump my/path/to/repo.git master 100
    if(args.length >= 3 && args(0) == "dump" && args(1).endsWith(".git")){
      val repo = new Repository(args(1))
      val commits = repo.getCommits(args(2), if(args.length == 4) args(3).toInt else Integer.MAX_VALUE)
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
    // cluster PATH BRANCH CLUSTER_TYPE NUMBER_OF_COMMITS
    // E.g. cluster my/path/to/repo.git master RelevantCluster 100
    // E.g. cluster my/path/to/repo.git master BigCluster 100
    else if(args.length >= 4 && args(0).equals("cluster") && args(1).endsWith(".git")){
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

      val repo = new Repository(args(1))
      val commits = repo.getCommits(args(2), if(args.length == 5) args(4).toInt else Integer.MAX_VALUE)
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
      val mode = args(3)
      if(mode == "BigCluster"){
        cluster.resultAsJSON()
      } else if(mode == "RelevantCluster"){
        cluster.relevantClustersToJSON()
      }
      println("V")
    } else {
      println("Unknown command")
      println("available commands are : ")
      println("- dump path branch commits")
      println("- cluster path branch (BigCluster|RelevantCluster) commits")
    }
  }
}
