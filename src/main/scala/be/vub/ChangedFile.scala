package be.vub

import java.io.{BufferedWriter, File, FileWriter}

import com.github.gumtreediff.client.Run
import com.github.gumtreediff.gen.Generators
import com.github.gumtreediff.matchers.{Mapping, Matchers}
import com.github.gumtreediff.tree.ITree

import scala.jdk.CollectionConverters._

class ChangedFile(commit: Commit, oldProgram: String, newProgram: String) {

  def getConcreteEdits: List[ConcreteEdit] = {
    Run.initGenerators()

    val oldFile: File = {
      val oldFile = new File("Old.cs")
      val oldbw = new BufferedWriter(new FileWriter(oldFile))
      oldbw.write(oldProgram)
      oldbw.close()
      oldFile
    }

    val newFile: File = {
      val newFile = new File("New.cs")
      val newbw = new BufferedWriter(new FileWriter(newFile))
      newbw.write(newProgram)
      newbw.close()
      newFile
    }

    val oldTree: ITree = {
      val oldTreeContext = Generators.getInstance.getTree(oldFile.getAbsolutePath)
      oldTreeContext.getRoot // return the root of the tree
    }

    val newTree: ITree = {
      val newTreeContext = Generators.getInstance.getTree(newFile.getAbsolutePath)
      newTreeContext.getRoot
    }
    val m = Matchers.getInstance.getMatcher(newTree, oldTree) // retrieve the default matcher
    m.`match`()
    val mappingStore = m.getMappings

    val mappings: List[Mapping] = mappingStore.asScala.toList

    var modified: List[Mapping] = List[Mapping]()
    var unmodified: List[Mapping] = List[Mapping]()
    for (mapping <- mappings) {
      if (mapping.first.isIsomorphicTo(mapping.second)) {
        unmodified = mapping :: unmodified
      } else {
        modified = mapping :: modified
      }
    }

    var concreteEdits: List[ConcreteEdit] = List[ConcreteEdit]()

    for (mod <- modified) {
      val concreteEdit: ConcreteEdit = new ConcreteEdit(beforeTree = mod.getFirst,
        afterTree = mod.getSecond,
        commits = List[String](commit.getCommitIdHash))

      concreteEdits = concreteEdit :: concreteEdits
    }

    concreteEdits
  }
}
