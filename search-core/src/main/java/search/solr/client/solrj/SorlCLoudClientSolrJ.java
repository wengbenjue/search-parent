package search.solr.client.solrj;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import search.common.entity.EntityJava;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by soledede on 2015/11/6.
 */
public class SorlCLoudClientSolrJ {
   //121.199.42.48
   static String zkHostString = "114.215.201.192:3213,121.40.241.26:3213,121.40.54.54:3213/solr";
   static CloudSolrClient server = null;
  // static String queryString = "q=*\\:*";


    public static void main(String[] args) {

        //search();
        searchMergeCloud();
    }

    public static void searchMergeCloud(){
        server = new CloudSolrClient(zkHostString);
        server.setDefaultCollection("searchcloud");
        server.setZkConnectTimeout(60000);
        server.setZkClientTimeout(60000);
        server.connect();
        // server.setParser(new XMLResponseParser());
        SolrQuery query = new SolrQuery();
        query.set("qt", "/select");
        query.setQuery("(original:d^50) OR (sku:d^50) OR (brand_ps:BDS/百得^30) OR (sku:*d*^11) OR (original:*d*^10) OR (text:d^2) OR (pinyin:d^0.002)");
        try {
            QueryResponse response = server.query(query);
            System.out.println(response);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void search() {
        server = new CloudSolrClient(zkHostString);
        server.setDefaultCollection("searchcloud");
        server.setZkConnectTimeout(60000);
        server.setZkClientTimeout(60000);
        server.connect();
        // server.setParser(new XMLResponseParser());
        SolrQuery query = new SolrQuery();
       // query.setQuery(queryString);
       // query.setRequestHandler("/select");

       // query.set("fl", "category,title,price");
        //query.setFields("category", "title", "price");
        query.set("qt", "/select");
        //query.set("fq","secondLevelCategory:工具车及箱包");
        //query.setFacet(true);
       // query.
        //query.setFacetMinCount(1);
       // query.setFacetMissing(false);
        //query.setFields("int1_ti");
        //query.set("facet.field","{!ex=tagForString1_s}string1_s");
        query.set("q", "*:*");
        query.set("fq","topLevelCategory:工具");
        query.set("fq","secondLevelCategory:工具车及箱包");
        query.set("fq","thirdLevelCategory:工具箱");
        query.set("fq","fourthLevelCategory:塑料工具箱");
        query.set("fq","{!tag='tagForBrand_s,tagForString1_s,tagForString2_s,tagForString3_s,tagForString4_s,tagForInt1_ti,tagForInt2_ti'}brand_s:ENDURA/力易得");

        query.set("facet",true);
        query.set("facet.mincount",1);
        query.set("facet.field","{!ex=tagForInt1_ti}int1_ti&facet.field={!ex=tagForInt2_ti}int2_ti&facet.field={!ex=tagForString1_s}string1_s&facet.field={!ex=tagForString2_s}string2_s&facet.field={!ex=tagForString3_s}string3_s&{!ex=tagForString4_s}facet.field=string4_s&facet.field={!ex=tagForBrand_s}brand_s");

        //query.set("fq","secondLevelCategory:工具车及箱包");
        query.setRows(5);
        query.setStart(0);
        try {
            List<String> s = new ArrayList<String>();
           // QueryResponse response = server.query(query);

            QueryResponse response = server.query(query);
            List<EntityJava> entitys = response.getBeans(EntityJava.class);
            for(EntityJava e : entitys){
                System.out.println(e.toString()+e.getOriginal());
            }
           // SolrDocumentList list = response.getResults();
            System.out.println("size:"+entitys.size());
           // for(SolrDocument d: list){
                //System.out.println(d.getFieldNames().toString());

           // }
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
