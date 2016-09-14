package search.common.entity.bizesinterface;

/**
 * Created by soledede.weng on 2016/9/13.
 */
public class CompanyStock extends AbstractGraphEntity{

    private String code;
    private String simPy;

    public CompanyStock() {
    }

    public CompanyStock(String id, String code, String name, String simPy) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.simPy = simPy;
    }

    public CompanyStock(String id, String code, String name, String simPy,Double weight) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.simPy = simPy;
        this.weight = weight;
    }


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


    public String getSimPy() {
        return simPy;
    }

    public void setSimPy(String simPy) {
        this.simPy = simPy;
    }
}
