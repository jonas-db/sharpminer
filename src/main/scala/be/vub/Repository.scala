package be.vub

import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import scala.jdk.CollectionConverters._

class Repository(pathname : String) {
  private val f : File = new File(pathname)
  
  private val git : Git = Git.open(f)
  
  def getCommits(treeName: String, limit: Int) : List[Commit] = {
    println("repo exists?: "+f.exists())
    val repository = new FileRepository(f)
    println("branch="+repository.getBranch)
    println("branch full="+repository.getFullBranch)
    var commits: List[String] = List()
    val start = repository.resolve(treeName)
    println("start (should not be null)="+start)
    for (commit <- git.log.add(start).call.asScala) {
      commits = commit.getName::commits
    }
    commits.tail.take(limit).map(x => getCommit(x))
  }
  
  def getGit : Git = git

  private def memoizedGetCommit: String => Commit = {
    def getCommit(commitIdHash : String): Commit = {
      val commit : Commit = new Commit(commitIdHash,this)
      commit
    }

    val cache = collection.mutable.Map.empty[String,Commit]

    commitIdHash => cache.getOrElse(commitIdHash,{
      cache update(commitIdHash,getCommit(commitIdHash))
      cache(commitIdHash)
    })
  }

  def getCommit: String => Commit = memoizedGetCommit

}
