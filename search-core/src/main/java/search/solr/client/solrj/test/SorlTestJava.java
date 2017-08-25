package search.solr.client.solrj.test;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import search.common.entity.IndexResult;
import search.solr.client.user.SorlClientUI;
import search.common.entity.Automtic;

import java.util.*;

/**
 * Created by soledede on 2015/11/16.
 */
public class SorlTestJava {
    static SorlClientUI client = SorlClientUI.singleInstanceUI();

    public static void main(String[] args) {
       // testAsynUpdateIndicesByMapSet();
       // client.close();
        //noQuto();
        addIndices();
    }


    public static void noQuto(){
        String json = "\"value\"";
        String t = json.replaceAll("\"(\\w+)\"", "$1");
        System.out.println(t);
    }


    public static void addIndices() {
        //no boost
        List<Map<String, Object>> l = new ArrayList<java.util.Map<java.lang.String, Object>>();
        Map<String, Object> tzar = new HashMap<java.lang.String, Object>();
        tzar.put("id", "t343455");
        //tzar.put("location","40.715,-74.007");

        //have boost
       // Object[] objs = new Object[]{"添加索引boost测试中...unknown brand...", 10f};
        //tzar.put("brand", objs);

        l.add(tzar);

        client.addIndices(l,"mergecloud");
        client.close();
    }

    //测试异步更新索引
    public static void testAsynUpdateIndicesByMapSet() {
        //no boost
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        Map<String, Object> tzar = new HashMap<String, Object>();
        tzar.put("id", "7436001");
        Map<String, Object> set = new HashMap<String, Object>();
        set.put(Automtic.SET(), "Map测试原子更新，100天");
        tzar.put("leadTime", set);

        //have boost
        Object[] objs = new Object[]{Automtic.SET(), "原子更新boost测试中...unknown brand...", 4.6f};
        tzar.put("brand", objs);

        l.add(tzar);

        client.asynUpdateIndicesByMapSet(l, new FutureCallback<IndexResult>() {
            @Override
            public void onSuccess(IndexResult result) {
                System.out.println(result.code() + "\n" + result.msg() + "\n" + result.error());
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        });

        client.close();
    }

    public static void testUpdateIndicsByMap() {

        //no boost
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        Map<String, Object> tzar = new HashMap<String, Object>();
        tzar.put("id", "7436001");
        Map<String, Object> set = new HashMap<String, Object>();
        set.put(Automtic.SET(), "Map测试原子更新，88天");
        tzar.put("leadTime", set);

        //have boost
        Object[] objs = new Object[]{Automtic.SET(), "原子更新boost测试中...unknown brand...", 4.6f};
        tzar.put("brand", objs);

        l.add(tzar);

        client.updateIndicesByMapSet(l);
        client.close();
    }


    public static void testUpdateIndicesByDoc() {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", "7436001");
        Map<String, Object> setField = new LinkedHashMap<String, Object>();
        setField.put("set", "Java测试原子更新，250天");
        doc.addField("leadTime", setField);
        client.updateIndexByDoc(doc);
        client.close();
    }

    public static void testSearchByQuery() {
        SolrQuery query = new SolrQuery();
        query.set("qt", "/select");
        query.add("q", "*:*");
        query.add("fq", "topLevelCategory:工具");
        query.add("fq", "secondLevelCategory:工具车及箱包");
        query.add("fq", "thirdLevelCategory:工具箱");
        query.add("fq", "fourthLevelCategory:塑料工具箱");
        query.add("fq", "{!tag='tagForBrand_s,tagForString1_s,tagForString2_s,tagForString3_s,tagForString4_s,tagForInt1_ti,tagForInt2_ti'}brand_s:ENDURA/力易得");
        query.setFacet(true);
        query.setFacetMinCount(1);
        query.addFacetField("{!ex=tagForInt2_ti}int2_ti", "{!ex=tagForString1_s}string1_s", "{!ex=tagForString2_s}string2_s", "{!ex=tagForString3_s}string3_s", "{!ex=tagForString4_s}string4_s", "{!ex=tagForBrand_s}brand_s");
        query.setRows(10);
        query.setStart(0);
        QueryResponse dL = client.searchByQuery(query, "searchcloud");
        System.out.println(dL.getResults().size());
    }

    public static Object[] objs = new Object[]{1, "zhong", "w"};
}
