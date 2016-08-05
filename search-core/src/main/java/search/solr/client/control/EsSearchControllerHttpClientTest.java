package search.solr.client.control;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
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
    }

    public static void testRecordLogs() {
        String url = "http://localhost:8999/search/record";

        RecordLogParameter obj = new RecordLogParameter("誉衡药业", "sd", "0.0.0", "sdf", "haha", "sdjkl", "userId");

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp  = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
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

    public static void testIndexByKeywords() {
        String url = "http://localhost:8999/es/index/keywords";

        IndexKeywordsParameter obj = new IndexKeywordsParameter();
        List keywords = new ArrayList<String>();
        keywords.add("无人机");
        obj.setKeywords(keywords);
        obj.setOriginQuery("无人机对话");

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

    public static void testDelIndexByKeywords() {
        String url = "http://localhost:8999/es/delete/keywords";

        IndexKeywordsParameter obj = new IndexKeywordsParameter();
        List keywords = new ArrayList<String>();
        keywords.add("机动车贸易");
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
        obj.put("keyword","人类基因组");

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
}
