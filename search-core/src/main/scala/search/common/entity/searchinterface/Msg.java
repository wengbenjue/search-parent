package search.common.entity.searchinterface;

import java.io.Serializable;

/**
 * Created by soledede on 2016/2/20.
 */
public class Msg  implements Serializable{
    private Integer code;
    private String  msg;

    public Msg() {
    }

    public Msg(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
