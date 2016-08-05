package search.common.entity.bizesinterface;

/**
 * Created by soledede.weng on 2016/8/5.
 */
public class IndexObjEntity {
    private String keyword;
    private String rvkw;

    public IndexObjEntity() {
    }

    public IndexObjEntity(String keyword, String rvkw) {
        this.keyword = keyword;
        this.rvkw = rvkw;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getRvkw() {
        return rvkw;
    }

    public void setRvkw(String rvkw) {
        this.rvkw = rvkw;
    }
}
