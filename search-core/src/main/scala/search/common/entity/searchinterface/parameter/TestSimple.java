package search.common.entity.searchinterface.parameter;

import javax.validation.constraints.NotNull;

/**
 * Created by soledede on 2016/4/18.
 */
public class TestSimple {
    private String code;
    @NotNull
    private Integer soledede;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Integer getSoledede() {
        return soledede;
    }

    public void setSoledede(Integer soledede) {
        this.soledede = soledede;
    }
}
