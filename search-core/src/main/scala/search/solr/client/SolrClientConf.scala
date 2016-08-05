package search.solr.client

import scala.collection.mutable.HashMap
import scala.collection.JavaConverters._

/**
  * Created by soledede on 2015/11/16.
  */
private[search] class SolrClientConf(loadDefaults: Boolean) extends Cloneable {
  def this() = this(true)

  private val settings = new HashMap[String, String]()

  if (loadDefaults) {
    // Load any solr.client.* system properties
    for ((k, v) <- System.getProperties.asScala if k.startsWith("solr.client.")) {
      settings(k) = v
    }
  }

  def set(key: String, value: String): SolrClientConf = {
    if (key == null) {
      throw new NullPointerException("null key")
    }
    if (value == null) {
      throw new NullPointerException("null value")
    }
    settings(key) = value
    this
  }


  /** Set multiple parameters together */
  def setAll(settings: Traversable[(String, String)]) = {
    this.settings ++= settings
    this
  }

  /** Set a parameter if it isn't already configured */
  def setIfMissing(key: String, value: String): SolrClientConf = {
    if (!settings.contains(key)) {
      settings(key) = value
    }
    this
  }


  /** Remove a parameter from the configuration */
  def remove(key: String): SolrClientConf = {
    settings.remove(key)
    this
  }

  /** Get a parameter; throws a NoSuchElementException if it's not set */
  def get(key: String): String = {
    settings.getOrElse(key, throw new NoSuchElementException(key))
  }

  /** Get a parameter, falling back to a default if not set */
  def get(key: String, defaultValue: String): String = {
    settings.getOrElse(key, defaultValue)
  }

  /** Get a parameter as an Option */
  def getOption(key: String): Option[String] = {
    settings.get(key)
  }

  /** Get all parameters as a list of pairs */
  def getAll: Array[(String, String)] = settings.clone().toArray

  /** Get a parameter as an integer, falling back to a default if not set */
  def getInt(key: String, defaultValue: Int): Int = {
    getOption(key).map(_.toInt).getOrElse(defaultValue)
  }

  /** Get a parameter as a long, falling back to a default if not set */
  def getLong(key: String, defaultValue: Long): Long = {
    getOption(key).map(_.toLong).getOrElse(defaultValue)
  }

  /** Get a parameter as a double, falling back to a default if not set */
  def getDouble(key: String, defaultValue: Double): Double = {
    getOption(key).map(_.toDouble).getOrElse(defaultValue)
  }

  /** Get a parameter as a boolean, falling back to a default if not set */
  def getBoolean(key: String, defaultValue: Boolean): Boolean = {
    getOption(key).map(_.toBoolean).getOrElse(defaultValue)
  }


  /** Does the configuration contain a given parameter? */
  def contains(key: String): Boolean = settings.contains(key)

  /** Copy this object */
  override def clone: SolrClientConf = {
    new SolrClientConf(false).setAll(settings)
  }

  /**
    * By using this instead of System.getenv(), environment variables can be mocked
    * in unit tests.
    */
  private[search] def getenv(name: String): String = System.getenv(name)


  /**
    * Return a string listing all keys and values, one per line. This is useful to print the
    * configuration out for debugging.
    */
  def toDebugString: String = {
    settings.toArray.sorted.map { case (k, v) => k + "=" + v }.mkString("\n")
  }
}
