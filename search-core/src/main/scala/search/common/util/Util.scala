package search.common.util

import java.beans.Introspector
import java.io._
import java.net.{Inet4Address, InetAddress, NetworkInterface}
import java.sql.Timestamp
import java.text.{DecimalFormat, SimpleDateFormat}
import java.util
import java.util.Date
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ThreadFactory, ThreadPoolExecutor, TimeUnit}
import java.util.regex.{Matcher, Pattern}

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.google.common.net.InetAddresses
import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.commons.lang3.SystemUtils
import search.common.entity.searchinterface.NiNi
import search.solr.client.searchInterface.SearchInterface._

import scala.annotation.tailrec
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.enumerationAsScalaIterator
import scala.collection.JavaConversions.propertiesAsScalaMap
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal
import scala.collection.JavaConversions._


/**
  * Created by soledede on 2015/11/25.
  */
private[search] object Util extends Logging {
  private var customHostname: Option[String] = None
  val isWindows = SystemUtils.IS_OS_WINDOWS

  private val daemonThreadFactoryBuilder: ThreadFactoryBuilder =
    new ThreadFactoryBuilder().setDaemon(true)

  def dateToString(date: java.util.Date) = {
    val format = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss")
    format.format(date)
  }

  def dataFomatStringYMD(date: java.util.Date): String = {
    val format = new SimpleDateFormat("yyyyMMdd")
    format.format(date)
  }

  def dataFomatStringYYYY_MM_dd_HH_mm_ss(date: java.util.Date): String = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    format.format(date)
  }

  def timestampToDate(timestamp: Long): java.util.Date = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val d = format.format(timestamp)
    val date = format.parse(d)
    date
  }

  def stringToDate(dateString: String, formatString: String = "yyyy-MM-dd HH:mm:ss") = {
    val format = new SimpleDateFormat(formatString)
    val date = format.parse(dateString)
    date
  }


  def convertBean(bean: Object): java.util.Map[String,Object] = {
    val clazz = bean.getClass()
    val returnMap = new java.util.HashMap[String, Object]()
    val beanInfo = Introspector.getBeanInfo(clazz)

    val propertyDescriptors = beanInfo.getPropertyDescriptors()
    for (i <- 0 until propertyDescriptors.length) {
      val descriptor = propertyDescriptors(i)
      val propertyName = descriptor.getName()
      if (!propertyName.equals("class")) {
        val readMethod = descriptor.getReadMethod()
        val result = readMethod.invoke(bean)
        if (result != null) {
          returnMap.put(propertyName, result)
        }
      }
    }
    return returnMap
  }

  // for clone
  def deepClone(src: Object): Object = {
    var o: Object = null
    try {
      if (src != null) {
        val baos = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(baos)
        oos.writeObject(src)
        oos.close()
        val bais = new ByteArrayInputStream(baos.toByteArray())
        val ois = new ObjectInputStream(bais)
        o = ois.readObject()
        ois.close()
      }
    } catch {
      case e: Exception => logError("clone failed!", e.getCause)
    }
    return o
  }


  def convertDateFormat(time: java.util.Date): String = {
    val format: String = "yyyy-MM-dd HH:mm:ss SSS"
    val formatter = new SimpleDateFormat(format)
    formatter.format(time)
  }

  def convertDateFormat(time: Long, format: String = "yyyy-MM-dd HH:mm:ss SSS"): String = {
    val date = new java.util.Date(time)
    val formatter = new SimpleDateFormat(format)
    formatter.format(date)
  }

  def tryOrIOException(block: => Unit) {
    try {
      block
    } catch {
      case e: IOException => throw e
      case NonFatal(t) => throw new IOException(t)
    }
  }

  def isEnglish(charaString: String): Boolean = {
    charaString.matches("^[a-z|A-Z|\\s]*")
  }


  def isNumeric(str: String): Boolean = {
    for (i <- 0 until str.length()) {
      val chr = str.charAt(i)
      if (chr < 48 || chr > 57)
        return false
    }
    return true
  }

  def caculateCostTime(block: => Any): NiNi = {
    val startTime = System.currentTimeMillis()
    val result = block

    val endTime = System.currentTimeMillis()
    val costTime = (endTime - startTime).toFloat
    val costSecondTime = costTime / 1000f
    logInfo(s"search engine cost ${costTime} ms convert to second: ${costSecondTime} s")
    val nini = new NiNi(costTime, costSecondTime, result)
    if (result == null) {
      nini.setCode(-1)
      nini.setMsg("no data")
    }
    nini
  }

  def stringTotimestamp(time: String): Long = {
    val format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    //val d = format.format(time)
    val date = format.parse(time)
    date.getTime
  }

  def getContextClassLoader =
    Option(Thread.currentThread().getContextClassLoader).getOrElse(getClass.getClassLoader)

  def namedThreadFactory(prefix: String): ThreadFactory = {
    daemonThreadFactoryBuilder.setNameFormat(prefix + "-%d").build()
  }

  def newDaemonFixedThreadPool(nThreads: Int, prefix: String): ThreadPoolExecutor = {
    val threadFactory = namedThreadFactory(prefix)
    Executors.newFixedThreadPool(nThreads, threadFactory).asInstanceOf[ThreadPoolExecutor]
  }

  def inferCores(): Int = {
    Runtime.getRuntime.availableProcessors()
  }

  def regex(input: String, regex: String): Boolean = {
    if (regex == null || input == null) {
      return false
    }
    val p: Pattern = Pattern.compile(regex)
    val m: Matcher = p.matcher(input)

    if (m.find()) return true
    else return false
  }

  private lazy val localIpAddressNew: InetAddress = findLocalInetAddress()

  def localHostNameForURI(): String = {
    customHostname.getOrElse(InetAddresses.toUriString(localIpAddressNew))
  }


  private def findLocalInetAddress(): InetAddress = {
    val defaultIpOverride = System.getenv("SEARCH_LOCAL_IP")
    if (defaultIpOverride != null) {
      InetAddress.getByName(defaultIpOverride)
    } else {
      val address = InetAddress.getLocalHost
      if (address.isLoopbackAddress) {
        // Address resolves to something like 127.0.1.1, which happens on Debian; try to find
        // a better address using the local network interfaces
        // getNetworkInterfaces returns ifs in reverse order compared to ifconfig output order
        // on unix-like system. On windows, it returns in index order.
        // It's more proper to pick ip address following system output order.
        val activeNetworkIFs = NetworkInterface.getNetworkInterfaces.toList
        val reOrderedNetworkIFs = if (isWindows) activeNetworkIFs else activeNetworkIFs.reverse

        for (ni <- reOrderedNetworkIFs) {
          val addresses = ni.getInetAddresses.toList
            .filterNot(addr => addr.isLinkLocalAddress || addr.isLoopbackAddress)
          if (addresses.nonEmpty) {
            val addr = addresses.find(_.isInstanceOf[Inet4Address]).getOrElse(addresses.head)
            // because of Inet6Address.toHostName may add interface at the end if it knows about it
            val strippedAddress = InetAddress.getByAddress(addr.getAddress)
            // We've found an address that looks reasonable!
            log.warn("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
              " a loopback address: " + address.getHostAddress + "; using " +
              strippedAddress.getHostAddress + " instead (on interface " + ni.getName + ")")
            logWarning("Set SEARCH_LOCAL_IP if you need to bind to another address")
            return strippedAddress
          }
        }
        logWarning("Your hostname, " + InetAddress.getLocalHost.getHostName + " resolves to" +
          " a loopback address: " + address.getHostAddress + ", but we couldn't find any" +
          " external IP address!")
        logWarning("Set Crawler_LOCAL_IP if you need to bind to another address")
      }
      address
    }
  }


  def getFormattedClassName(obj: AnyRef): String = {
    obj.getClass.getSimpleName.replace("$", "")
  }


  val counter: LoadingCache[String, AtomicLong] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(2, TimeUnit.SECONDS)
      .build(new CacheLoader[String, AtomicLong]() {
        override def load(key: String): AtomicLong = {
          new AtomicLong(0)
        }
      })

  //limit the request count for single user
  def fluidControl(block: => AnyRef, key: String = null): AnyRef = {
    var cacheKey = System.currentTimeMillis() / 1000 + ""
    if (key != null)
      cacheKey = key + "_" + cacheKey
    if (counter.get(cacheKey).incrementAndGet() > fluidLimit) {
      "I'm sorry for this message,you should request slowly!"
    } else {
      block
    }
  }


  def getClassLoader: ClassLoader = getClass.getClassLoader


  def regexExtract(input: String, regex: String): AnyRef = regexExtract(input, regex, -2)


  def regexExtractSeq(input: String, regex: String): java.util.Collection[String] = {
    regexExtract(input, regex, -2).asInstanceOf[java.util.List[String]]
  }

  def splitRegex(input: String, regex: String): java.util.Collection[String] = {
    val splitArray = input.split(regex)
    splitArray.toList
  }

  def decimalFormat2(value: Double): Double = {
    val df = new DecimalFormat("#.##")
    df.format(value).toDouble
  }

  def regexSplitNoCharcter(str: StringBuffer, regex: String): java.util.Collection[String] = {
    val MIN_THRESHOLD = 50
    val MAX_THRESHOLD = 80
    val sb = new StringBuffer(MAX_THRESHOLD)
    val list = new java.util.ArrayList[String]()


    if (regex == null) {
      return null
    }
    val p = Pattern.compile(regex)
    val m = p.matcher(str)

    /*按照句子结束符分割句子*/
    val substrs1 = p.split(str)
    val substrs = new Array[String](substrs1.length)

    /*将句子结束符连接到相应的句子后*/
    if (substrs.length > 0) {
      var count = 0
      while (count < substrs.length) {
        if (m.find()) {
          substrs(count) = m.group()
        }
        count += 1
      }
    }
    //                //String [] substrs = str.split("[。？！?.!]");
    for (i <- 0 until substrs.length) {

      if (substrs(i).length() < MIN_THRESHOLD) {
        //语句小于要求的分割粒度
        sb.append(substrs(i))
        //sb.append("||");
        if (sb.length() > MIN_THRESHOLD) {
          //System.out.println("A New TU: " + sb.toString());
          list.add(sb.toString())
          sb.delete(0, sb.length())
        }
      }
      else {
        //语句满足要求的分割粒度
        if (sb.length() != 0) //此时如果缓存有内容则应该先将缓存存入再存substrs[i]的内容  以保证原文顺序
        {
          list.add(sb.toString())
          sb.delete(0, sb.length())
        }
        list.add(substrs(i))
      }
    }
    list
  }

  def regexSplit(str: StringBuffer, regex: String): java.util.Collection[String] = {
    val MIN_THRESHOLD = 50
    val MAX_THRESHOLD = 80
    val sb = new StringBuffer(MAX_THRESHOLD)
    val list = new java.util.ArrayList[String]()


    if (regex == null) {
      return null
    }
    val p = Pattern.compile(regex)
    val m = p.matcher(str)

    /*按照句子结束符分割句子*/
    val substrs = p.split(str)

    /*将句子结束符连接到相应的句子后*/
    if (substrs.length > 0) {
      var count = 0
      while (count < substrs.length) {
        if (m.find()) {
          substrs(count) += m.group()
        }
        count += 1
      }
    }
    //                //String [] substrs = str.split("[。？！?.!]");
    for (i <- 0 until substrs.length) {

      if (substrs(i).length() < MIN_THRESHOLD) {
        //语句小于要求的分割粒度
        sb.append(substrs(i))
        //sb.append("||");
        if (sb.length() > MIN_THRESHOLD) {
          //System.out.println("A New TU: " + sb.toString());
          list.add(sb.toString())
          sb.delete(0, sb.length())
        }
      }
      else {
        //语句满足要求的分割粒度
        if (sb.length() != 0) //此时如果缓存有内容则应该先将缓存存入再存substrs[i]的内容  以保证原文顺序
        {
          list.add(sb.toString())
          sb.delete(0, sb.length())
        }
        list.add(substrs(i))
      }
    }
    list
  }

  /*type Index = Int
  def binarySearch(ds: Array[TrieNode], key: Char): Option[Index] = {
    @tailrec
    def go(lo: Index, hi: Index): Option[Index] = {
      if (lo > hi)
        None
      else {
        val mid: Index = lo + (hi - lo) / 2
        ds(mid) match {
          case mv if (mv.character.equals(key) ) => Some(mid)
          case mv if (mv.character <= key) => go(mid + 1, hi)
          case _ => go(lo, mid - 1)
        }
      }
    }
    go(0, ds.size - 1)
  }*/

  def regexExtract(input: String, regex: String, group: Int = 0): Object = {
    if (regex == null) {
      return input
    }
    val p: Pattern = Pattern.compile(regex)
    val m: Matcher = p.matcher(input)
    if (group == -2) {
      val list = new java.util.ArrayList[String]

      while (m.find) {
        var i: Int = 1
        while (i <= m.groupCount) {
          {
            list.add(m.group(i))
          }
          i += 1
        }
      }
      return list
    } else {
      if (m.find) {
        if (group == -1) {
          var r = ""
          val s = new StringBuilder
          var i: Int = 1
          while (i <= m.groupCount) {
            {
              s ++= m.group(i)
              if (i != m.groupCount) {
                s ++= " "
              }
            }
            ({
              i += 1
            })
          }
          r = s.toString
          if (r.length == 0)
            r = m.group()
          return r
        }
        else {
          return m.group(group)
        }
      }
      else return ""
    }
  }
}

object testUtil {
  def main(args: Array[String]) {
    // testTime
    //testNumber
    //testRegex
    testFormat()
  }


  def testFormat() = {
    val result = Util.dataFomatStringYMD(new Date())
    println(result)
    println("20160919" > "20150920")
  }

  def testNumber = {
    println(Util.isNumeric("43345s"))
    println("is english:" + Util.isEnglish("LiuDeHUA"))
  }

  def testTime = {
    //println( Util.stringTotimestamp("2016-03-07 01:41:39.000"))
    // println(Util.timestampToDate(1457477786924L))
    println(Util.timestampToDate(1457759504865L))
  }

  def testRegex() = {
    val input = "A布局dg生态sd4"
    val regex = "(.*?)布局(.*?)生态(.*?)"
    println(Util.regex(input, regex))
  }
}