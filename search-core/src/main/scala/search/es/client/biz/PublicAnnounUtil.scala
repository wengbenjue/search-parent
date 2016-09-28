package search.es.client.biz

import java.io.File
import java.util

import search.common.util.Util

import scala.io.Source

/**
  * Created by soledede.weng on 2016/9/28.
  */
private[search] object PublicAnnounUtil {


  def loadAllPdfTxt(): java.util.Collection[java.util.Map[String, Object]] = {
    val listPdf = new util.ArrayList[java.util.Map[String, Object]]()
    val dirPath = "D:\\announce\\Text\\2015\\YEAR"
    val dir = new File(dirPath)
    val files = dir.listFiles().filter(_.isFile)
    files.foreach { f =>
      val mapPdf = loadAnnounceDataFromPdfTxt(f.getAbsolutePath)
      if (!mapPdf.isEmpty) listPdf.add(mapPdf)
    }
    listPdf
  }

  def loadAnnounceDataFromPdfTxt(path: String): java.util.Map[String, Object] = {
    val map = new util.HashMap[String, Object]()
    var lineNume = 0
    var flag = false
    var secondFlag = false
    val content = new StringBuilder
    var id = path.replaceAll(".text.txt", "")
    id = id.substring(id.lastIndexOf(File.separator) + 1)
    map.put("id", id)
    var companyName = ""
    var companySim = ""
    var companyCode = ""
    for (line <- Source.fromFile(path).getLines) {
      if (!line.trim.equalsIgnoreCase("") && !line.startsWith("---") && !line.startsWith("===")) {
        var lineCom = line.replaceAll("(\\|[0-9|.]+,[0-9|.]+,[0-9|.]+,[0-9|.]+\\|)", "")
        lineCom = lineCom.replaceAll("(\\[[0-9|.]+,[0-9|.]+,[0-9|.]+,[0-9|.]+\\])", "")
        content.append(lineCom)
        if (lineNume == 1) {
          if (!lineCom.contains("年度报告")) {
            val comArray = lineCom.split("\\|")
            if (comArray.length > 1) {
              //有公司代码和公司简称
              val comCodeArray = comArray(0)
              val comSimArray = comArray(1)

              var comCode = comCodeArray.split("：")
              if (comCode.length < 2) {
                comCode = comCodeArray.split(":")
              }
              if (comCode.length == 2) {
                companyCode = comCode(1).trim
              }

              var comSim = comSimArray.split("：")
              if (comSim.length < 2) {
                comSim = comSimArray.split(":")
              }
              if (comSim.length == 2) {
                companySim = comSim(1).trim
              }

            } else {
              companyName = lineCom.trim
              flag = true
              secondFlag = true
            }
          } else {
            flag = true
            secondFlag = true
          }
        } else if (lineNume == 2) {
          if (!flag) {
            val comArray = lineCom.split("\\|")
            if (comArray.length > 1) {
              //有公司代码和公司简称
            } else {
              secondFlag = true
              //有公司全称
              companyName = lineCom.trim
            }

          }
        } else if (lineNume == 3) {
          if (!secondFlag) {
            //有公司全称
            companyName = lineCom.trim
          }
        }
        if (lineCom.startsWith("股票代码") || lineCom.startsWith("证券代码")) {
          var comCode = lineCom.split("：")
          if (comCode.length < 2) {
            comCode = lineCom.split(":")
          }
          if (comCode.length == 2) {
            companyCode = comCode(1).trim
          }
        }
        if (lineCom.startsWith("股票简称") || lineCom.startsWith("证券简称")) {
          var comSim = lineCom.split("：")
          if (comSim.length < 2) {
            comSim = lineCom.split(":")
          }
          if (comSim.length == 2) {
            companySim = comSim(1).trim
          }
        }

      }
      lineNume += 1
    }

    if (companyCode != null && !companyCode.equalsIgnoreCase(""))
      map.put("company_code", companyCode)
    if (companyName != null && !companyName.equalsIgnoreCase(""))
      map.put("company", companyName)
    if (companySim != null && !companySim.equalsIgnoreCase(""))
      map.put("company_sim", companySim)
    if (content != null && !content.isEmpty)
      map.put("content", content.toString())
    map
  }


  def main(args: Array[String]) {
    testReadFilesFromDir
  }

  def testReadFilesFromDir() = {
    val dirPath = "D:\\announce\\Text\\2015\\YEAR"
    val dir = new File(dirPath)
    val files = dir.listFiles().filter(_.isFile)
    files.foreach(f => println(f.getAbsolutePath))
  }

  def testLoadAnnounceDataFromPdfTxt() = {
    //val file = "D:\\announce\\1ACCFD96591F10DD4EF66DC45D014922.text.txt"
    //val file = "D:\\announce\\1BCCC53C2EB2E33FD505CC38E3F83B0E.text.txt"
    // val file = "D:\\announce\\1BFC2F7FA456000C6CAD14CA98FE2F21.text.txt"
    val file = "D:\\announce\\4A1CA500625117B82DBAC2C7C06BBFDB.text.txt"
    loadAnnounceDataFromPdfTxt(file)
  }


  def regexTest = {
    var input = "[1,10.6,89.9,84.9]公司A"
    input = input.replaceAll("(\\[[0-9|.]+,[0-9|.]+,[0-9|.]+,[0-9|.]+\\])", "")
    println(input)
  }

}
