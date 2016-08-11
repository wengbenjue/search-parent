package search.common.entity.searchinterface.parameter;

/**
 * Created by soledede.weng on 2016/8/1.
 */
public class KnowledgeGraphParameter {
    private String keyword;
    private Integer needSearch = 1;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getNeedSearch() {
        return needSearch;
    }

    public void setNeedSearch(Integer needSearch) {
        this.needSearch = needSearch;
    }
}
