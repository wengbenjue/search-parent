package search.common.algorithm.impl

import java.util

import search.common.algorithm.{Dictionary, TrieNode}
import search.common.config.Configuration
import search.common.util.{Logging, Util}

import scala.util.control.Breaks._
import scala.collection.JavaConversions._

/**
  * Created by soledede.weng on 2016/9/12.
  */
class TrieDictionary extends Dictionary with Logging {

  import search.common.algorithm.impl.TrieDictionary._

  var rootNodes = new Array[TrieNode](FIRST_CHARACTER_INDEX_LENGTH)


  /**
    * add a word to trie node
    *
    * @param word
    */
  override def add(word: String): Unit = {
    if (word == null || word.trim.length < 1) {
      return
    }
    var node = getRootNodeIfNotExistThenCreate(word.trim.charAt(0))
    val remainCharaters = word.trim.drop(1)
    remainCharaters.foreach { character =>
      val child = node.getChildIfNotExistThenCreate(character)
      node = child
    }
    //this represent this is a right word
    //node.terminal = true
    node.setTerminal(true)
  }

  override def remove(word: String): Unit = {
    if (word == null || word.trim.isEmpty) return
    val item = word.trim
    var rootNode = getRootNodeByCharacter(item.charAt(0))
    if (rootNode == null) {
      logDebug(s"remove,no root node for word: ${word} ")
      return
    }

    for (i <- 1 until item.length) {
      val character = item.charAt(i)
      val child = rootNode.getChild(character)
      if (child == null) {
        logDebug(s"remove,node not exists for word: ${word}")
      } else rootNode = child
    }
    if (rootNode.isTerminal) {
      //if (rootNode.terminal) {
      //set current node to non leaf node,this represent delete this word from trie tree
      // rootNode.terminal = false
      rootNode.setTerminal(false)
      logDebug(s"word ${word} have removed from trie node")
    } else {
      logDebug(s"remove,node not exists for word: ${word}")
    }
  }


  override def removeAll(items: util.List[String]): Unit = {
    items.foreach(remove(_))
  }


  override def contains(word: String): Boolean = {
    contains(word,0,word.length)
  }

  private def contains(item: String, start: Int, length: Int): Boolean = {
    if (start < 0 || length < 1) {
      return false
    }
    if (item == null || item.length < length) {
      return false
    }
    var node: TrieNode = getRootNodeByCharacter(item.charAt(start))
    if (node == null) {
      return false
    }
    for (i <- 1 until length) {
      val character: Char = item.charAt(i + start)
      val child: TrieNode = node.getChild(character)
      if (child == null) {
        return false
      }
      else {
        node = child
      }
    }
    if (node.isTerminal) {
      return true
    }
    return false
  }

  override def prefix(prefix: String): java.util.List[String] = {
    val result = new util.ArrayList[String]
    if (prefix == null || prefix.trim.length < 1) return result
    val word = prefix.trim
    //search rootNode
    var rootNode = getRootNodeByCharacter(word.charAt(0))
    if (rootNode == null) return result
    //find node of prefix
    for (i <- 1 until word.length) {
      val character = prefix.charAt(i)
      val child = rootNode.getChild(character)
      if (child == null) {
        return result
      } else rootNode = child
    }

    //search node recursion
    searchNodeByPrefix(rootNode, prefix)
    def searchNodeByPrefix(node: TrieNode, prefix: String): Unit = {
      //current node is terminal
      //  if (node.terminal) result.add(prefix)
      if (node.isTerminal) result.add(prefix)
      for (item <- node.getChildren) {
        //for (item <- node.children) {
        //searchNodeByPrefix(item, prefix + item.character)
        searchNodeByPrefix(item, prefix + item.getCharacter)
      }
    }
    result
  }

  override def clear(): Unit = {
    rootNodes = new Array[TrieNode](FIRST_CHARACTER_INDEX_LENGTH)
  }

  /**
    * get the rootNode of character,if not exists add it to rootNodes
    *
    * @param character
    * @return
    */
  protected def getRootNodeIfNotExistThenCreate(character: Char): TrieNode = {
    var rootNode = getRootNodeByCharacter(character)
    if (rootNode == null) {
      rootNode = new TrieNode(character)
      addRootNode(rootNode)
    }
    rootNode
  }

  /**
    * add a rootNode ,if there is have one or more,link it after cuurent rootNode
    *
    * @param rootNode
    */
  def addRootNode(rootNode: TrieNode): Unit = {
    //the index of current root node in  rootNodes
    //val index: Int = rootNode.character % FIRST_CHARACTER_INDEX_LENGTH
    val index: Int = rootNode.getCharacter % FIRST_CHARACTER_INDEX_LENGTH

    //check if there are some conflict
    val existsRootNode = rootNodes(index)
    if (existsRootNode != null) {
      //  rootNode.brother = existsRootNode
      rootNode.setBrother(existsRootNode)
    }
    //make sure the new rootNode always in the front
    rootNodes(index) = rootNode
  }

  /**
    * get the rootNode of character,if not exists,return null
    *
    * @param character
    * @return
    */
  protected def getRootNodeByCharacter(character: Char) = {
    //the index of current root node in  rootNodes
    val index: Int = character % FIRST_CHARACTER_INDEX_LENGTH
    var rootNode = rootNodes(index)
    // while (rootNode != null && character != rootNode.character) {
    while (rootNode != null && character != rootNode.getCharacter) {
      //if have conflict,then search it by chain
      // rootNode = rootNode.brother
      rootNode = rootNode.getBrother
    }
    rootNode
  }
}


object TrieDictionary extends Configuration {
  //limit the range of first node,default characters is 20000
  val FIRST_CHARACTER_INDEX_LENGTH = firstCharacterIndexLength
}

/*class TrieNode(
                var character: Char,
                var terminal: Boolean,
                var brother: TrieNode,
                var children: Array[TrieNode],
                var id: String
              ) extends Comparable[Char] {

  def this(character: Char) = {
    this(character, false, null, new Array[TrieNode](0), null)
  }

  /**
    * insert a character to the order array(elements)
    *
    * @param elements order array
    * @param element
    * @return
    */
  def insert(elements: Array[TrieNode], element: TrieNode): Array[TrieNode] = {

    var newElements: Array[TrieNode] = new Array[TrieNode](0)
    if (elements == null || elements.length == 0) {
      newElements = new Array[TrieNode](1)
      newElements(0) = element
      return newElements
    }
    val length = elements.length
    newElements = new Array[TrieNode](length + 1)
    var isLastElement = true
    breakable {
      for (i <- 0 until length) {
        if (element.character <= elements.length) {
          //insert the new element to this position
          newElements(i) = element
          //other elements fall back order by order
          System.arraycopy(elements, i, newElements, i + 1, length - i)
          isLastElement = false
          break
        } else {
          newElements(i) = elements(i)
        }
      }
    }
    if (isLastElement) {
      //insert new element to the tail
      newElements(length) = element
    }

    return newElements
  }

  def addChild(child: TrieNode) {
    children = insert(children, child)
  }

  /**
    * search node that represent the character from children by Two point search algorithm
    *
    * @param character the character to be found
    * @return
    */
  def getChild(character: Char): TrieNode = {
    if (children == null) return null
    //val index = Util.binarySearch(children,character).getOrElse(-1)
    val index = java.util.Arrays.binarySearch(children.asInstanceOf[Array[AnyRef]], character)
    if (index >= 0) {
      children(index)
    } else null

  }

  /**
    *
    * @param character
    * @return
    */
  def getChildIfNotExistThenCreate(character: Char): TrieNode = {
    var child = getChild(character)
    if (child == null) {
      child = new TrieNode(character)
      addChild(child)
    }
    child
  }

  override def compareTo(o: Char): Int = this.character - o.toChar
}*/

object TestTrieNode {
  def main(args: Array[String]) {


    //val d = new TrieDictionary
    val d = new TrieDictionaryExpand
    /* d.add("测试")
     d.add("翁本")
     d.add("翁本珏")
     d.add("翁梅")
     d.add("sole")
     d.add("soledede")
     d.add("solede")
     d.add("same")
     d.add("some")
     var prefixList = d.prefix("solede")
     println(prefixList)
     d.remove("solede")
     prefixList = d.prefix("solede")
     println(prefixList)*/

    /* d.add("测试1", "1")
     d.add("翁本1", "2")
     d.add("翁本珏1", "3")
     d.add("翁梅1", "4")
     d.add("sole1", "5")
     d.add("soledede", "6")*/
    d.add("CQGT", "7")
    d.add("CEGT", "8")
    d.add("CQCG", "8")
    d.add("CQRD", "8")
    d.add("CQGTF", "7")
    d.add("CQPJ", "7")
    // d.add("CHFRT", "8")
    //d.add("CQGED", "7")
    val prefixList1 = d.prefixWitId("cq".toUpperCase)
    println(prefixList1)


    println("阿拉伯" > "美国")
  }
}