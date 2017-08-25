package search.solr.client.index.gene.sql

import java.io.PrintWriter
import java.sql.{DriverManager, Connection}
import java.util.Date

import search.solr.client.index.entity.CategoryFacet
import search.common.util.Util

import scala.collection.mutable.ListBuffer
import scala.reflect.io.File

/**
  * Created by soledede on 2015/11/25.
  */
object ScalaJdbcConnect {


//_${Util.dateToString(new Date())}
  val filePath: String = s"D:\\search\\datasource\\categoryfacet\\dataimport\\categoryFacet.xml"


  def main(args: Array[String]) {
    sqlSelect
  }


  def sqlSelect() = {
    // connect to the database named "mysql" on the localhost
    val driver = "net.sourceforge.jtds.jdbc.Driver"
    val url = "jdbc:jtds:sqlserver://xxxxx/xxxxx"
    val username = "xxxx"
    val password = "xxxxx"

    // there's probably a better way to do this
    var connection: Connection = null

    try {
      // make the connection
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)

      // create the statement, and run the select query
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SELECT DISTINCT CAST(tt.catid AS varchar(10))+'_'+tt.filter_id as id,* FROM (select  DISTINCT CAST(b.attribute_id AS varchar(10))+'_s' filter_id,a.tree_id+1000000 catid,b.attribute_desc_zh,'' value_zh,a.seq,0 seq_in from PICategoryAttribute a inner join PIAttributeMaster b on a.attribute_id=b.attribute_id inner join PIItemAttributeTech d on b.attribute_id=d.attribute_id inner join PIItemMaster e on d.item_sku=e.item_sku and a.tree_id=e.category inner join SyncSkuToWebsite sstw on d.item_sku=sstw.item_sku where a.filter_type=1 and e.company_id=12 and d.value_zh is not null and d.value_zh!='' union all select CAST(b.attribute_id AS varchar(10))+'_r' filter_id,a.tree_id+1000000 catid,b.attribute_desc_zh,c.RANGE,a.seq,c.seq from PICategoryAttribute a inner join PIAttributeMaster b on a.attribute_id=b.attribute_id right join PICategoryAttributeVR c on a.tree_id=c.tree_id and a.attribute_id=c.attribute_id inner join PIItemAttributeTech d on b.attribute_id=d.attribute_id inner join PIItemMaster e on d.item_sku=e.item_sku and a.tree_id=e.category inner join SyncSkuToWebsite sstw on e.item_sku=sstw.item_sku where a.filter_type=2 and e.company_id=12 ) AS tt ORDER BY catid,seq,seq_in")
      val list  = new ListBuffer[CategoryFacet]
      while (resultSet.next()) {
        val id = resultSet.getString("id")
        val filterId_s = resultSet.getString("filter_id")
        val catid_s = resultSet.getString("catid")
        val attDescZh_s = resultSet.getString("attribute_desc_zh")
        val range_s = resultSet.getString("value_zh")
        val attSort_ti = resultSet.getInt("seq")
        val rangeSort_ti = resultSet.getInt("seq_in")
        val categoryFacet = new CategoryFacet(id,catid_s, filterId_s, attDescZh_s, range_s, attSort_ti, rangeSort_ti)
        list += categoryFacet
        println(s"id=$id, filter_id=$filterId_s, catid =$id attribute_desc_zh=$attDescZh_s  value_zh=$range_s seq=$attSort_ti seq_in=$rangeSort_ti")
      }
      generateAddIndexXmlList(list,filePath)
    } catch {
      case e => e.printStackTrace
    }
    connection.close()
  }

  private def generateAddIndexXmlList(list : Seq[CategoryFacet],filePath: String) = {
    val xml = new StringBuilder
    xml.append("<add>")
    xml.append("\n")
    list.foreach(
      generateAddIndex(_,xml)
    )
    xml.append("</add>")
    writeToDisk(xml.toString(),filePath)
    println(s"写入文件 $filePath 成功,总共${list.size}个文档！")
  }

  private def generateAddIndex(entity: Object,xml: StringBuilder) = {
    xml.append("<doc>")
    xml.append("\n")
    val clazz = entity.getClass
    val fields = clazz.getDeclaredFields
    val methods = clazz.getMethods
    fields.foreach{f =>
      val fieldName = f.getName
      val fieldValue = invokeMethod(entity,fieldName, null)
      if(fieldValue!=null && !fieldValue.toString.trim.equalsIgnoreCase("")) {
        xml.append("<field name=\"")
        xml.append(fieldName)
        xml.append("\">")
        xml.append(fieldValue)
        xml.append("</field>")
        xml.append("\n")
      }
    }
    xml.append("</doc>")
    xml.append("\n")
  }

  private def writeToDisk(xml: String,filePath: String)={
    val w = new PrintWriter(filePath)
    w.println(xml)
    w.close()
  }

  private def invokeMethod(obj: Object,fieldName: String,args:Array[Object]) = {
   val clazz = obj.getClass
  val method = clazz.getMethod(fieldName)
    method.invoke(obj)
  }

}
