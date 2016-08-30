package search.solr.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import search.common.entity.searchinterface.parameter.IndexParameter;
import search.common.entity.searchinterface.parameter.RecordLogParameter;
import search.common.entity.searchinterface.parameter.SearchRequestParameter;
import search.common.http.HttpClientUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by soledede on 2016/3/2.
 */
public class HttpClientTest {

    public static void main(String[] args) {
        //  testHttp();
       // testSearchByKeywords();
       // testGetCategoryIdsByKeyWords();
        //testPopularityKeyWords();
       // testSuggestByKeyWords();
        //testAttributeFilterSearchCatId(true);

        //testAttributeFilterSearchCatId(false);
        //testAttributeFilterSearch(); //for search
       // testSearchBrandsByCatoryId();

        //index();
        // deleteIds();
        // testRecordLogs();
        //testCrawlerTrigger();
    }

    public static void testSynomy(){


    }

    public static void testRecordLogs(){
        String url = "http://192.168.51.118:8999/search/record";

        RecordLogParameter obj = new RecordLogParameter("工具", "sd", "0.0.0", "sdf", "haha", "sdjkl", "userId");

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
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


    public static void testCrawlerTrigger() {
        String url = "http://192.168.100.20:5000/?kw=王老吉";



        Map headers = null;
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "get", null,headers);
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


    public static void deleteIds() {
        String url = "http://192.168.51.118:8999/search/delete/ids";

        IndexParameter obj = new IndexParameter();
        obj.setCollection("mergescloud");
        List<String> list = new ArrayList<String>();
        list.add("23");
        obj.setIds(list);
        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
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


    public static void index() {
        String url = "http://192.168.51.118:8999/search/index";


        IndexParameter obj = new IndexParameter();
        obj.setCollection("mergescloud");
        obj.setStartUpdateTime(1900990435L);
        obj.setEndUpdataTime(2343242L);
        obj.setTotalNum(4);

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
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


    public static void testAttributeFilterSearch() {
        String url = "http://192.168.51.118:8999/search/filter/search";


        LinkedHashMap<java.lang.String, java.lang.String> sorts = new java.util.LinkedHashMap<java.lang.String, java.lang.String>();
        sorts.put("price", "asc");

        Map<java.lang.String, java.lang.String> filters = new java.util.HashMap<java.lang.String, java.lang.String>();
        filters.put("da_89_s", "TIME/时代");
        // filters.put("t87_tf", "[0 TO *}");

        LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> filterFieldsValues = new java.util.LinkedHashMap<java.lang.String, java.util.List<java.lang.String>>();
        filterFieldsValues.put("da_89_s", null);
        filterFieldsValues.put("da_2955_s", null);
        List rangeList = new java.util.ArrayList<String>();
        rangeList.add("[* TO 0}");
        rangeList.add("[0 TO 10}");
        rangeList.add("[10 TO 20}");
        rangeList.add("[20 TO 30}");
        rangeList.add("[30 TO *}");
        // filterFieldsValues.put("t87_tf", rangeList);


        SearchRequestParameter obj = new SearchRequestParameter();
        obj.setCollection("mergescloud");
        obj.setAttrCollection("screencloud");
        obj.setKeyWords("超声波");
        obj.setCatagoryId(3180);
        obj.setCityId(321);
        obj.setSorts(sorts);
        obj.setFilters(filters);
        obj.setFilterFieldsValues(filterFieldsValues);
        obj.setStart(0);
        obj.setRows(2);
        obj.setComeFromSearch(true);

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
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

    public static void testAttributeFilterSearchCatId(Boolean catTouch) {
        String url = "http://192.168.51.118:8999/search/filter/cataid";


        LinkedHashMap<java.lang.String, java.lang.String> sorts = new java.util.LinkedHashMap<java.lang.String, java.lang.String>();
        sorts.put("price", "asc");

        Map<java.lang.String, java.lang.String> filters = new java.util.HashMap<java.lang.String, java.lang.String>();
       // filters.put("da_89_s", "TIME/时代");
        // filters.put("t87_tf", "[0 TO *}");

        filters.put("da_2223_s", "xx5");

        LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> filterFieldsValues = new java.util.LinkedHashMap<java.lang.String, java.util.List<java.lang.String>>();
       /* filterFieldsValues.put("da_89_s", null);
        filterFieldsValues.put("da_2955_s", null);*/
        filterFieldsValues.put("da_89_s",null);
        filterFieldsValues.put("da_2223_s",null);
        filterFieldsValues.put("da_25_s",null);

        List rangeList = new java.util.ArrayList<String>();
        rangeList.add("[* TO 0}");
        rangeList.add("[0 TO 10}");
        rangeList.add("[10 TO 20}");
        rangeList.add("[20 TO 30}");
        rangeList.add("[30 TO *}");
        // filterFieldsValues.put("t87_tf", rangeList);


        SearchRequestParameter obj = new SearchRequestParameter();
        obj.setCollection("mergescloud");
        obj.setAttrCollection("screencloud");
        obj.setKeyWords("超声波");
        obj.setCatagoryId(6770);
        obj.setCityId(321);
        obj.setSorts(sorts);
        if(!catTouch) {
            obj.setFilters(filters);
            obj.setFilterFieldsValues(filterFieldsValues);
        }
        obj.setStart(0);
        obj.setRows(3);
        obj.setCategoryTouch(catTouch);

        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println("flag----"+catTouch+"_"+sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }


    public static void testSearchBrandsByCatoryId() {
        String url = "http://192.168.51.118:8999/search/brands";

        SearchRequestParameter obj = new SearchRequestParameter();
        obj.setCollection("mergescloud");
        obj.setCatagoryId(6770);
        obj.setCityId(321);

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

    public static void testSuggestByKeyWords() {
        String url = "http://192.168.51.118:8999/search/suggest/keywords";

        //  SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆筒", 363, null, null, 0, 10);
        SearchRequestParameter obj = new SearchRequestParameter();
        obj.setCollection("mergescloud");
        obj.setKeyWords("圆筒");
        obj.setCityId(321);


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


    public static void testPopularityKeyWords() {
        String url = "http://192.168.51.118:8999/search/popularity";

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


    public static void testGetCategoryIdsByKeyWords() {
        String url = "http://192.168.51.118:8999/search/catids";

        //  SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆筒", 363, null, null, 0, 10);
        SearchRequestParameter obj = new SearchRequestParameter();
        obj.setCollection("mergescloud");
        obj.setKeyWords("圆筒");
        obj.setCityId(321);

        Map filters = new java.util.HashMap<java.lang.String, java.lang.String>();
        filters.put("da_661_s", "36×20 -> 35×33");
        // filters.put("t87_tf", "[0 TO *}");
        //obj.setFilters(null);


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
        String url = "http://192.168.51.118:8999/search/keywords";
        // String url = "http://search-test.ehsy.com/search/keywords";

        //  SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆筒", 363, null, null, 0, 10);
        SearchRequestParameter obj = new SearchRequestParameter();
        obj.setCollection("mergescloud");
        obj.setAttrCollection("screencloud");
        obj.setKeyWords("超声波");
        obj.setCityId(321);
        LinkedHashMap sorts = new java.util.LinkedHashMap<java.lang.String, java.lang.String>();
        sorts.put("price", "asc");
        obj.setSorts(sorts);
        Map filters = new java.util.HashMap<java.lang.String, java.lang.String>();
        filters.put("da_661_s", "36×20 -> 35×33");
        // filters.put("t87_tf", "[0 TO *}");
        //obj.setFilters(filters);
        obj.setStart(0);
        obj.setRows(1);


        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", obj, headers);
        try {
            HttpEntity entity = httpResp.getEntity();
            String sResponse = EntityUtils.toString(entity);
            System.out.println(sResponse);
            ObjectMapper om = new ObjectMapper();
            om.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);



            JsonNode obj1 = om.readTree(sResponse);
            ArrayNode resultArray = (ArrayNode) obj1.get("data").get("searchResult").get("result");
            String picUrl = resultArray.get(0).get("picUrl").toString();
            String picUrl1 = resultArray.get(0).get("picUrl").asText();
            JSONObject falstObj = JSON.parseObject(sResponse);
            JSONObject data = falstObj.getJSONObject("data");
            JSONObject searchResult = data.getJSONObject("searchResult");
            JSONArray result = searchResult.getJSONArray("result");
            JSONArray picUrls = result.getJSONObject(0).getJSONArray("picUrl");
            String picUrlS = picUrls.getString(0);
            System.out.println("---------------------------"+picUrlS);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }


    public static void testHttp() {
        String url = "http://218.244.132.8:8088/recommend/sku";
        Map parametersMap = new java.util.HashMap<String, java.lang.Object>();
        // parametersMap.put("userId", "null");
        // parametersMap.put("catagoryId", "null");
        parametersMap.put("brandId", "1421");
        parametersMap.put("number", Integer.valueOf(30));
        Map headers = new java.util.HashMap<String, String>();
        headers.put("Content-Type", "application/json");
        // Function2 callback = new Function2<HttpContext, HttpResponse, BoxedUnit>(){});
        CloseableHttpResponse httpResp = HttpClientUtil.requestHttpSyn(url, "post", parametersMap, headers);
        try {
            String sResponse = EntityUtils.toString(httpResp.getEntity());
            ObjectMapper om = new ObjectMapper();
            JsonNode obj = om.readTree(sResponse);
            ArrayNode resultArray = (ArrayNode) obj.get("data").get("searchResult").get("result");
            String picUrl = resultArray.get(0).get("picUrl").toString();
            String picUrl1 = resultArray.get(0).get("picUrl").asText();
            System.out.println(sResponse);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            HttpClientUtils.closeQuietly(httpResp);
        }
    }
}
