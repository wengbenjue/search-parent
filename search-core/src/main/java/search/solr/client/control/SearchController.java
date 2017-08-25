package search.solr.client.control;


import org.springframework.web.bind.annotation.*;
import search.common.entity.searchinterface.NiNi;
import search.common.entity.searchinterface.parameter.IndexParameter;
import search.common.entity.searchinterface.parameter.RecordLogParameter;
import search.common.entity.searchinterface.parameter.SearchRequestParameter;
import search.common.entity.searchinterface.parameter.TestSimple;
import search.solr.client.product.Producter;
import search.solr.client.searchInterface.SearchInterface;

import java.util.List;


@RestController
@RequestMapping("/search")
public class SearchController {

    //getCatagoryIds by keywords
    @RequestMapping(value = "/catids", method = RequestMethod.POST)
    public NiNi getCategoryIdsByKeyWords(@RequestBody final SearchRequestParameter categoryIdsByKeyWord) {
        String collection = categoryIdsByKeyWord.getCollection();
        Integer cityId = categoryIdsByKeyWord.getCityId();
        if (collection == null || cityId == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection or cityId is null!");
            return nini;
        } else
            return SearchInterface.getCategoryIdsByKeyWords(collection, categoryIdsByKeyWord.getKeyWords(), cityId, categoryIdsByKeyWord.getFilters());
    }

    //search and filter by keywords
    @RequestMapping(value = "/keywords", method = RequestMethod.POST)
    public NiNi searchByKeywords(@RequestBody final SearchRequestParameter searchByKeywords) {
        String collection = searchByKeywords.getCollection();
        String attrCollection = searchByKeywords.getAttrCollection();
        Integer cityId = searchByKeywords.getCityId();
        if (collection == null || attrCollection == null || cityId == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection or attrCollection or cityId is null!");
            return nini;
        } else
            return SearchInterface.searchByKeywords(collection, attrCollection, searchByKeywords.getKeyWords(), cityId, searchByKeywords.getFilters(), searchByKeywords.getSorts(), searchByKeywords.getStart(), searchByKeywords.getRows());
    }

    //search and filter by keywords
    @RequestMapping(value = "/popularity", method = RequestMethod.POST)
    public NiNi popularityKeywords() {
        return SearchInterface.popularityKeyWords();
    }

    //suggest keyword  for search
    @RequestMapping(value = "/suggest/keywords", method = RequestMethod.POST)
    public NiNi suggestByKeyWords(@RequestBody final SearchRequestParameter suggestByKeyWord) {
        String collection = suggestByKeyWord.getCollection();
        Integer cityId = suggestByKeyWord.getCityId();
        if (collection == null || cityId == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection or cityId is null");
            return nini;
        } else
            return SearchInterface.suggestByKeyWords(collection, suggestByKeyWord.getKeyWords(), cityId);
    }

    //get brands by catoryid
    @RequestMapping(value = "/brands", method = RequestMethod.POST)
    public NiNi searchBrandsByCatoryId(@RequestBody final SearchRequestParameter searchBrands) {
        String collection = searchBrands.getCollection();
        Integer cityId = searchBrands.getCityId();
        Integer catagoryId = searchBrands.getCatagoryId();
        if (collection == null || cityId == null || catagoryId == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection or cityId  or catagoryId is null");
            return nini;
        } else
            return SearchInterface.searchBrandsByCatoryId(collection, catagoryId, cityId);
    }


    //filter by catoryid
    @RequestMapping(value = "/filter/cataid", method = RequestMethod.POST)
    public NiNi attributeFilterSearchCate(@RequestBody final SearchRequestParameter attributeFilterCate) {
        String collection = attributeFilterCate.getCollection();
        String attrCollection = attributeFilterCate.getAttrCollection();
        Integer cityId = attributeFilterCate.getCityId();
        Integer catagoryId = attributeFilterCate.getCatagoryId();
        if (collection == null || cityId == null || catagoryId == null || attrCollection == null) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection or cityId or catagoryId or attrCollection is null");
            return nini;
        } else
            return SearchInterface.attributeFilterSearch(collection, attrCollection, attributeFilterCate.getKeyWords(), attributeFilterCate.getCatagoryId(), cityId, attributeFilterCate.getSorts(), attributeFilterCate.getFilters(), attributeFilterCate.getFilterFieldsValues(), attributeFilterCate.getStart(), attributeFilterCate.getRows(), attributeFilterCate.getCategoryTouch());
    }


    //filter by search ,top-K catids
    @RequestMapping(value = "/filter/search", method = RequestMethod.POST)
    public NiNi attributeFilterSearch(@RequestBody final SearchRequestParameter attributeFilter) {
        String collection = attributeFilter.getCollection();
        Integer cityId = attributeFilter.getCityId();
        Boolean isComeFromSearch = attributeFilter.getComeFromSearch();
        if (collection == null || cityId == null || isComeFromSearch == false) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection or cityId or is null and isComeFromSearch  must be true ");
            return nini;
        } else
            return SearchInterface.attributeFilterSearch(collection, attributeFilter.getKeyWords(), attributeFilter.getCatagoryId(), cityId, attributeFilter.getSorts(),
                    attributeFilter.getFilters(), attributeFilter.getFilterFieldsValues(), attributeFilter.getStart(), attributeFilter.getRows(),
                    null, isComeFromSearch);
    }

    //record log
    @RequestMapping(value = "/record",method ={RequestMethod.POST,RequestMethod.GET})
    public void recordSearchLog(@RequestBody final RecordLogParameter logEntity) {
        SearchInterface.recordSearchLog(logEntity.getKeyWords(), logEntity.getAppKey(), logEntity.getClientIp(), logEntity.getUserAgent(), logEntity.getSourceType(), logEntity.getCookies(), logEntity.getUserId());
    }


    //***************************************************//index
    @RequestMapping(value = "/index", method = RequestMethod.POST)
    public NiNi deleteIds(@RequestBody final IndexParameter indexEntity) {
        String collection = indexEntity.getCollection();
        Integer totalNum = indexEntity.getTotalNum();
        Long minUpdateTime = indexEntity.getMinUpdateTime();

        if (collection == null || totalNum == null || totalNum <= 0) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection  is null and totalNum must greater than 0 ");
            return nini;
        } else {
            if (minUpdateTime != null) return Producter.send(collection, minUpdateTime, totalNum);
            else
                return Producter.send(collection, indexEntity.getStartUpdateTime(), indexEntity.getEndUpdataTime(), totalNum);
        }

    }

    //delete index by ids
    @RequestMapping(value = "/delete/ids", method = RequestMethod.POST)
    public NiNi index(@RequestBody final IndexParameter indexEntity) {
        String collection = indexEntity.getCollection();
        List<String> ids = indexEntity.getIds();

        if (collection == null || ids == null || ids.size() <= 0) {
            NiNi nini = new NiNi();
            nini.setCode(-1);
            nini.setMsg("collection  is null and ids is null or empty ");
            return nini;
        } else return Producter.delete(collection, ids);
    }

    @RequestMapping(value = "/test", method = RequestMethod.POST)
    public NiNi test(@RequestBody final TestSimple testSimple) {
        return SearchInterface.searchByKeywords("mergescloud", "screencloud", "圆筒", 363, null, null, 0, 10);
    }
}