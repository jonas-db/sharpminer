package be.vub

import java.io.{BufferedWriter, File, FileWriter}
import java.nio.file.{Path, Paths}

import scala.collection.mutable
import scala.collection.parallel.CollectionConverters._
import scala.collection.parallel.mutable.ParArray
import scala.math.floorMod

class HierarchicalCluster(edits : List[Edit]) {
  private var activeClusters : ParArray[Edit] = ArrayIsParallelizable(edits.toArray).par
  val hole = 7200
  //Pick a pair e1, e2
  //Generalize the edit pattern using anti-unification
  //remove e1, e2 and add e3 instead
  //set e3 as the parent of e1 and e2

  private val s: mutable.Stack[Edit] = mutable.Stack()

  def findClosest(current: Edit) : (Edit,EditPattern) = {
    var closest : Edit = current
    var closestEditPattern : EditPattern = null
    var closestDistance : Int = -1
    for{cluster <- activeClusters} {
      //activeClusters.filter(_ != current).seq
      if(!cluster.equals(current)) {
        val editPattern = new EditPattern(cluster,current)
        val distance = editPattern.getDistance
        if ((distance < closestDistance) || (closestDistance == -1)) {
          closest = cluster
          closestEditPattern = editPattern
          closestDistance = distance
        }
      }

    }
    (closest,closestEditPattern)
  }

  var average: Long = 0
  var done = 1

  @scala.annotation.tailrec
  private def nearestNeighbourChain(): Unit = {
    val start = java.lang.System.currentTimeMillis()
    if(s.isEmpty){
      s.push(activeClusters.head)
    }
    val current: Edit = s.pop()
    s.push(current)
    val closest = findClosest(current)
    if(s.contains(closest._1)){
      s.pop()
      s.remove(s.indexOf(closest._1))
      //activeClusters = closest._2 +: activeClusters.filter(_ != closest._1).filter(_ != current)
      activeClusters = closest._2 +: activeClusters.filter(x => x != closest._1 && x != current)
    } else {
      s.push(closest._1)
    }
    val end = java.lang.System.currentTimeMillis()
    val diff = end-start

    average = average + diff
    done = done + 1

    if(done % 100 == 0) {
      val le = activeClusters.length
      //val eta = (Math.pow(le, 2)*(average/done)) / edits.length
      val eta = (le*(average/done))
      println(s"Took ${diff}ms, Clusters left: ${le}, ETA: ${(eta/1000).toInt}secs ${(eta/60000).toInt}mins ${(eta/(60000*60)).toInt}hours")
    }

    if(activeClusters.length <= 1) {
      println(s"Done. Iterations=$done")
    }

    if(activeClusters.length != 1) {
      nearestNeighbourChain()
    }
  }

  private val result : Edit = {
    nearestNeighbourChain()
    activeClusters.head
  }

  def getResult : Edit = result

  private def writeEdit(edit: Edit): Unit = {
    val p = Paths.get("out", "json", "Edits", "edit" + edit.getID + ".json")
    val newFile = p.toFile // new File("./out/json/Edits/edit" + edit.getID + ".json")
    val newbw = new BufferedWriter(new FileWriter(newFile))
    ujson.writeTo(edit.editToJSON, newbw, 4)
    newbw.close()
  }

  def resultAsJSON(): ujson.Arr = {
    def clusterToJSON(edit : Edit): ujson.Arr = {
      val left = edit.getLeft
      val right = edit.getRight
      var children = List[ujson.Arr]()
      if(left != null && right != null){
        children = clusterToJSON(left) :: clusterToJSON(right) :: children
      }
      var importance = false
      if(!(floorMod(edit.getBeforeTree.getType,10000)  == hole) && !(floorMod(edit.getAfterTree.getType,10000) == hole)){
        writeEdit(edit)
        importance = true
      }
      ujson.Arr(
        ujson.Obj("Reference"-> edit.getID,"importance"->importance ,"distance"-> edit.getDistance,"Children"-> children)
      )
    }
    val dir11 = new File("out")
    var successful11 = dir11.mkdir
    while(!successful11){
      successful11=dir11.mkdir()
      println("unsuccesfull 11")
    }

    val dir1 = new File("out/json")
    var successful1 = dir1.mkdir
    while(!successful1){
      successful1=dir1.mkdir()
      println("unsuccesfull 1")
    }
    val dir2 = new File("out/json/Edits")
    var successful2 = dir2.mkdir
    while(!successful2){
      successful2=dir2.mkdir()
      println("unsuccesfull 2")
    }
    val json = clusterToJSON(result)
    val newFile = new File("out/json/Cluster.json")
    println("writing:")
    val newbw = new BufferedWriter(new FileWriter(newFile))
    ujson.writeTo(json, newbw, 4)
    newbw.close()
    json
  }

  def relevantClustersToJSON() : Unit = {
    var l = List(result)

    var set: Set[Int] = Set.empty[Int]
    var duplicate: Int = 0

    def childrenToJSON(edit: Edit): ujson.Arr = {
        val left = edit.getLeft
        val right = edit.getRight
        var children = List[ujson.Arr]()

        if(left != null && right != null){
          //children = childrenToJSON(left) :: childrenToJSON(right) :: children

          var todo = List(left, right)

          while(todo.nonEmpty) {
            val h = todo.head
            todo = todo.tail

            children = childrenToJSON(h) :: children
          }
        }

        if(!(floorMod(edit.getBeforeTree.getType,10000)  == hole) && !(floorMod(edit.getAfterTree.getType,10000) == hole)){
          writeEdit(edit)

        }
/*
      if(set.contains(edit.getID)) {
        println("contains")
        duplicate = duplicate + 1
      } else {
        println("not contains:"+edit.getID)
      }
      set = set + edit.getID
*/
      ujson.Arr(
        ujson.Obj("Reference"-> edit.getID,"distance"-> edit.getDistance,"Children"-> children)
      )
    }


    def clusterToJSON(edit : Edit): Unit ={
      val left = edit.getLeft
      val right = edit.getRight

      if(!(floorMod(edit.getBeforeTree.getType,10000)  == hole) && !(floorMod(edit.getAfterTree.getType,10000) == hole)){
        writeEdit(edit)
        val newFile = new File("out/json/Clusters/Cluster"+ edit.getID + ".json")
        var children = List[ujson.Arr]()
        if(left != null && right != null) {
          //children = childrenToJSON(left) :: childrenToJSON(right) :: children

          var todo = List(left, right)

          while(todo.nonEmpty) {
            val h = todo.head
            todo = todo.tail

            children = childrenToJSON(h) :: children
          }

        }
        val json = ujson.Arr(
          ujson.Obj("Reference"-> edit.getID,"distance"-> edit.getDistance,"Children"-> children)
        )
        val newbw = new BufferedWriter(new FileWriter(newFile))
        ujson.writeTo(json, newbw, 4)
        newbw.close()
      } else {
        if(left != null && right != null){
          l = l :+ left
          l = l :+ right
          //clusterToJSON(left)
          //clusterToJSON(right)
        }
      }
    }
    val dir11 = new File("out")
    var successful11 = dir11.mkdir
    while(!successful11 && !dir11.exists()){
      successful11=dir11.mkdir()
      println("unsuccesfull 11")
    }

    val dir1 = new File("out/json")
    var successful1 = dir1.mkdir
    while(!successful1 && !dir11.exists()){
      successful1=dir1.mkdir()
      println("unsuccesfull 1")
    }
    val dir2 = new File("out/json/Edits")
    var successful2 = dir2.mkdir
    while(!successful2 && !dir11.exists()){
      successful2=dir2.mkdir()
      println("unsuccesfull 2")
    }
    val dir3 = new File("out/json/Clusters")
    var successful3 = dir3.mkdir
    while(!successful3 && !dir11.exists()){
      successful3=dir3.mkdir()
      println("unsuccesfull 3")
    }
    //clusterToJSON(result)



    while(l.nonEmpty) {
        val h = l.head
        l = l.tail

        clusterToJSON(h)
    }

    println("duplicate="+duplicate)
  }





}
