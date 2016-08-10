package search.solr.client.control;


import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import search.common.entity.bizesinterface.IndexObjEntity;
import search.es.client.biz.BizeEsInterface;
import search.common.entity.searchinterface.NiNi;
import search.common.entity.searchinterface.parameter.*;
import search.solr.client.searchInterface.SearchInterface;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;


@RestController
@RequestMapping("/es")
public class EsSearchController {


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
            return BizeEsInterface.searchTopKeyWord(null, keywords);
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
        if (keywords == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("keywords is null!");
            return nini;
        } else {
            NiNi result = BizeEsInterface.wrapShowStateAndGetByQuery(keywords);
            SearchInterface.recordSearchLog(keywords, request(), getSessionId());
            return result;
        }

    }

    @RequestMapping(value = "/search/clean/cacheredis", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi cleanRedisByNamespace(final String namespace) {
        return BizeEsInterface.wrapCleanRedisByNamespace(namespace);
    }


    @RequestMapping(value = "/search/del/mongo/index", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi deleteAllMongoData() {
        return BizeEsInterface.wrapDeleteAllMongoData();
    }

    @RequestMapping(value = "/search/clean/all", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi cleanAllFromMongoAndIndex() {
        return BizeEsInterface.wrapCleanAllFromMongoAndIndex();
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
}