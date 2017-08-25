package search.common.bloomfilter.mutable

import java.util.concurrent.{ConcurrentHashMap, ConcurrentMap}

import search.common.bloomfilter.CanGenerateHashFrom

import scala.collection.mutable.HashMap
import scala.reflect.ClassTag


class BloomFilter[T](numberOfBits: Long, numberOfHashes: Int)
                    (implicit canGenerateHash: CanGenerateHashFrom[T]) {

  private val bits = new UnsafeBitArray(numberOfBits)

  def add(x: T): Unit = {
    val hash = canGenerateHash.generateHash(x)
    val hash1 = hash >>> 32
    val hash2 = (hash << 32) >> 32

    var i = 0
    while (i < numberOfHashes) {
      val computedHash = hash1 + i * hash2
      bits.set((computedHash & Long.MaxValue) % numberOfBits)
      i += 1
    }
  }

  def mightContain(x: T): Boolean = {
    val hash = canGenerateHash.generateHash(x)
    val hash1 = hash >>> 32
    val hash2 = (hash << 32) >> 32
    var i = 0
    while (i < numberOfHashes) {
      val computedHash = hash1 + i * hash2
      if (!bits.get((computedHash & Long.MaxValue) % numberOfBits))
        return false
      i += 1
    }
    true
  }

  def expectedFalsePositiveRate(): Double = {
    math.pow(bits.getBitCount.toDouble / numberOfBits, numberOfHashes.toDouble)
  }

  def dispose(): Unit = bits.dispose()

}

object BloomFilter {
  private val instanceMap = new HashMap[String, AnyRef]()

  def apply[T](bizType: String, numberOfItems: Long, falsePositiveRate: Double)
                       (implicit canGenerateHash: CanGenerateHashFrom[T]): BloomFilter[T] = {

    val nb = optimalNumberOfBits(numberOfItems, falsePositiveRate)
    val nh = optimalNumberOfHashes(numberOfItems, nb)
    if (!instanceMap.contains(bizType)) {
      this.synchronized {
        if (!instanceMap.contains(bizType))
          instanceMap(bizType) = new BloomFilter[T](nb, nh)
      }
    }
    instanceMap(bizType).asInstanceOf[BloomFilter[T]]
  }

  def optimalNumberOfBits(numberOfItems: Long, falsePositiveRate: Double): Long = {
    math.ceil(-1 * numberOfItems * math.log(falsePositiveRate) / math.log(2) / math.log(2)).toLong
  }

  def optimalNumberOfHashes(numberOfItems: Long, numberOfBits: Long): Int = {
    math.ceil(numberOfBits / numberOfItems * math.log(2)).toInt
  }

}
