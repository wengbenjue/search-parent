package search.common.entity.searchinterface;

import java.io.Serializable;
import java.util.List;

/**
 * Created by soledede on 2016/2/20.
 */
public class FilterAttributeSearchResult implements Serializable {

    private List<FilterAttribute> filterAttributes; //all filter attributes
    private SearchResult searchResult; //the searchResult
    private List<Integer> categoryIds; // filter category ids

    public FilterAttributeSearchResult() {
    }

    public FilterAttributeSearchResult(List<FilterAttribute> filterAttributes, SearchResult searchResult, List<Integer> categoryIds) {
        this.filterAttributes = filterAttributes;
        this.searchResult = searchResult;
        this.categoryIds = categoryIds;
    }

    public List<FilterAttribute> getFilterAttributes() {
        return filterAttributes;
    }

    public void setFilterAttributes(List<FilterAttribute> filterAttributes) {
        this.filterAttributes = filterAttributes;
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;
    }

    public List<Integer> getCategoryIds() {
        return categoryIds;
    }

    public void setCategoryIds(List<Integer> categoryIds) {
        this.categoryIds = categoryIds;
    }


}
