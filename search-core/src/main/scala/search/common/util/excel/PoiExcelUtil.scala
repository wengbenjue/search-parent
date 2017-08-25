package search.common.util.excel

import java.io.{File, FileOutputStream}
import java.util
import java.util.Date

import org.apache.poi.hssf.usermodel.HSSFCellStyle
import org.apache.poi.hssf.util.HSSFColor
import org.apache.poi.ss.usermodel.{BorderStyle, CellStyle, FillPatternType, HorizontalAlignment}
import org.apache.poi.xssf.streaming.SXSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import search.common.config.EsConfiguration
import search.common.util.{Logging, Util}
import search.es.client.biz.BizeEsInterface

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.io.Source

/**
  * Created by soledede.weng on 2016/9/27.
  */
private[search] object PoiExcelUtil extends Logging with EsConfiguration {


  def writeNewsExcel() = {
    val pageSize = 3000
    val flushSize = 100
    var flushFactor = 0

    var fileOut = new FileOutputStream(newExcelPath)

    //创建工作簿
    //val wb = new XSSFWorkbook()
   val wb = new SXSSFWorkbook()
    // 创建工作表
    val sheet = wb.createSheet("news")
    val createHelper = wb.getCreationHelper()

    //设置Excel中的边框
    val cellStyle = wb.createCellStyle()
    /*cellStyle.setAlignment(HorizontalAlignment.CENTER)
    cellStyle.setBorderBottom(BorderStyle.MEDIUM)
    cellStyle.setBottomBorderColor(HSSFColor.BLACK.index)
    cellStyle.setBorderLeft(BorderStyle.MEDIUM)
    cellStyle.setLeftBorderColor(HSSFColor.BLACK.index);
    cellStyle.setBorderRight(BorderStyle.MEDIUM)
    cellStyle.setRightBorderColor(HSSFColor.BLACK.index)
    cellStyle.setBorderTop(BorderStyle.MEDIUM)
    cellStyle.setTopBorderColor(HSSFColor.BLACK.index)*/


    cellStyle.setAlignment(CellStyle.ALIGN_CENTER)
    cellStyle.setBorderBottom(CellStyle.BORDER_MEDIUM)
    cellStyle.setBottomBorderColor(HSSFColor.BLACK.index)
    cellStyle.setBorderLeft(CellStyle.BORDER_MEDIUM)
    cellStyle.setLeftBorderColor(HSSFColor.BLACK.index);
    cellStyle.setBorderRight(CellStyle.BORDER_MEDIUM)
    cellStyle.setRightBorderColor(HSSFColor.BLACK.index)
    cellStyle.setBorderTop(CellStyle.BORDER_MEDIUM)
    cellStyle.setTopBorderColor(HSSFColor.BLACK.index)

    // 创建标题行
    var row = sheet.createRow(0)
    var cell = row.createCell(0)
    cell.setCellStyle(cellStyle)
    cell.setCellValue("查询词")
    cell = row.createCell(1)
    cell.setCellValue("count")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(2)
    cell.setCellValue("新闻标题")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(3)
    cell.setCellValue("url")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(4)
    cell.setCellValue("摘要")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(5)
    cell.setCellValue("相关公司")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(6)
    cell.setCellValue("相关词")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(7)
    cell.setCellValue("相关概念")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(8)
    cell.setCellValue("相关事件")
    cell.setCellStyle(cellStyle)
    cell = row.createCell(9)
    cell.setCellValue("创建时间")
    cell.setCellStyle(cellStyle)


    //设置列宽
    sheet.setColumnWidth(0, 2500)
    sheet.setColumnWidth(1, 2500)
    sheet.setColumnWidth(2, 12500)
    sheet.setColumnWidth(3, 18500)
    sheet.setColumnWidth(4, 25000)
    sheet.setColumnWidth(5, 7500)
    sheet.setColumnWidth(6, 7500)
    sheet.setColumnWidth(7, 7500)
    sheet.setColumnWidth(8, 7500)
    sheet.setColumnWidth(9, 7500)

    logInfo("开始写入Excel")

    //记录行数
    var cnt = 1
    var cnt_exec = 1
    logInfo(s"当前写入行数(除表头外):${cnt}")

    def queryStrings(): Seq[String] = {
      val queryPath = "D:\\java\\new_query.txt"
     val queryList = Source.fromFile(new File(queryPath),"GB2312").getLines.toList
      queryList
      //Seq("股票")
    }

    queryStrings.foreach(queryNews(_))

    wb.write(fileOut)
    fileOut.close()
    logInfo(s"写入Excel完成,path:${newExcelPath}")

    def queryNews(query: String): Unit = {



      logInfo(s"查询并写入query:${query}")
      var result: Array[java.util.Map[String, AnyRef]] = null
      var count: Int = 0
      var queryResult = BizeEsInterface.queryNews(query, 0, 0, 6)
      if (queryResult != null) {
        count = queryResult.getCount
        if (count > 0) {
          val page = if (count % pageSize == 0) (count / pageSize) else (count / pageSize) + 1

          //分页查询
          for (i <- 0 until page) {
            if (i == page - 1) {
              if (count % pageSize != 0) {
                queryResult = BizeEsInterface.queryNews(query, i * pageSize, pageSize, 6)
              } else queryResult = BizeEsInterface.queryNews(query, i * pageSize, count % pageSize, 6)
            } else
              queryResult = BizeEsInterface.queryNews(query, i * pageSize, pageSize, 6)


            if (queryResult != null) {
              result = queryResult.getResult
              if (result != null && result.size > 0) {
                if (i == 0) writeKVNewsToExcel(query, count, result)
                else writeKVNewsToExcel(null, -1, result)
              }
            }
          }
        }

      }

    }



    def writeKVNewsToExcel(query: String, count: Int, news: Array[java.util.Map[String, AnyRef]]) = {
      logInfo(s"当前写入行数(除表头外):${cnt}")
      if (query != null) {
        row = sheet.createRow(cnt)
        cnt += 1
        cnt_exec += 1
        val cell = row.createCell(0)
        val style = wb.createCellStyle()

        style.setFillForegroundColor(HSSFColor.RED.index)
        //style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
        style.setFillPattern(CellStyle.SOLID_FOREGROUND)


        cell.setCellStyle(style)
        cell.setCellValue(query)
        row.createCell(1).setCellValue(count)
      }


      if(cnt_exec>=flushSize){
        flushFactor += 1
        for(i <- flushFactor*flushSize until flushFactor*flushSize+flushSize){
          logInfo(s"flush rownum: ${i}, ${sheet.getRow(i)}")
        }
        logInfo(s"刷新当前批次${flushSize}到磁盘！")
        cnt_exec= 0
      }

      news.foreach { newsMap =>
        val title = newsMap.getOrDefault("title", "").toString
        val url = newsMap.getOrDefault("url", "").toString
        //摘要
        var summary = newsMap.getOrDefault("summary", "").toString
        summary = summary.replaceFirst("#&#", "")
        //相关公司
        val companys = newsMap.getOrDefault("companys", null)
        var companysString: String = ""
        if (companys != null) {
          val companysList = companys.asInstanceOf[util.Collection[String]]
          companysString = companysList.mkString(",")
        }

        //相关词
        val kws = newsMap.getOrDefault("kw", null)
        var kwString: String = ""
        if (kws != null) {
          val kwsList = kws.asInstanceOf[util.Collection[String]]
          kwString = kwsList.mkString(",")
        }

        //相关概念
        val topics = newsMap.getOrDefault("topics", null)
        var topicsString: String = ""
        if (topics != null) {
          val topicsList = topics.asInstanceOf[util.Collection[String]]
          topicsString = topicsList.mkString(",")
        }

        //相关事件
        val events = newsMap.getOrDefault("events", null)
        var eventsString: String = ""
        if (events != null) {
          val eventsList = events.asInstanceOf[util.Collection[String]]
          eventsString = eventsList.mkString(",")
        }
        //创建时间
        val create_on = newsMap.getOrDefault("create_on", "").toString
        // 添加数据行
        row = sheet.createRow(cnt)
        cnt += 1
        cnt_exec += 1

        try {
          row.createCell(2).setCellValue(title)
          row.createCell(3).setCellValue(url)
          if (summary.length >= 32767) summary = summary.substring(0, 32766)
          row.createCell(4).setCellValue(summary)
          row.createCell(5).setCellValue(companysString)
          row.createCell(6).setCellValue(kwString)
          row.createCell(7).setCellValue(topicsString)
          row.createCell(8).setCellValue(eventsString)
          row.createCell(9).setCellValue(create_on)
        } catch {
          case e: Exception => logError("row failed",e)
        }
      }

    }
  }

  def main(args: Array[String]) {
    PoiExcelUtil.writeNewsExcel()
  }

}
