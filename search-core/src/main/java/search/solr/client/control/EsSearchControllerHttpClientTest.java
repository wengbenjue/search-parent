package search.solr.client.control;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import search.common.entity.bizesinterface.IndexObjEntity;
import search.common.http.HttpClientUtil;
import search.common.entity.searchinterface.parameter.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by soledede on 2016/3/2.
 */
public class EsSearchControllerHttpClientTest {

    public static void main(String[] args) {
        //testSearchByKeywords();
        //testIndexByKeywords();
        //testRecordLogs();
        //testShowStateByQuery();
        testDelIndexByKeywords();
        //testDelNamespaceFromRedis();
        //testIndexByKeywordsWithRw();
    }

    public static void testRecordLogs() {
        String url = "http://localhost:8999/search/record";

        RecordLogParameter obj = new RecordLogParameter("誉衡药业", "sd", "0.0.0", "sdf", "haha", "sdjkl", "userId");

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }

    public static void testIndexAll() {
        String url = "http://localhost:8999/es/index/all/?indexName=nlp&typeName=graph";

        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }

    public static void testdeleteMongo() {
        String url = "http://localhost:8999/search/del/mongo/index";

        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "get", null, null);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }


    public static void testIndexByKeywords() {
        String url = "http://localhost:8999/es/index/keywords";
        //String url = "http://54.222.222.172:8999/es/index/keywords";

        IndexKeywordsParameter obj = new IndexKeywordsParameter();
        List keywords = new ArrayList<String>();
        keywords.add("低空飞行");
        obj.setKeywords(keywords);
        //obj.setOriginQuery("无人机对话");

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }

    public static void testIndexByKeywordsWithRw() {
        String url = "http://localhost:8999/es/index/rws";
        //String url = "http://54.222.222.172:8999/es/index/keywords";

        List<IndexObjEntity> keywords = new ArrayList<IndexObjEntity>();
        IndexObjEntity kv = new IndexObjEntity();
        keywords.add(kv);
        kv.setKeyword("体天");
        kv.setRvkw("klw7");

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", keywords, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }

    public static void testDelIndexByKeywords() {
        String url = "http://localhost:8999/es/delete/keywords";

        IndexKeywordsParameter obj = new IndexKeywordsParameter();
        List keywords = new ArrayList<String>();
        keywords.add("机动车贸易");
        keywords.add("注入资产");
        obj.setKeywords(keywords);

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }


    public static void testSearchByKeywords() {
        //String url = "http://54.222.222.172:8999/es/search/keywords";
        // String url = "http://localhost:8999/es/search/keywords";
        String url = "http://192.168.100.12:8999/es/search/keywords";

        KnowledgeGraphParameter obj = new KnowledgeGraphParameter();
        obj.setKeyword("百度");

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }

    public static void testShowStateByQuery() {
        String url = "http://localhost:8999/es/search/state";

       /* KnowledgeGraphParameter obj = new KnowledgeGraphParameter();
        obj.setKeyword("人类基因组");*/
        Map obj = new java.util.HashMap<String, String>();
        obj.put("keyword", "kfc");

        Map headers = new java.util.HashMap<String, String>();
        //headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, null);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }

    public static void testDelNamespaceFromRedis() {
        String url = "http://localhost:8999/es/search/clean/cacheredis";

        Map obj = new java.util.HashMap<String, String>();
        obj.put("namespace", "graph_state");

        Map headers = new java.util.HashMap<String, String>();
        //headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, null);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }


    public static void testDelAllMongoData() {
        String url = "http://localhost:8999/es/search/del/mongo/index";
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", null, null);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }
}
