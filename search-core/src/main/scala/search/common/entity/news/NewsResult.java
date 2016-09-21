package search.common.entity.news;

import java.util.Map;

/**
 * Created by soledede.weng on 2016/9/21.
 */
public class NewsResult {
    private Integer count;
    private java.util.Map<String, Object> [] result;

    public NewsResult() {
    }

    public NewsResult(Integer count, Map<String, Object>[] result) {
        this.count = count;
        this.result = result;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Map<String, Object>[] getResult() {
        return result;
    }

    public void setResult(Map<String, Object>[] result) {
        this.result = result;
    }
}
