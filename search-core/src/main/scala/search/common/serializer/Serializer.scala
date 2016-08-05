package search.common.serializer

import java.io._
import java.nio.ByteBuffer

import search.common.config.SolrEnv
import search.common.util.NextIterator
import search.solr.client.{SolrClientConf, SolrClient}

import scala.reflect.ClassTag

/**
  * @author:soledede
  * @email:wengbenjue@163.com
  *                  the serializer of thead safe
  */
abstract class Serializer {

  /**
    * Default ClassLoader to use in deserialization. Implementations of [[Serializer]] should
    * make sure it is using this when set.
    */
  @volatile protected var defaultClassLoader: Option[ClassLoader] = None

  /**
    * Sets a class loader for the serializer to use in deserialization.
    *
    * @return this Serializer object
    */
  def setDefaultClassLoader(classLoader: ClassLoader): Serializer = {
    defaultClassLoader = Some(classLoader)
    this
  }

  /** Creates a new [[SerializerInstance]]. */
  def newInstance(): SerializerInstance
}

object Serializer {
  def apply(serializeType: String, conf: SolrClientConf): Serializer = {
    serializeType match {
      case "java" => getSerializer(JavaSerializer(conf))
      case _ => null.asInstanceOf[Serializer]
    }
  }

  def getSerializer(serializer: Serializer): Serializer = {
    if (serializer == null) SolrEnv.get.serializer else serializer
  }

  def getSerializer(serializer: Option[Serializer]): Serializer = {
    serializer.getOrElse(SolrEnv.get.serializer)
  }
}

/**
  * :: DeveloperApi ::
  * An instance of a serializer, for use by one thread at a time.
  */
abstract class SerializerInstance {
  def serialize[T: ClassTag](t: T): ByteBuffer

  def serializeArray[T: ClassTag](t: T): Array[Byte] = {
    null
  }


  def deserialize[T: ClassTag](bytes: ByteBuffer): T

  def deserialize[T: ClassTag](bytes: ByteBuffer, loader: ClassLoader): T

  def deserialize[T: ClassTag](input: InputStream): T = {
    null.asInstanceOf[T]
  }

  def deserialize[T: ClassTag](bytes: Array[Byte]): T = {
    null.asInstanceOf[T]
  }

  def serializeStream(s: OutputStream): SerializationStream

  def deserializeStream(s: InputStream): DeserializationStream
}

abstract class SerializationStream {
  def writeObject[T: ClassTag](t: T): SerializationStream

  def flush(): Unit

  def close(): Unit

  def writeAll[T: ClassTag](iter: Iterator[T]): SerializationStream = {
    while (iter.hasNext) {
      writeObject(iter.next())
    }
    this
  }
}

abstract class DeserializationStream {
  def readObject[T: ClassTag](): T

  def close(): Unit

  def asIterator: Iterator[Any] = new NextIterator[Any] {
    override protected def getNext() = {
      try {
        readObject[Any]()
      } catch {
        case eof: EOFException =>
          finished = true
      }
    }

    override protected def close() {
      DeserializationStream.this.close()
    }
  }
}
