package search.common.entity.news;

import java.util.Map;

/**
 * Created by soledede.weng on 2016/9/21.
 */
public class NewsQuery {
    private String query;
    private Integer from=0;
    private Integer to=10;
    private Integer leastTopMonth=6;
    private String sort;
    private String order;
    private Integer needHl=1; //0 false,1 true
    private Map<String, String> sorts;

    public NewsQuery() {
    }

    public NewsQuery(String query, Integer from, Integer to, Integer leastTopMonth, Map<String, String> sorts) {
        this.query = query;
        this.from = from;
        this.to = to;
        this.leastTopMonth = leastTopMonth;
        this.sorts = sorts;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public Integer getLeastTopMonth() {
        return leastTopMonth;
    }

    public void setLeastTopMonth(Integer leastTopMonth) {
        this.leastTopMonth = leastTopMonth;
    }

    public Map<String, String> getSorts() {
        return sorts;
    }

    public void setSorts(Map<String, String> sorts) {
        this.sorts = sorts;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public Integer getNeedHl() {
        return needHl;
    }

    public void setNeedHl(Integer needHl) {
        this.needHl = needHl;
    }
}
