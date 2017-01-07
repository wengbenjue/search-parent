package search.common.entity.report;

/**
 * 研报实体
 * Created by soledede.weng on 2017-01-06.
 */
public class ReportEntity {
    private Object result;
    private Long total;

    public ReportEntity() {
    }

    public ReportEntity(Object result, Long total) {
        this.result = result;
        this.total = total;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
