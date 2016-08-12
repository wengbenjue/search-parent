package search.common.entity.bizesinterface;

/**
 * Created by soledede.weng on 2016/8/11.
 */
public class QueryEntityWithCnt {

    private Integer from;
    private Integer to;
    private Long total;
    private Integer size;
    private Object result;

    public QueryEntityWithCnt() {
    }

    public QueryEntityWithCnt(Long total, Object result) {
        this.total = total;
        this.result = result;
    }

    public QueryEntityWithCnt(Integer from, Integer to, Long total, Integer size, Object result) {
        this.from = from;
        this.to = to;
        this.total = total;
        this.size = size;
        this.result = result;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
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

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
