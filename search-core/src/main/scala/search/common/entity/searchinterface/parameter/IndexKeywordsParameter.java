package search.common.entity.searchinterface.parameter;

import java.util.Collection;

/**
 * Created by soledede.weng on 2016/8/1.
 */
public class IndexKeywordsParameter {

    String originQuery;

    Collection<String> keywords;

    public String getOriginQuery() {
        return originQuery;
    }

    public void setOriginQuery(String originQuery) {
        this.originQuery = originQuery;
    }

    public Collection<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(Collection<String> keywords) {
        this.keywords = keywords;
    }
}
