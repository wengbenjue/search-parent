package search.solr.client.test

import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.client.solrj.{SolrQuery}
import org.apache.solr.common.{SolrInputDocument, SolrDocument, SolrDocumentList}
import search.solr.client.solrj.test.SorlTestJava
import search.solr.client.user.SorlClientUI
import search.solr.client.{SolrClient, SolrClientConf}
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._


/**
  * Created by soledede on 2015/11/16.
  */
object SolrJTest {
  val client = SorlClientUI.singleInstanceUI;

  def main(args: Array[String]) {
    testJavaObj()
  }

  def testJavaObj()={
    println(search.solr.client.solrj.test.SorlTestJava.objs.getClass)
    println(search.solr.client.solrj.test.SorlTestJava.objs.toString)
    val objArray = SorlTestJava.objs
    if(objArray.isInstanceOf[Array[Object]]){
      val sA =objArray.asInstanceOf[Array[Object]]
      println(sA(1))
    }

  }

  def testUpdateIndicesByDoc() = {
    val doc = new SolrInputDocument
    doc.setField("id","7436001")
    val setField =  new java.util.LinkedHashMap[java.lang.String,Object]
    setField("set")="测试原子更新，8天"
    doc.addField("leadTime",setField)
    client.updateIndexByDoc(doc)
    client.close()
  }

  def testSearchByQuery() = {
    val query: SolrQuery = new SolrQuery
    query.set("qt", "/select")
    query.add("q", "*:*")
    query.add("fq", "topLevelCategory:工具")
    query.add("fq", "secondLevelCategory:工具车及箱包")
    query.add("fq", "thirdLevelCategory:工具箱")
    query.add("fq", "fourthLevelCategory:塑料工具箱")
    query.add("fq", "{!tag='tagForBrand_s,tagForString1_s,tagForString2_s,tagForString3_s,tagForString4_s,tagForInt1_ti,tagForInt2_ti'}brand_s:ENDURA/力易得")
    query.setFacet(true)
    query.setFacetMinCount(1)
    query.addFacetField("{!ex=tagForInt2_ti}int2_ti","{!ex=tagForString1_s}string1_s","{!ex=tagForString2_s}string2_s","{!ex=tagForString3_s}string3_s","{!ex=tagForString4_s}string4_s","{!ex=tagForBrand_s}brand_s")
    query.setRows(10)
    query.setStart(0)
    val r =client.searchByQuery(query).asInstanceOf[QueryResponse]
    client.close()
    r.getResults.foreach(println)
  }
}
