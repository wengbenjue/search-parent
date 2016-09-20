package search.solr.client.control;


import org.springframework.boot.autoconfigure.integration.IntegrationAutoConfiguration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import search.common.entity.bizesinterface.GraphNodes;
import search.common.entity.bizesinterface.IndexObjEntity;
import search.common.entity.bizesinterface.QueryEntityWithCnt;
import search.common.entity.news.News;
import search.es.client.biz.BizeEsInterface;
import search.common.entity.searchinterface.NiNi;
import search.common.entity.searchinterface.parameter.*;
import search.es.client.biz.Wraps;
import search.solr.client.searchInterface.SearchInterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/es")
public class EsSearchController {


    @RequestMapping(value = "/index/news", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi indexNews(final Collection<News> news) {
        if (news == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("news is null!");
            return nini;
        } else
            return BizeEsInterface.wrapIndexNews(news);
    }


    //search and filter by keywords
    @RequestMapping(value = "/search/keywords", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi searchByKeywords(final KnowledgeGraphParameter knowledgeGraphParameter) {
        String keywords = knowledgeGraphParameter.getKeyword();
        if (keywords == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else
            return BizeEsInterface.searchTopKeyWord(request(), null, keywords);
    }

    //search and filter by keywords
    @RequestMapping(value = "/delete/keywords", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi deleteIndexByKeywords(@RequestBody final IndexKeywordsParameter indexKeywordsParameter) {
        Collection<String> keywords = indexKeywordsParameter.getKeywords();
        String originQuery = indexKeywordsParameter.getOriginQuery();
        if (keywords == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else
            return BizeEsInterface.wrapDelIndexByKeywords(keywords);
    }

    @RequestMapping(value = "/index/keywords", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi indexByKeywords(@RequestBody final IndexKeywordsParameter indexKeywordsParameter) {
        Collection<String> keywords = indexKeywordsParameter.getKeywords();
        String originQuery = indexKeywordsParameter.getOriginQuery();
        if (keywords == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else
            return BizeEsInterface.indexByKeywords(null, originQuery, keywords);
    }

    @RequestMapping(value = "/index/kw", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi indexByKeyword(final String keyword) {
        if (keyword == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else {
            List<IndexObjEntity> en = new ArrayList<>();
            en.add(new IndexObjEntity(keyword));
            return BizeEsInterface.wrapIndexByKeywords(en);
        }
    }


    @RequestMapping(value = "/index/rws", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi indexByKeywordsWithKw(@RequestBody final Collection<IndexObjEntity> keywords) {
        if (keywords == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else
            return BizeEsInterface.wrapIndexByKeywords(keywords);
    }


    //search and filter by keywords
    @RequestMapping(value = "/index/all", method = {RequestMethod.POST, RequestMethod.GET})
    public void indexAll(@RequestParam("indexName") String indexName,
                         @RequestParam("typeName") String typeName) {
        BizeEsInterface.totalIndexRun(indexName, typeName);
    }

    @RequestMapping(value = "/search/state", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi showStateByQuery(final KnowledgeGraphParameter knowledgeGraphParameter) {

        String keywords = knowledgeGraphParameter.getKeyword();
        Integer showLevel = knowledgeGraphParameter.getL();
        if (keywords == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else {
            NiNi result = BizeEsInterface.wrapShowStateAndGetByQuery(request(), keywords, showLevel, knowledgeGraphParameter.getNeedSearch());
            SearchInterface.recordSearchLog(keywords, request(), getSessionId());
            return result;
        }

    }


    @RequestMapping(value = "/search/prefix", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi prefix(@RequestParam("word") String word,
                       @RequestParam(value = "maxLengthPerType", required = false, defaultValue = "5") Integer maxLengthPerType) {
        if (word == null || word.trim().equalsIgnoreCase("")) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("prefix is null!");
            return nini;
        }
        return BizeEsInterface.wrapPrefix(word.trim(), maxLengthPerType);
    }

    @RequestMapping(value = "/search/clean/cacheredis", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi cleanRedisByNamespace(final String namespace) {
        return BizeEsInterface.wrapCleanRedisByNamespace(namespace);
    }


    @RequestMapping(value = "/search/nlp_cache/clean", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi cleanQueryByGraphKeys(@RequestParam("nodes") Set<String> nodes) {
        return BizeEsInterface.wrapCleanQueryByGraphKeys(nodes);
    }


    @RequestMapping(value = "/search/del/mongo/index", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi deleteAllMongoData() {
        return BizeEsInterface.wrapDeleteAllMongoData();
    }

    @RequestMapping(value = "/search/clean/all", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi cleanAllFromMongoAndIndex() {
        return BizeEsInterface.wrapCleanAllFromMongoAndIndex();
    }

    @RequestMapping(value = "/search/count", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi count() {
        return BizeEsInterface.wrapCount();
    }


    @RequestMapping(value = "/search/all", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi matchAllQueryWithCount(@RequestParam(value = "from", required = false, defaultValue = "0") Integer from, @RequestParam(value = "to", required = false, defaultValue = "10") Integer to) {
        NiNi result = BizeEsInterface.wrapMatchAllQueryWithCount(from, to);
        return result;

    }

    @RequestMapping(value = "/index/dump", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi wrapDumpIndexToDisk() {
        NiNi result = Wraps.wrapDumpIndexToDisk();
        return result;

    }

    @RequestMapping(value = "/search/warm", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi wrapWarmCache() {
        NiNi result = BizeEsInterface.wrapWarmCache();
        return result;
    }

    @RequestMapping(value = "/search/cache/load", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi loadCache() {
        NiNi result = BizeEsInterface.warpLoadCache();
        return result;
    }


    @RequestMapping(value = "/search/cache/view", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi viewCache(@RequestParam(value = "key", required = false, defaultValue = "-1") String key) {
        NiNi result = BizeEsInterface.wrapViewCache(key);
        return result;
    }

    @RequestMapping(value = "/search/bloomfilter/load", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi bloomFilterLoad() {
        NiNi result = BizeEsInterface.wrapAddBloomFilter();
        return result;
    }

    @RequestMapping(value = "/search/index/cat/load", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi indexCatOfKeywords() {
        NiNi result = BizeEsInterface.warpIndexCatOfKeywords();
        return result;
    }

    @RequestMapping(value = "/search/event/rule/load", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi loadEventRegexToCache() {
        NiNi result = BizeEsInterface.warpLoadEventRegexToCache();
        return result;
    }


    @RequestMapping(value = "/search/graph/allnode", method = {RequestMethod.POST, RequestMethod.GET})
    public GraphNodes filterGraphNodes() {
        GraphNodes result = BizeEsInterface.filterGraphNodes();
        return result;
    }


    public static String getSessionId() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();
        return request.getSession().getId();
    }

    public static HttpServletRequest request() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletRequest request = sra.getRequest();
        return request;
    }

    public static HttpServletResponse response() {
        RequestAttributes ra = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) ra;
        HttpServletResponse response = sra.getResponse();
        return response;
    }

}