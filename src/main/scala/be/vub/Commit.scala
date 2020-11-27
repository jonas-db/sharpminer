package be.vub

import java.io.{ByteArrayOutputStream, File, FileWriter, OutputStream, PrintWriter}

import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.{ObjectId, ObjectLoader, ObjectReader}
import org.eclipse.jgit.revwalk.{RevCommit, RevTree, RevWalk}
import org.eclipse.jgit.treewalk.CanonicalTreeParser

import scala.jdk.CollectionConverters._


class Commit(commitIdHash : String , repository: Repository) {

  def getCommitIdHash: String = commitIdHash

  //private def allChangedFiles : List[ChangedFile] = getFiles
  def fetchSource(objectId: ObjectId): String ={
    val loader : ObjectLoader = repository.getGit.getRepository.open(objectId)
    val out : OutputStream = new ByteArrayOutputStream()
    loader.copyTo(out)
    out.toString
  }


  def getDiff(): List[DiffEntry] = {
    //find the first previous commit
    val git = repository.getGit
    val commitId : ObjectId = git.getRepository.resolve(commitIdHash)
    val revWalk : RevWalk = new RevWalk(git.getRepository)
    val revCommit : RevCommit = revWalk.parseCommit(commitId)
    val revCommitTree : RevTree = revCommit.getTree
    val revPreviousCommit : RevCommit = revCommit.getParent(0)
    revWalk.parseHeaders(revPreviousCommit)
    val previousCommitId : ObjectId = revPreviousCommit.getTree
    revWalk.close()

    //Get trees for each commit
    val reader : ObjectReader = git.getRepository.newObjectReader()
    val oldTreeIter : CanonicalTreeParser = new CanonicalTreeParser()
    oldTreeIter.reset(reader, previousCommitId)
    val newTreeIter : CanonicalTreeParser = new CanonicalTreeParser()
    newTreeIter.reset(reader,revCommitTree)

    //Get list of changed files
    val diffs : List[DiffEntry] = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call().asScala.toList

    diffs
  }

  def getFiles: List[ChangedFile] = {
    val diffs = getDiff()
    var currentChangedFiles : List[ChangedFile] = List[ChangedFile]()
    for(entry : DiffEntry <- diffs){
      if ((entry.getChangeType == DiffEntry.ChangeType.MODIFY)&& entry.toString.contains(".cs")) {
        var loop = 0
        while(loop < 3){
          try{

            val file : ChangedFile = new ChangedFile(this,
            fetchSource(entry.getNewId.toObjectId),
            fetchSource(entry.getOldId.toObjectId))
            currentChangedFiles = currentChangedFiles.::(file)
            loop = 3
          }
          catch {
            case _: Throwable => loop += 1
          }
        }

      }

    }

    writeToFile("files.csv", s"$commitIdHash,${diffs.size},${diffs.count(_.toString.contains(".cs"))},${currentChangedFiles.size}\n")

    currentChangedFiles
  }

  def writeToFile(p: String, s: String): Unit = {
    val pw = new FileWriter(new File(p),true)
    try pw.write(s) finally pw.close()
  }

  def getAllConcreteEdits : List[Edit] = {
    var allEdits : List[Edit] = List()
    val all = getFiles
    for(changedFile <- all){
      val concreteEdits = changedFile.getConcreteEdits
      allEdits = concreteEdits:::allEdits
    }

    writeToFile("edits.csv", s"$commitIdHash,${allEdits.size}\n")

    allEdits
  }

}
