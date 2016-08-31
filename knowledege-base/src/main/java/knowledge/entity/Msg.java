package knowledge.entity;

/**
 * Created by soledede.weng on 2016/8/31.
 */
public class Msg {
    private Integer code=-1;
    private String message;

    public Msg() {
    }

    public Msg(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
