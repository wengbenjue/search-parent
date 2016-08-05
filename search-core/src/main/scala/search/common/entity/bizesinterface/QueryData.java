package search.common.entity.bizesinterface;

/**
 * Created by soledede.weng on 2016/8/5.
 */
public class QueryData {
    private String query;
    private String target;
    private Object data;

    public QueryData() {
    }

    public QueryData(String query, String target, Object data) {
        this.query = query;
        this.target = target;
        this.data = data;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
