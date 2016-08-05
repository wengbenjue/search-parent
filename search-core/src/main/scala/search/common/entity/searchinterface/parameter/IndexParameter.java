package search.common.entity.searchinterface.parameter;

import java.util.List;

/**
 * Created by soledede on 2016/4/18.
 */
public class IndexParameter {
    private String collection;
    private Long startUpdateTime;
    private Long endUpdataTime;
    private Long minUpdateTime;
    private Integer totalNum;
    private java.util.List<String> ids;

    public IndexParameter() {
    }

    public IndexParameter(String collection, Long startUpdateTime, Long endUpdataTime, Long minUpdateTime, Integer totalNum, List<String> ids) {
        this.collection = collection;
        this.startUpdateTime = startUpdateTime;
        this.endUpdataTime = endUpdataTime;
        this.minUpdateTime = minUpdateTime;
        this.totalNum = totalNum;
        this.ids = ids;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Long getStartUpdateTime() {
        return startUpdateTime;
    }

    public void setStartUpdateTime(Long startUpdateTime) {
        this.startUpdateTime = startUpdateTime;
    }

    public Long getEndUpdataTime() {
        return endUpdataTime;
    }

    public void setEndUpdataTime(Long endUpdataTime) {
        this.endUpdataTime = endUpdataTime;
    }

    public Integer getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(Integer totalNum) {
        this.totalNum = totalNum;
    }

    public List<String> getIds() {
        return ids;
    }

    public void setIds(List<String> ids) {
        this.ids = ids;
    }

    public Long getMinUpdateTime() {
        return minUpdateTime;
    }

    public void setMinUpdateTime(Long minUpdateTime) {
        this.minUpdateTime = minUpdateTime;
    }
}
