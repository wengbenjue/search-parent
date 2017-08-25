package search.common.serializer

import java.io._
import java.nio.ByteBuffer

import org.apache.zookeeper.server.ByteBufferInputStream
import search.solr.client.SolrClientConf
import search.common.util.Util

import scala.reflect.ClassTag

/**
 * @author soledede
 */
private[search] class JavaSerializationStream(out: OutputStream, counterReset: Int)
  extends SerializationStream {
  private val objOut = new ObjectOutputStream(out)
  private var counter = 0

  /**
   * Calling reset to avoid memory leak:
   */
  def writeObject[T: ClassTag](t: T): SerializationStream = {
    objOut.writeObject(t)
    counter += 1
    if (counterReset > 0 && counter >= counterReset) {
      objOut.reset()
      counter = 0
    }
    this
  }

  def flush() {
    objOut.flush()
  }

  def close() {
    objOut.close()
  }
}

private[search] class JavaDeserializationStream(in: InputStream, loader: ClassLoader)
  extends DeserializationStream {
  private val objIn = new ObjectInputStream(in) {
    override def resolveClass(desc: ObjectStreamClass) =
      Class.forName(desc.getName, false, loader)
  }

  def readObject[T: ClassTag](): T = objIn.readObject().asInstanceOf[T]

  def close() {
    objIn.close()
  }
}


private[search] class JavaSerializerInstance(counterReset: Int, defaultClassLoader: ClassLoader)
  extends SerializerInstance {

  override def serialize[T: ClassTag](t: T): ByteBuffer = {
    val bos = new ByteArrayOutputStream()
    val out = serializeStream(bos)
    out.writeObject(t)
    out.close()
    ByteBuffer.wrap(bos.toByteArray)
  }

  override def serializeArray[T: ClassTag](t: T): Array[Byte] = {
    val bos = new ByteArrayOutputStream()
    val out = serializeStream(bos)
    out.writeObject(t)
    out.close()
    bos.toByteArray
  }

  override def deserialize[T: ClassTag](bytes: ByteBuffer): T = {
    val bis = new ByteBufferInputStream(bytes)
    val in = deserializeStream(bis)
    in.readObject()
  }

  override def deserialize[T: ClassTag](input: InputStream): T = {
    val in = deserializeStream(input)
    in.readObject()
  }

  override def deserialize[T: ClassTag](bytes: ByteBuffer, loader: ClassLoader): T = {
    val bis = new ByteBufferInputStream(bytes)
    val in = deserializeStream(bis, loader)
    in.readObject()
  }

  override def deserialize[T: ClassTag](bytes: Array[Byte]): T = {
    val bis = new ByteBufferInputStream(ByteBuffer.wrap(bytes))
    val in = deserializeStream(bis)
    in.readObject()
  }

  override def serializeStream(s: OutputStream): SerializationStream = {
    new JavaSerializationStream(s, counterReset)
  }

  override def deserializeStream(s: InputStream): DeserializationStream = {
    new JavaDeserializationStream(s, Util.getContextClassLoader)
  }

  def deserializeStream(s: InputStream, loader: ClassLoader): DeserializationStream = {
    new JavaDeserializationStream(s, loader)
  }
}

class JavaSerializer private(conf: SolrClientConf) extends Serializer with Externalizable {
  private var counterReset = conf.getInt("crawler.serializer.objectStreamReset", 100)

  override def newInstance(): SerializerInstance = {
    val classLoader = defaultClassLoader.getOrElse(Thread.currentThread.getContextClassLoader)
    new JavaSerializerInstance(counterReset, classLoader)
  }

  override def writeExternal(out: ObjectOutput): Unit = Util.tryOrIOException {
    out.writeInt(counterReset)
  }

  override def readExternal(in: ObjectInput): Unit = Util.tryOrIOException {
    counterReset = in.readInt()
  }
}

object JavaSerializer {
  private var instance: JavaSerializer = null

  def apply(conf: SolrClientConf) = {
    if (instance == null) {
      instance = new JavaSerializer(conf)
    }
    instance
  }
}
