package search.common.entity.searchinterface;

/**
 * Created by soledede on 2016/4/18.
 */
public class NiNi {
    private Float costMillisecond;
    private Float costSecond;
    private Object data;
    private String msg="sucsess";
    private Integer code=0;

    public NiNi() {
    }

    public NiNi(Float costMillisecond, Float costSecond, Object data) {
        this.costMillisecond = costMillisecond;
        this.costSecond = costSecond;
        this.data = data;
    }

    public NiNi(Float costMillisecond, Float costSecond, Object data, String msg, Integer code) {
        this.costMillisecond = costMillisecond;
        this.costSecond = costSecond;
        this.data = data;
        this.msg = msg;
        this.code = code;
    }

    public Float getCostMillisecond() {
        return costMillisecond;
    }

    public void setCostMillisecond(Float costMillisecond) {
        this.costMillisecond = costMillisecond;
    }

    public Float getCostSecond() {
        return costSecond;
    }

    public void setCostSecond(Float costSecond) {
        this.costSecond = costSecond;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
