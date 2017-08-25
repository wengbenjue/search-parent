package search.common.entity.searchinterface.parameter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by soledede on 2016/4/18.
 */
public class SearchRequestParameter {
    private String collection;
    private String attrCollection;
    private String keyWords;
    private Integer cityId;
    private java.util.Map<java.lang.String, java.lang.String> filters;
    private java.util.LinkedHashMap<java.lang.String, java.lang.String> sorts;
    private Integer start;
    private Integer rows;
    private Integer catagoryId;
    private java.util.LinkedHashMap<java.lang.String, java.util.List<java.lang.String>> filterFieldsValues;
    private Boolean isCategoryTouch;
    private Boolean isComeFromSearch=true;

    public SearchRequestParameter() {
    }


    public SearchRequestParameter(String collection, String attrCollection, String keyWords, Integer cityId, Map<String, String> filters, LinkedHashMap<String, String> sorts, Integer start, Integer rows, Integer catagoryId, LinkedHashMap<String, List<String>> filterFieldsValues, Boolean isCategoryTouch, Boolean isComeFromSearch) {
        this.collection = collection;
        this.attrCollection = attrCollection;
        this.keyWords = keyWords;
        this.cityId = cityId;
        this.filters = filters;
        this.sorts = sorts;
        this.start = start;
        this.rows = rows;
        this.catagoryId = catagoryId;
        this.filterFieldsValues = filterFieldsValues;
        this.isCategoryTouch = isCategoryTouch;
        this.isComeFromSearch = isComeFromSearch;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getAttrCollection() {
        return attrCollection;
    }

    public void setAttrCollection(String attrCollection) {
        this.attrCollection = attrCollection;
    }

    public String getKeyWords() {
        return keyWords;
    }

    public void setKeyWords(String keyWords) {
        this.keyWords = keyWords;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public Map<String, String> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, String> filters) {
        this.filters = filters;
    }

    public LinkedHashMap<String, String> getSorts() {
        return sorts;
    }

    public void setSorts(LinkedHashMap<String, String> sorts) {
        this.sorts = sorts;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getCatagoryId() {
        return catagoryId;
    }

    public void setCatagoryId(Integer catagoryId) {
        this.catagoryId = catagoryId;
    }

    public LinkedHashMap<String, List<String>> getFilterFieldsValues() {
        return filterFieldsValues;
    }

    public void setFilterFieldsValues(LinkedHashMap<String, List<String>> filterFieldsValues) {
        this.filterFieldsValues = filterFieldsValues;
    }

    public Boolean getCategoryTouch() {
        return isCategoryTouch;
    }

    public void setCategoryTouch(Boolean categoryTouch) {
        isCategoryTouch = categoryTouch;
    }

    public Boolean getComeFromSearch() {
        return isComeFromSearch;
    }

    public void setComeFromSearch(Boolean comeFromSearch) {
        isComeFromSearch = comeFromSearch;
    }
}
