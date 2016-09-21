package search.common.algorithm.impl

import java.util

import search.common.algorithm.{DictionaryExpand, TrieNode}

/**
  * Created by soledede.weng on 2016/9/13.
  */
class TrieDictionaryExpand extends TrieDictionary with DictionaryExpand with Serializable{

  override def add(word: String, id: String): Unit = {
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
    //node.id = id
    node.setId(id)
  }

  /**
    *
    * @param prefix
    * @return (word,id)
    */
  override def prefixWitId(prefix: String): util.List[(String, String)] = {
    val result = new util.ArrayList[(String, String)]
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
      //if (node.terminal) result.add((prefix,node.id))
      if (node.isTerminal) result.add((prefix, node.getId))
      // for (item <- node.children) {
      for (item <- node.getChildren) {
        //searchNodeByPrefix(item, prefix + item.character)
        searchNodeByPrefix(item, prefix + item.getCharacter)
      }
    }
    result
  }

  override def removeWithId(word: String): Unit = {
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

    //if (rootNode.terminal) {
    if (rootNode.isTerminal) {
      //set current node to non leaf node,this represent delete this word from trie tree
      //rootNode.terminal = false
      rootNode.setTerminal(false)
      //rootNode.id = null
      rootNode.setId(null)
      logDebug(s"word ${word} have removed from trie node")
    } else {
      logDebug(s"remove,node not exists for word: ${word}")
    }
  }


  /**
    * find a word by id
    *
    * @param word
    * @return
    */
  override def findWithId(word: String): String = {
    findWithId(word, 0, word.length)
  }

  private def findWithId(item: String, start: Int, length: Int): String = {
    if (start < 0 || length < 1) {
      return null
    }
    if (item == null || item.length < length) {
      return null
    }
    var node: TrieNode = getRootNodeByCharacter(item.charAt(start))
    if (node == null) {
      return null
    }
    for (i <- 1 until length) {
      val character: Char = item.charAt(i + start)
      val child: TrieNode = node.getChild(character)
      if (child == null) {
        return null
      }
      else {
        node = child
      }
    }
    if (node.isTerminal) {
      if (node.getId == null) return null
      return node.getId
    }
    return null
  }

}
