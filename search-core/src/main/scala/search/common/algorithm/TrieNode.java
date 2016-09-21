package search.common.algorithm;


import java.io.Serializable;
import java.util.Arrays;

/**
 * Created by soledede.weng on 2016/9/13.
 */
public  class TrieNode implements Serializable , Comparable{
    private char character;
    private boolean terminal;
    private TrieNode brother;
    private TrieNode[] children = new TrieNode[0];
    private String id;

    public TrieNode(char character) {
        this.character = character;
    }

    public boolean isTerminal() {
        return terminal;
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public char getCharacter() {
        return character;
    }

    public void setCharacter(char character) {
        this.character = character;
    }
    public TrieNode getBrother() {
        return brother;
    }

    public void setBrother(TrieNode brother) {
        this.brother = brother;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TrieNode[] getChildren() {
        return children;
    }

    public void setChildren(TrieNode[] children) {
        this.children = children;
    }
    /**
     * 利用二分搜索算法从有序数组中找到特定的节点
     *
     * @param character 待查找节点
     * @return NULL OR 节点数据
     */
    public TrieNode getChild(char character) {
        int index = Arrays.binarySearch(children, character);
        if (index >= 0) {
            return children[index];
        }
        return null;
    }

    public TrieNode getChildIfNotExistThenCreate(char character) {
        TrieNode child = getChild(character);
        if (child == null) {
            child = new TrieNode(character);
            addChild(child);
        }
        return child;
    }

    public void addChild(TrieNode child) {
        children = insert(children, child);
    }

    /**
     * 将一个字符追加到有序数组
     *
     * @param array   有序数组
     * @param element 字符
     * @return 新的有序数字
     */
    private TrieNode[] insert(TrieNode[] array, TrieNode element) {
        int length = array.length;
        if (length == 0) {
            array = new TrieNode[1];
            array[0] = element;
            return array;
        }
        TrieNode[] newArray = new TrieNode[length + 1];
        boolean insert = false;
        for (int i = 0; i < length; i++) {
            if (element.getCharacter() <= array[i].getCharacter()) {
                //新元素找到合适的插入位置
                newArray[i] = element;
                //将array中剩下的元素依次加入newArray即可退出比较操作
                System.arraycopy(array, i, newArray, i + 1, length - i);
                insert = true;
                break;
            } else {
                newArray[i] = array[i];
            }
        }
        if (!insert) {
            //将新元素追加到尾部
            newArray[length] = element;
        }
        return newArray;
    }

    /**
     * 注意这里的比较对象是char
     *
     * @param o char
     * @return
     */
    @Override
    public int compareTo(Object o) {
        return this.getCharacter() - (char) o;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrieNode trieNode = (TrieNode) o;

        return character == trieNode.character;

    }

    @Override
    public int hashCode() {
        return (int) character;
    }
}
