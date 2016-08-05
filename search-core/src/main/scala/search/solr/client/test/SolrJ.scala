package  search.solr.client.test

import org.apache.solr.client.solrj.SolrQuery
import org.apache.solr.client.solrj.impl.CloudSolrClient
import org.apache.solr.client.solrj.response.QueryResponse
import org.apache.solr.common.SolrDocumentList
import search.common.entity.EntityJava
import scala.collection.JavaConversions._

object SolrJ {


  // static String queryString = "q=*\\:*";
  def main(args: Array[String]) {
    search
  }





  def search {
    var zkHostString: String = "114.215.201.192:3213,121.40.241.26:3213,121.40.54.54:3213/solr"
     var server: CloudSolrClient = new CloudSolrClient(zkHostString)
    server.setDefaultCollection("searchcloud")
    server.setZkConnectTimeout(60000)
    server.setZkClientTimeout(60000)
    server.connect

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
    try {
      //val s: util.Seq[String] = new util.Lis[String]
      val response: QueryResponse = server.query(query)
      //val entitys: Seq[EntityJava] = response.getBeans(classOf[EntityJava])
      val docList: SolrDocumentList = response.getResults

      docList.foreach(println)
      println("\n")
      val fs = response.getFacetFields
      println(fs.size())
      fs.foreach(f=>println(f.getName+":"+f.getValues+"\n"))
      //val factPivot = response.getFacetPivot
      //factPivot.foreach()
    /*  for (e <- entitys) {
        System.out.println(e.toString + e.original + "\n" + e.sku)
      }*/


      //System.out.println("size:" + entitys.size)
    }
    catch {
      case e: Exception => {
        e.printStackTrace
      }

    } finally {
      try {

      }
      catch {
        case e: Exception => {
          e.printStackTrace
        }
      }
    }
  }
}