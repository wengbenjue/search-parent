package search.common.entity.bizesinterface;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by soledede.weng on 2016/8/5.
 */
public class IndexObjEntity {
    private String keyword;
    private Collection<String> rvkw;

    public IndexObjEntity() {
    }

    public IndexObjEntity(String keyword) {
        this.keyword = keyword;
    }

    public IndexObjEntity(String keyword, String rvkw) {
        this.keyword = keyword;
        List<String> list = new ArrayList<>();
        list.add(rvkw);
        this.rvkw = list;
    }

    public IndexObjEntity(String keyword, Collection<String> rvkw) {
        this.keyword = keyword;
        this.rvkw = rvkw;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Collection<String> getRvkw() {
        return rvkw;
    }

    public void setRvkw(Collection<String> rvkw) {
        this.rvkw = rvkw;
    }
}
